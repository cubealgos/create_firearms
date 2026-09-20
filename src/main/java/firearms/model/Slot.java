package firearms.model;

/**
 * The five attachment slots, declared in the fixed application order the stat derivation function
 * folds modifiers in: muzzle, optic, magazine, grip, stock (`docs/spec/domains/weapon.md`
 * {@code WEAPON-REQ-003}). Enum declaration order <em>is</em> that fixed order — {@link
 * StatDerivation} iterates {@link #values()} rather than re-deciding the order per call site.
 * Which slots exist on which class is {@link WeaponClass}'s own concern, not this enum's.
 */
public enum Slot {
    MUZZLE,
    OPTIC,
    MAGAZINE,
    GRIP,
    STOCK
}
