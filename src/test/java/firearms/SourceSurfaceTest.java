package firearms;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
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
 * Three claims about the source tree, checked against the files themselves: the mod opens no
 * socket of its own (COMP-REQ-001: no networking type is referenced outside Minecraft's own packet
 * API), every translation key the code names — including a damage type's death message keys, e.g.
 * {@code firearms.combat.CombatRegistration}'s {@code death.attack.firearms.bullet[.player]}
 * (`COMBAT-REQ-004`) — has an {@code en_us} entry, and no source or resource file names PUBG, its
 * weapon names, or its branding (COMP-REQ-002: every weapon here uses only its real-world
 * designation).
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
    void noSourceOrResourceFileNamesPubgOrItsBranding() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (Path file : allTextFiles()) {
            String text = Files.readString(file);
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
