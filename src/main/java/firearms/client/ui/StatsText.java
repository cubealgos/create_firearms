package firearms.client.ui;

import java.util.Locale;

/**
 * Number formatting shared by every {@code WeaponTooltip} line, mirroring {@code
 * create_metered_motor}'s own {@code StatsText}: every value goes through {@link Locale#ROOT} so a
 * client's own locale never turns a decimal point into a comma mid-tooltip. Pure Java, no
 * Minecraft import, so {@code firearms.gametest.WeaponTooltipGameTest} and any plain unit test can
 * call it directly.
 */
public final class StatsText {
    private StatsText() {
    }

    /** One decimal place, e.g. a derived damage, muzzle velocity, spread or recoil value. */
    public static String oneDecimal(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    /** A whole number, e.g. a tick count, a magazine size, or durability remaining. */
    public static String whole(int value) {
        return String.format(Locale.ROOT, "%d", value);
    }

    /**
     * A multiplicative {@code Modifier}'s own factor as a signed percentage change, e.g. {@code
     * 0.90} (spread ×0.90) becomes {@code "-10%"} and {@code 1.5} (magazine size ×1.5) becomes
     * {@code "+50%"} — the attachment tooltip's own "+/-" line shape.
     */
    public static String percentChange(double factor) {
        return String.format(Locale.ROOT, "%+.0f%%", (factor - 1.0) * 100.0);
    }

    /** An additive {@code Modifier}'s own raw amount, signed, e.g. {@code "+2.0"} or {@code "-2.0"}. */
    public static String signed(double value) {
        return String.format(Locale.ROOT, "%+.1f", value);
    }
}
