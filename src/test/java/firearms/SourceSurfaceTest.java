package firearms;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Claims about the source tree, checked against the files themselves: the mod opens no
 * socket of its own (COMP-REQ-001: no networking type is referenced outside Minecraft's own packet
 * API), every translation key the code names — including a damage type's death message keys, e.g.
 * {@code firearms.combat.CombatRegistration}'s {@code death.attack.firearms.bullet[.player]}
 * (`COMBAT-REQ-004`) — has an {@code en_us} entry, no source or resource file names PUBG, its
 * weapon names, or its branding (COMP-REQ-002: every weapon here uses only its real-world
 * designation), no source file imports `create_villager_customers` (`TRADE-REQ-006`: the master
 * buy-back trades work standalone, with no dependency of any kind on that sibling mod — confirmed
 * structurally alongside {@code build.gradle.kts}'s own dependency list, which names only
 * `create` and Fabric), no source file imports a vanilla menu/screen-handler registration type
 * (`UI-REQ-004`: the vanilla smithing table and Create's deployer are the only interaction
 * surfaces this mod ever adds a recipe for; no screen of its own exists to register), no source
 * file imports `VillagerProfession`/`PoiType` to register a new one (`TRADE-REQ-003`: only the two
 * existing professions are ever touched, by tag-merge), and no cartridge recipe file's own `type`
 * is a Create processing type (`AMMO-REQ-005`: the crafting-table recipe is the only path at 1.0,
 * the deferred `create:pressing`/`create:mixing` automation path named in `domains/ammo.md` §3
 * unbuilt).
 */
final class SourceSurfaceTest {
    private static final Path MAIN = Path.of("src/main/java");
    private static final Path RESOURCES = Path.of("src/main/resources");
    private static final Path LANG = Path.of("src/main/resources/assets/firearms/lang/en_us.json");
    private static final Pattern NETWORKING = Pattern.compile(
        "java\\.net\\.|java\\.nio\\.channels\\.|HttpClient|Socket|URLConnection|HttpURLConnection");
    private static final Pattern KEY = Pattern.compile(
        "\"((?:item|block|screen|tooltip|command)\\.firearms\\.[a-z_.]+|death\\.attack\\.firearms\\.[a-z_.]+)\"");
    private static final Pattern PUBG = Pattern.compile("pubg|playerunknown", Pattern.CASE_INSENSITIVE);
    private static final Pattern VILLAGER_CUSTOMERS_IMPORT = Pattern.compile(
        "^import\\s+[a-zA-Z0-9_.]*villager[a-zA-Z0-9_.]*customer[a-zA-Z0-9_.]*;", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern MENU_TYPE_IMPORT = Pattern.compile(
        "^import\\s+net\\.minecraft\\.world\\.inventory\\.(MenuType|AbstractContainerMenu);", Pattern.MULTILINE);
    private static final Pattern PROFESSION_OR_POI_IMPORT = Pattern.compile(
        "^import\\s+net\\.minecraft\\.world\\.entity\\.npc\\.villager\\.VillagerProfession;"
            + "|^import\\s+net\\.minecraft\\.world\\.entity\\.ai\\.village\\.poi\\.PoiType(s)?;",
        Pattern.MULTILINE);
    private static final Path RECIPE_DATA = Path.of("src/main/resources/data/firearms/recipe");
    private static final Pattern CREATE_PROCESSING_RECIPE_TYPE = Pattern.compile(
        "\"type\"\\s*:\\s*\"create:(mixing|pressing)\"");

    @Test
    void noNetworkingTypeIsReferencedByTheMod() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (Path file : javaSources()) {
            String text = Files.readString(file);
            Matcher m = NETWORKING.matcher(text);
            if (m.find()) {
                offenders.add(file + " mentions " + m.group());
            }
        }
        assertTrue(offenders.isEmpty(), "COMP-REQ-001: no outbound networking, but " + offenders);
    }

    @Test
    void everyTranslationKeyNamedInCodeHasAnEnglishEntry() throws IOException {
        String lang = Files.readString(LANG);
        List<String> missing = new ArrayList<>();
        for (Path file : javaSources()) {
            Matcher m = KEY.matcher(Files.readString(file));
            while (m.find()) {
                String key = m.group(1);
                // Keys ending in a dot are prefixes completed at runtime.
                if (!key.endsWith(".") && !lang.contains("\"" + key + "\"")) {
                    missing.add(key + " in " + file.getFileName());
                }
            }
        }
        assertTrue(missing.isEmpty(), "every key has an en_us entry, but " + missing);
    }

    @Test
    void noSourceFileImportsCreateVillagerCustomers() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (Path file : javaSources()) {
            String text = Files.readString(file);
            Matcher m = VILLAGER_CUSTOMERS_IMPORT.matcher(text);
            if (m.find()) {
                offenders.add(file + " imports " + m.group());
            }
        }
        assertTrue(offenders.isEmpty(), "TRADE-REQ-006: no dependency of any kind on create_villager_customers, but " + offenders);
    }

    @Test
    void noSourceFileImportsAVanillaMenuRegistrationType() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (Path file : javaSources()) {
            String text = Files.readString(file);
            Matcher m = MENU_TYPE_IMPORT.matcher(text);
            if (m.find()) {
                offenders.add(file + " imports " + m.group());
            }
        }
        assertTrue(offenders.isEmpty(), "UI-REQ-004: no screen of this mod's own, so nothing registers a MenuType, but " + offenders);
    }

    @Test
    void noSourceFileRegistersANewVillagerProfessionOrPointOfInterest() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (Path file : javaSources()) {
            String text = Files.readString(file);
            Matcher m = PROFESSION_OR_POI_IMPORT.matcher(text);
            if (m.find()) {
                offenders.add(file + " imports " + m.group());
            }
        }
        assertTrue(offenders.isEmpty(),
            "TRADE-REQ-003: no new villager profession, job-site block, or point-of-interest type, but " + offenders);
    }

    @Test
    void noCartridgeRecipeUsesACreateProcessingType() throws IOException {
        List<String> offenders = new ArrayList<>();
        if (Files.isDirectory(RECIPE_DATA)) {
            try (Stream<Path> walk = Files.walk(RECIPE_DATA)) {
                for (Path file : walk.filter(p -> p.toString().endsWith(".json")).toList()) {
                    String text = Files.readString(file);
                    Matcher m = CREATE_PROCESSING_RECIPE_TYPE.matcher(text);
                    if (m.find()) {
                        offenders.add(file + " uses " + m.group());
                    }
                }
            }
        }
        assertTrue(offenders.isEmpty(),
            "AMMO-REQ-005: no Create automation recipe (pressing/mixing) for cartridges at 1.0, but " + offenders);
    }

    @Test
    void noSourceOrResourceFileNamesPubgOrItsBranding() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (Path file : allTextFiles()) {
            // ISO-8859-1 rather than Files.readString's strict UTF-8: every byte maps to exactly
            // one char, so this never throws on a binary file's content, and FA-9's own PNG
            // sprites live under src/main/resources too — COMP-REQ-002 ("no texture ... references
            // PUBG") means this scan must cover them, not choke on them.
            String text = new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1);
            if (PUBG.matcher(text).find()) {
                offenders.add(file.toString());
            }
        }
        assertTrue(offenders.isEmpty(), "COMP-REQ-002: no PUBG name or branding, but " + offenders);
    }

    private static List<Path> javaSources() throws IOException {
        try (Stream<Path> walk = Files.walk(MAIN)) {
            return walk.filter(p -> p.toString().endsWith(".java")).toList();
        }
    }

    /**
     * Every source or resource file this mod ships, except the binary media types under {@code
     * assets/} (sound, image, font) — {@code .ogg} placeholders landed at `FA-6`
     * (`assets/firearms/sounds/`) are the first of those, and a naive UTF-8 {@code readString}
     * over one throws {@link java.nio.charset.MalformedInputException}; item/model textures land
     * later (`FA-9`) and would hit the identical failure without this filter.
     */
    private static final Set<String> BINARY_EXTENSIONS = Set.of(".ogg", ".png", ".jpg", ".jpeg", ".ttf", ".otf");

    private static List<Path> allTextFiles() throws IOException {
        try (Stream<Path> mainWalk = Files.walk(MAIN); Stream<Path> resourcesWalk = Files.walk(RESOURCES)) {
            List<Path> files = new ArrayList<>();
            mainWalk.filter(Files::isRegularFile).filter(SourceSurfaceTest::isText).forEach(files::add);
            resourcesWalk.filter(Files::isRegularFile).filter(SourceSurfaceTest::isText).forEach(files::add);
            return files;
        }
    }

    private static boolean isText(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String extension = dot < 0 ? "" : name.substring(dot).toLowerCase(Locale.ROOT);
        return !BINARY_EXTENSIONS.contains(extension);
    }
}
