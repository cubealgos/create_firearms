package firearms.model;

/**
 * The six 1.0 calibres, one per weapon, none repeated (`docs/spec/domains/weapon.md`
 * {@code WEAPON-DEC-002}); the matching set of cartridges is `docs/spec/domains/ammo.md`'s own
 * territory, not this package's. {@link #displayId()} is the exact real-world designation the spec
 * tables use, kept distinct from the enum constant name since a calibre name is not a valid Java
 * identifier as written.
 */
public enum Caliber {
    ACP_45(".45 ACP"),
    MM_9("9mm"),
    MM_7_62("7.62mm"),
    MM_5_56("5.56mm"),
    MAGNUM_300(".300 Magnum"),
    GAUGE_12("12 gauge");

    private final String displayId;

    Caliber(String displayId) {
        this.displayId = displayId;
    }

    /** The real-world designation as `docs/spec/domains/weapon.md` and `domains/ammo.md` write it. */
    public String displayId() {
        return displayId;
    }
}
