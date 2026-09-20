package firearms.model;

import java.util.function.Consumer;

/**
 * The out-of-range clamp-and-log path a datapack's own weapon/attachment data can trigger
 * (`docs/spec/contracts/public-surface.md` {@code SURFACE-REQ-002}): a derived stat that falls
 * outside its valid range is clamped to the nearest valid value and reported once, rather than
 * crashing the derivation, mirroring {@code create_synthetic_diamonds}'s {@code WeightedPick.of}
 * and {@code create_villager_customers}'s own {@code SURFACE-REQ-002} precedent.
 */
final class Clamp {
    private Clamp() {
    }

    /** Clamps {@code value} to at least {@code min}, reporting once through {@code onClamped} when it does. */
    static double atLeast(String field, double value, double min, Consumer<String> onClamped) {
        if (value < min) {
            onClamped.accept(field + " " + value + " is below the minimum " + min + "; clamped to " + min + " (SURFACE-REQ-002)");
            return min;
        }
        return value;
    }

    /** Clamps {@code value} to at least {@code min}, reporting once through {@code onClamped} when it does. */
    static int atLeast(String field, int value, int min, Consumer<String> onClamped) {
        if (value < min) {
            onClamped.accept(field + " " + value + " is below the minimum " + min + "; clamped to " + min + " (SURFACE-REQ-002)");
            return min;
        }
        return value;
    }
}
