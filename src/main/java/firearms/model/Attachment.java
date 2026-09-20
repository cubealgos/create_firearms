package firearms.model;

import java.util.List;

/**
 * An attachment item: its id, slot, stat modifiers, and (optic-only) zoom. The 22 constants below
 * are `docs/spec/domains/attach.md` §3's table, literally (`ATTACH-DEC-001`'s "universal by slot"
 * framing: any attachment fits any class that has its slot, regardless of class) — proposed
 * starting numbers, not balanced, per that section's own note; this ticket ships them as-is and
 * defers retuning to the balance sweep (`FA-14`). A datapack's own attachment data files are
 * `FA-3`'s territory, not this record's.
 *
 * @param id the attachment's own identifier
 * @param slot the one slot this attachment fits
 * @param modifiers this attachment's effect on the base weapon's stats, folded by {@link
 *     StatDerivation}
 * @param zoom the scope magnification an optic contributes, {@code 1.0} ("none") for every
 *     non-optic attachment and for the non-magnifying optics (red dot, holo); read only by {@link
 *     StatDerivation} as a slot override, never folded as a {@link Modifier}
 *     (`docs/spec/domains/attach.md` §3)
 */
public record Attachment(String id, Slot slot, List<Modifier> modifiers, double zoom) {

    public Attachment {
        modifiers = List.copyOf(modifiers);
    }

    private static Attachment of(String id, Slot slot, Modifier... modifiers) {
        return new Attachment(id, slot, List.of(modifiers), 1.0);
    }

    // -- Muzzle --

    /** Suppressor: spread ×0.90, muzzle velocity ×0.97. */
    public static final Attachment SUPPRESSOR = of(
        "suppressor", Slot.MUZZLE,
        Modifier.multiply(Stat.SPREAD, 0.90), Modifier.multiply(Stat.MUZZLE_VELOCITY, 0.97)
    );

    /** Compensator: recoil ×0.75. */
    public static final Attachment COMPENSATOR = of(
        "compensator", Slot.MUZZLE, Modifier.multiply(Stat.RECOIL_VERTICAL, 0.75)
    );

    /** Flash hider: spread ×0.95. */
    public static final Attachment FLASH_HIDER = of(
        "flash_hider", Slot.MUZZLE, Modifier.multiply(Stat.SPREAD, 0.95)
    );

    // -- Optic --

    /** Red dot: spread-while-aiming ×0.85, zoom none (1.0). */
    public static final Attachment RED_DOT = new Attachment(
        "red_dot", Slot.OPTIC, List.of(Modifier.multiply(Stat.SPREAD_WHILE_AIMING, 0.85)), 1.0
    );

    /** Holo: spread-while-aiming ×0.85, zoom none (1.0). */
    public static final Attachment HOLO = new Attachment(
        "holo", Slot.OPTIC, List.of(Modifier.multiply(Stat.SPREAD_WHILE_AIMING, 0.85)), 1.0
    );

    /** 2x scope: spread-while-aiming ×0.75, zoom 2.0. */
    public static final Attachment SCOPE_2X = new Attachment(
        "scope_2x", Slot.OPTIC, List.of(Modifier.multiply(Stat.SPREAD_WHILE_AIMING, 0.75)), 2.0
    );

    /** 3x scope: spread-while-aiming ×0.65, zoom 3.0. */
    public static final Attachment SCOPE_3X = new Attachment(
        "scope_3x", Slot.OPTIC, List.of(Modifier.multiply(Stat.SPREAD_WHILE_AIMING, 0.65)), 3.0
    );

    /** 4x scope: spread-while-aiming ×0.55, zoom 4.0. */
    public static final Attachment SCOPE_4X = new Attachment(
        "scope_4x", Slot.OPTIC, List.of(Modifier.multiply(Stat.SPREAD_WHILE_AIMING, 0.55)), 4.0
    );

    /** 6x scope: spread-while-aiming ×0.45, zoom 6.0. */
    public static final Attachment SCOPE_6X = new Attachment(
        "scope_6x", Slot.OPTIC, List.of(Modifier.multiply(Stat.SPREAD_WHILE_AIMING, 0.45)), 6.0
    );

    /** 8x scope: spread-while-aiming ×0.35, zoom 8.0. */
    public static final Attachment SCOPE_8X = new Attachment(
        "scope_8x", Slot.OPTIC, List.of(Modifier.multiply(Stat.SPREAD_WHILE_AIMING, 0.35)), 8.0
    );

    /** 15x scope: spread-while-aiming ×0.25, zoom 15.0. */
    public static final Attachment SCOPE_15X = new Attachment(
        "scope_15x", Slot.OPTIC, List.of(Modifier.multiply(Stat.SPREAD_WHILE_AIMING, 0.25)), 15.0
    );

    // -- Magazine --

    /** Extended magazine: magazine size ×1.5, reload ×1.1. */
    public static final Attachment EXTENDED_MAGAZINE = of(
        "extended_magazine", Slot.MAGAZINE,
        Modifier.multiply(Stat.MAGAZINE_SIZE, 1.5), Modifier.multiply(Stat.RELOAD_TICKS, 1.1)
    );

    /** Quickdraw magazine: reload ×0.8. */
    public static final Attachment QUICKDRAW_MAGAZINE = of(
        "quickdraw_magazine", Slot.MAGAZINE, Modifier.multiply(Stat.RELOAD_TICKS, 0.8)
    );

    /** Extended quickdraw magazine: magazine size ×1.3, reload ×0.95. */
    public static final Attachment EXTENDED_QUICKDRAW_MAGAZINE = of(
        "extended_quickdraw_magazine", Slot.MAGAZINE,
        Modifier.multiply(Stat.MAGAZINE_SIZE, 1.3), Modifier.multiply(Stat.RELOAD_TICKS, 0.95)
    );

    // -- Grip --

    /** Vertical grip: recoil ×0.85. */
    public static final Attachment VERTICAL_GRIP = of(
        "vertical_grip", Slot.GRIP, Modifier.multiply(Stat.RECOIL_VERTICAL, 0.85)
    );

    /** Angled grip: reload ×0.9. */
    public static final Attachment ANGLED_GRIP = of(
        "angled_grip", Slot.GRIP, Modifier.multiply(Stat.RELOAD_TICKS, 0.9)
    );

    /** Half grip: spread ×0.9. */
    public static final Attachment HALF_GRIP = of(
        "half_grip", Slot.GRIP, Modifier.multiply(Stat.SPREAD, 0.9)
    );

    /** Light grip: fire rate ×0.95 (faster — fewer ticks between shots). */
    public static final Attachment LIGHT_GRIP = of(
        "light_grip", Slot.GRIP, Modifier.multiply(Stat.FIRE_RATE_TICKS, 0.95)
    );

    /** Thumb grip: recoil ×0.9. */
    public static final Attachment THUMB_GRIP = of(
        "thumb_grip", Slot.GRIP, Modifier.multiply(Stat.RECOIL_VERTICAL, 0.9)
    );

    // -- Stock --

    /** Tactical stock: recoil ×0.85. */
    public static final Attachment TACTICAL_STOCK = of(
        "tactical_stock", Slot.STOCK, Modifier.multiply(Stat.RECOIL_VERTICAL, 0.85)
    );

    /** Cheek pad: spread ×0.9. */
    public static final Attachment CHEEK_PAD = of(
        "cheek_pad", Slot.STOCK, Modifier.multiply(Stat.SPREAD, 0.9)
    );

    /** Bullet loops: reload ×0.85. */
    public static final Attachment BULLET_LOOPS = of(
        "bullet_loops", Slot.STOCK, Modifier.multiply(Stat.RELOAD_TICKS, 0.85)
    );

    /** All 22 attachments, in the table's own order, muzzle through stock. */
    public static final List<Attachment> ALL = List.of(
        SUPPRESSOR, COMPENSATOR, FLASH_HIDER,
        RED_DOT, HOLO, SCOPE_2X, SCOPE_3X, SCOPE_4X, SCOPE_6X, SCOPE_8X, SCOPE_15X,
        EXTENDED_MAGAZINE, QUICKDRAW_MAGAZINE, EXTENDED_QUICKDRAW_MAGAZINE,
        VERTICAL_GRIP, ANGLED_GRIP, HALF_GRIP, LIGHT_GRIP, THUMB_GRIP,
        TACTICAL_STOCK, CHEEK_PAD, BULLET_LOOPS
    );
}
