package firearms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import firearms.model.Slot;
import firearms.model.WeaponClass;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/**
 * `FA-9`'s item model assets, checked against the files themselves rather than by loading
 * Minecraft, the same {@code create_metered_motor} {@code ModelAssetsTest} precedent this mirrors:
 * every {@code items/*.json} model reference resolves to a model file that exists, every model
 * file's own texture reference resolves to a PNG that exists, every base weapon's composite carries
 * exactly its own class's slot condition layers (`WeaponClass#slots()`) and no other, and every
 * attachment id `data/firearms/attachment/*.json` names has both a sprite and a model, wired into
 * its own slot's {@code items/attachment_<slot>.json} (`docs/spec/04-architecture.md`
 * `ARCH-DEC-006`, `docs/spec/domains/weapon.md` `WEAPON-REQ-012`).
 */
final class ModelAssetsTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/firearms");
    private static final Path ITEMS = ASSETS.resolve("items");
    private static final Path MODELS = ASSETS.resolve("models/item");
    private static final Path TEXTURES = ASSETS.resolve("textures/item");
    private static final Path WEAPON_DATA = Path.of("src/main/resources/data/firearms/weapon");
    private static final Path ATTACHMENT_DATA = Path.of("src/main/resources/data/firearms/attachment");

    private static final Pattern MODEL_REF = Pattern.compile("\"model\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern TEXTURES_BLOCK = Pattern.compile("\"textures\"\\s*:\\s*\\{([^}]*)}");
    private static final Pattern RESOURCE_LOCATION = Pattern.compile("\"([a-z0-9_.-]+:[a-z0-9_/.-]+)\"");
    private static final Pattern WEAPON_ID_CASE = Pattern.compile("\"weapon_id\"\\s*:\\s*\"firearms:([a-z0-9_]+)\"");
    private static final Pattern SLOT_COMPONENT = Pattern.compile("firearms:attachment_(muzzle|optic|magazine|grip|stock)");
    private static final Pattern WEAPON_CLASS_FIELD = Pattern.compile("\"class\"\\s*:\\s*\"([a-z0-9_]+)\"");
    private static final Pattern ATTACHMENT_SLOT_FIELD = Pattern.compile("\"slot\"\\s*:\\s*\"([a-z0-9_]+)\"");

    @Test
    void everyItemDefinitionModelReferenceResolvesToAnExistingModelFile() throws IOException {
        List<String> missing = new ArrayList<>();
        for (Path itemFile : jsonFiles(ITEMS)) {
            String text = Files.readString(itemFile);
            Matcher m = MODEL_REF.matcher(text);
            while (m.find()) {
                Path modelFile = modelFileFor(m.group(1));
                if (!Files.isRegularFile(modelFile)) {
                    missing.add(itemFile + ": " + m.group(1) + " -> " + modelFile);
                }
            }
        }
        assertTrue(missing.isEmpty(), "every item definition model reference resolves: " + missing);
    }

    @Test
    void everyModelsTextureReferenceResolvesToAnExistingPng() throws IOException {
        List<String> missing = new ArrayList<>();
        for (Path modelFile : jsonFiles(MODELS)) {
            String text = Files.readString(modelFile);
            Matcher block = TEXTURES_BLOCK.matcher(text);
            if (!block.find()) {
                continue;
            }
            Matcher refs = RESOURCE_LOCATION.matcher(block.group(1));
            while (refs.find()) {
                Path png = textureFileFor(refs.group(1));
                if (!Files.isRegularFile(png)) {
                    missing.add(modelFile + ": " + refs.group(1) + " -> " + png);
                }
            }
        }
        assertTrue(missing.isEmpty(), "every model's texture reference resolves to a PNG: " + missing);
    }

    @Test
    void everyBaseHasExactlyItsOwnClasssSlotLayersAndNoOther() throws IOException {
        String weaponJson = Files.readString(ITEMS.resolve("weapon.json"));
        Map<String, Integer> starts = new LinkedHashMap<>();
        Matcher ids = WEAPON_ID_CASE.matcher(weaponJson);
        while (ids.find()) {
            starts.put(ids.group(1), ids.start());
        }
        List<String> order = new ArrayList<>(starts.keySet());
        List<String> mismatches = new ArrayList<>();
        for (int i = 0; i < order.size(); i++) {
            String weaponId = order.get(i);
            int from = starts.get(weaponId);
            int to = i + 1 < order.size() ? starts.get(order.get(i + 1)) : weaponJson.length();
            String chunk = weaponJson.substring(from, to);

            Set<String> actual = new TreeSet<>();
            Matcher slots = SLOT_COMPONENT.matcher(chunk);
            while (slots.find()) {
                actual.add(slots.group(1));
            }

            Set<String> expected = new TreeSet<>();
            for (Slot slot : weaponClassOf(weaponId).slots()) {
                expected.add(slot.name().toLowerCase(Locale.ROOT));
            }
            if (!actual.equals(expected)) {
                mismatches.add(weaponId + ": expected " + expected + " but found " + actual);
            }
        }
        assertEquals(6, order.size(), "items/weapon.json names all six bases: " + order);
        assertTrue(mismatches.isEmpty(), "every base's composite carries exactly its class's slots: " + mismatches);
    }

    @Test
    void everyAttachmentIdHasASpriteAModelAndIsWiredIntoItsOwnSlotsItemDefinition() throws IOException {
        List<String> missingSprite = new ArrayList<>();
        List<String> missingModel = new ArrayList<>();
        List<String> notWired = new ArrayList<>();
        Map<String, String> itemDefinitionText = new LinkedHashMap<>();

        for (Path attachmentFile : jsonFiles(ATTACHMENT_DATA)) {
            String name = filenameWithoutExtension(attachmentFile);
            String data = Files.readString(attachmentFile);
            Matcher slotField = ATTACHMENT_SLOT_FIELD.matcher(data);
            assertTrue(slotField.find(), attachmentFile + " names its own slot");
            String slot = slotField.group(1);

            Path sprite = TEXTURES.resolve("attachment/" + name + ".png");
            if (!Files.isRegularFile(sprite)) {
                missingSprite.add(name);
            }
            Path model = MODELS.resolve("attachment/" + name + ".json");
            if (!Files.isRegularFile(model)) {
                missingModel.add(name);
            }

            String itemDefinition = itemDefinitionText.computeIfAbsent(slot, s -> readOrEmpty(ITEMS.resolve("attachment_" + s + ".json")));
            if (!itemDefinition.contains("\"firearms:" + name + "\"")) {
                notWired.add(name + " (slot " + slot + ")");
            }
        }

        assertTrue(missingSprite.isEmpty(), "every attachment has a sprite: " + missingSprite);
        assertTrue(missingModel.isEmpty(), "every attachment has a model: " + missingModel);
        assertTrue(notWired.isEmpty(), "every attachment is a case in its own slot's item definition: " + notWired);
    }

    @Test
    void baseWeaponAndLayerTexturesAre32x16AndAttachmentAndCartridgeIconsAre16x16() throws IOException {
        for (Path png : pngFiles(TEXTURES.resolve("weapon"))) {
            var image = ImageIO.read(png.toFile());
            assertEquals(32, image.getWidth(), png + " width");
            assertEquals(16, image.getHeight(), png + " height");
        }
        for (Path dir : List.of(TEXTURES.resolve("attachment"), TEXTURES.resolve("cartridge"))) {
            for (Path png : pngFiles(dir)) {
                var image = ImageIO.read(png.toFile());
                assertEquals(16, image.getWidth(), png + " width");
                assertEquals(16, image.getHeight(), png + " height");
            }
        }
    }

    private static WeaponClass weaponClassOf(String weaponId) throws IOException {
        Path dataFile = WEAPON_DATA.resolve(weaponId + ".json");
        assertTrue(Files.isRegularFile(dataFile), "weapon data file exists for " + weaponId);
        Matcher m = WEAPON_CLASS_FIELD.matcher(Files.readString(dataFile));
        assertTrue(m.find(), dataFile + " names its own class");
        return WeaponClass.valueOf(m.group(1).toUpperCase(Locale.ROOT));
    }

    private static String readOrEmpty(Path path) {
        try {
            return Files.isRegularFile(path) ? Files.readString(path) : "";
        } catch (IOException e) {
            return "";
        }
    }

    private static String filenameWithoutExtension(Path path) {
        String name = path.getFileName().toString();
        return name.substring(0, name.length() - ".json".length());
    }

    /** {@code "firearms:item/weapon/m1911"} -> {@code models/item/weapon/m1911.json}. */
    private static Path modelFileFor(String reference) {
        String path = reference.substring(reference.indexOf(':') + 1);
        assertTrue(path.startsWith("item/"), reference + " is under the item/ model root");
        return ASSETS.resolve("models").resolve(path + ".json");
    }

    /** {@code "firearms:item/weapon/m1911"} -> {@code textures/item/weapon/m1911.png}. */
    private static Path textureFileFor(String reference) {
        String path = reference.substring(reference.indexOf(':') + 1);
        return ASSETS.resolve("textures").resolve(path + ".png");
    }

    private static List<Path> jsonFiles(Path root) throws IOException {
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(p -> p.toString().endsWith(".json")).sorted().toList();
        }
    }

    private static List<Path> pngFiles(Path root) throws IOException {
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(p -> p.toString().endsWith(".png")).sorted().toList();
        }
    }
}
