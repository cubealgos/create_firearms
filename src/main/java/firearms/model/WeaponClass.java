package firearms.model;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * The six 1.0 weapon classes, each with a fixed per-class slot set shared by every weapon in that
 * class (`docs/spec/domains/weapon.md` §3, {@code WEAPON-REQ-001}). Fixed per class, not per
 * weapon, so a datapack adding a new weapon to an existing class needs no new slot logic
 * (`WEAPON-DEC-003`).
 */
public enum WeaponClass {
    PISTOL(Slot.MUZZLE, Slot.OPTIC, Slot.MAGAZINE),
    SMG(Slot.MUZZLE, Slot.OPTIC, Slot.MAGAZINE, Slot.GRIP, Slot.STOCK),
    ASSAULT_RIFLE(Slot.MUZZLE, Slot.OPTIC, Slot.MAGAZINE, Slot.GRIP, Slot.STOCK),
    DMR(Slot.MUZZLE, Slot.OPTIC, Slot.MAGAZINE, Slot.STOCK),
    SNIPER_RIFLE(Slot.MUZZLE, Slot.OPTIC, Slot.MAGAZINE, Slot.STOCK),
    SHOTGUN(Slot.MUZZLE, Slot.MAGAZINE);

    private final Set<Slot> slots;

    WeaponClass(Slot... slots) {
        this.slots = EnumSet.copyOf(Arrays.asList(slots));
    }

    /** The slots a weapon of this class carries; only these can ever hold an attachment component. */
    public Set<Slot> slots() {
        return EnumSet.copyOf(slots);
    }

    /** Whether a weapon of this class has the given slot at all (`docs/spec/domains/attach.md` `ATTACH-FAIL-001`). */
    public boolean hasSlot(Slot slot) {
        return slots.contains(slot);
    }
}
