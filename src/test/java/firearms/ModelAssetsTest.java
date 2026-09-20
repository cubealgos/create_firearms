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
    private static final Pattern TEXTURE_SIZE = Pattern.compile("\"texture_size\"\\s*:\\s*\\[\\s*(\\d+)\\s*,\\s*(\\d+)\\s*]");
    private static final Pattern UV = Pattern.compile(
        "\"uv\"\\s*:\\s*\\[\\s*(-?[0-9.]+)\\s*,\\s*(-?[0-9.]+)\\s*,\\s*(-?[0-9.]+)\\s*,\\s*(-?[0-9.]+)\\s*]");

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
    void attachmentAndCartridgeIconsAre16x16() throws IOException {
        for (Path dir : List.of(TEXTURES.resolve("attachment"), TEXTURES.resolve("cartridge"))) {
            for (Path png : pngFiles(dir)) {
                var image = ImageIO.read(png.toFile());
                assertEquals(16, image.getWidth(), png + " width");
                assertEquals(16, image.getHeight(), png + " height");
            }
        }
    }

    /**
     * `WEAPON-REQ-016` (`FA-22`): every cuboid weapon or part model that declares a {@code
     * texture_size} names at least one face, and every one of those faces' {@code uv} rectangles
     * lies inside that declared size -- `tools/models.py`'s atlas packer's own contract, checked
     * against the committed output rather than by loading Minecraft.
     */
    @Test
    void everyWeaponModelElementFaceUvLiesInsideItsDeclaredTextureSize() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (Path modelFile : jsonFiles(MODELS.resolve("weapon"))) {
            String text = Files.readString(modelFile);
            Matcher sizeMatcher = TEXTURE_SIZE.matcher(text);
            if (!sizeMatcher.find()) {
                continue; // a bare per-class display parent carries no elements or texture_size.
            }
            double width = Double.parseDouble(sizeMatcher.group(1));
            double height = Double.parseDouble(sizeMatcher.group(2));
            Matcher uvMatcher = UV.matcher(text);
            boolean sawAFace = false;
            while (uvMatcher.find()) {
                sawAFace = true;
                double x0 = Double.parseDouble(uvMatcher.group(1));
                double y0 = Double.parseDouble(uvMatcher.group(2));
                double x1 = Double.parseDouble(uvMatcher.group(3));
                double y1 = Double.parseDouble(uvMatcher.group(4));
                boolean inBounds = x0 >= 0 && x0 <= width && x1 >= 0 && x1 <= width
                    && y0 >= 0 && y0 <= height && y1 >= 0 && y1 <= height;
                if (!inBounds) {
                    offenders.add(modelFile + ": uv [" + x0 + ", " + y0 + ", " + x1 + ", " + y1 + "] outside "
                        + width + "x" + height);
                }
            }
            if (!sawAFace) {
                offenders.add(modelFile + ": declares texture_size but has no faces");
            }
        }
        assertTrue(offenders.isEmpty(), "every weapon model element's face uvs lie inside its texture_size: " + offenders);
    }

    /**
     * The atlas PNG a weapon or part model's {@code textures} block names is exactly the size that
     * same model's {@code texture_size} declares -- the other half of the packer's contract, since
     * an undersized or oversized PNG would make the in-bounds check above meaningless.
     */
    @Test
    void everyWeaponAtlasPngMatchesItsModelsDeclaredTextureSize() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (Path modelFile : jsonFiles(MODELS.resolve("weapon"))) {
            String text = Files.readString(modelFile);
            Matcher sizeMatcher = TEXTURE_SIZE.matcher(text);
            if (!sizeMatcher.find()) {
                continue;
            }
            int width = Integer.parseInt(sizeMatcher.group(1));
            int height = Integer.parseInt(sizeMatcher.group(2));
            Matcher block = TEXTURES_BLOCK.matcher(text);
            assertTrue(block.find(), modelFile + " declares texture_size and so must have a textures block");
            Matcher refs = RESOURCE_LOCATION.matcher(block.group(1));
            assertTrue(refs.find(), modelFile + " names at least one texture");
            Path png = textureFileFor(refs.group(1));
            var image = ImageIO.read(png.toFile());
            if (image.getWidth() != width || image.getHeight() != height) {
                offenders.add(modelFile + ": atlas " + png + " is " + image.getWidth() + "x" + image.getHeight()
                    + " but texture_size declares " + width + "x" + height);
            }
        }
        assertTrue(offenders.isEmpty(), "every weapon atlas PNG matches its model's declared texture_size: " + offenders);
    }

    /**
     * `WEAPON-REQ-016`: every attachment named in {@code data/firearms/attachment/*.json} has a
     * cuboid part model under its own slot and name for every {@link WeaponClass} whose {@link
     * WeaponClass#hasSlot(Slot)} includes that attachment's slot -- the cross product
     * `tools/models.py`'s part-model loop is meant to have generated in full.
     */
    @Test
    void everyAttachmentHasAPartModelForEveryClassCarryingItsSlot() throws IOException {
        List<String> missing = new ArrayList<>();
        for (Path attachmentFile : jsonFiles(ATTACHMENT_DATA)) {
            String name = filenameWithoutExtension(attachmentFile);
            Matcher slotField = ATTACHMENT_SLOT_FIELD.matcher(Files.readString(attachmentFile));
            assertTrue(slotField.find(), attachmentFile + " names its own slot");
            Slot slot = Slot.valueOf(slotField.group(1).toUpperCase(Locale.ROOT));
            for (WeaponClass weaponClass : WeaponClass.values()) {
                if (!weaponClass.hasSlot(slot)) {
                    continue;
                }
                String classDir = weaponClass.name().toLowerCase(Locale.ROOT);
                String slotName = slot.name().toLowerCase(Locale.ROOT);
                Path part = MODELS.resolve("weapon/part/" + classDir + "/" + slotName + "_" + name + ".json");
                if (!Files.isRegularFile(part)) {
                    missing.add(part.toString());
                }
            }
        }
        assertTrue(missing.isEmpty(), "every attachment has a part model for every class carrying its slot: " + missing);
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

    /** FA-23: 26.2's item-model {@code transformation} is {@code Transformation.CODEC}, all four fields required. */
    @Test
    void everyItemDefinitionTransformationCarriesAllFourFields() throws IOException {
        Pattern block = Pattern.compile("\"transformation\"\\s*:\\s*\\{([^}]*)}");
        List<String> bad = new ArrayList<>();
        for (Path itemFile : jsonFiles(ITEMS)) {
            Matcher m = block.matcher(Files.readString(itemFile));
            while (m.find()) {
                String body = m.group(1);
                for (String key : List.of("translation", "left_rotation", "scale", "right_rotation")) {
                    if (!body.contains("\"" + key + "\"")) {
                        bad.add(itemFile + ": transformation lacks " + key);
                    }
                }
                if (!Pattern.compile("\"left_rotation\"\\s*:\\s*\\[[^\\]]*,[^\\]]*,[^\\]]*,[^\\]]*]").matcher(body).find()) {
                    bad.add(itemFile + ": left_rotation is not a four-element quaternion");
                }
            }
        }
        assertTrue(bad.isEmpty(), "every transformation parses as Transformation.CODEC: " + bad);
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
