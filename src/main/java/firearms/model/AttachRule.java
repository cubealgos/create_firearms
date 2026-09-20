package firearms.model;

/**
 * The pure half of the shared attach function (`docs/spec/domains/attach.md` `ATTACH-DEC-001`,
 * `ATTACH-REQ-001`, `002`): given a weapon's class, one of its slots, and whether that slot is
 * currently occupied, decides only whether an attachment may go there. {@code true} exactly when
 * the class has the slot at all and the slot is not already filled — never {@code true} for an
 * occupied slot (`ATTACH-FAIL-003`, `decisions/DEC-017-no-detach-durability.md`) and never {@code
 * true} for a slot the class lacks (`ATTACH-FAIL-001`). No Minecraft import: reading a real
 * weapon's current slot occupancy off its {@code firearms:attachment_<slot>} component, and
 * writing the merged result, is {@code firearms.attach}'s job (`FA-7`), which calls this rule for
 * the match decision and nothing else.
 */
public final class AttachRule {

    /**
     * Whether an attachment for {@code slot} may attach to a weapon of {@code weaponClass} whose
     * current occupancy of that slot is {@code slotOccupied}.
     */
    public static boolean canAttach(WeaponClass weaponClass, Slot slot, boolean slotOccupied) {
        return weaponClass.hasSlot(slot) && !slotOccupied;
    }

    private AttachRule() {
    }
}
