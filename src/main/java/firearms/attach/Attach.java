package firearms.attach;

import firearms.component.Base;
import firearms.component.ComponentRegistration;
import firearms.data.WeaponRegistry;
import firearms.item.AttachmentItem;
import firearms.item.WeaponItem;
import firearms.model.AttachRule;
import firearms.model.Slot;
import firearms.model.WeaponBase;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * The one shared attach function both attach front ends call (`docs/spec/domains/attach.md`
 * `ATTACH-DEC-001`, `ATTACH-REQ-008`): {@link #matches(ItemStack, ItemStack)} decides whether
 * {@code addition} may attach to {@code base}, and {@link #assemble(ItemStack, ItemStack)} builds
 * the resulting stack once a caller has already confirmed that match. Neither method mutates
 * either input stack. The slot-empty-and-class-has-it decision itself is {@link
 * firearms.model.AttachRule}'s pure logic ({@code ATTACH-REQ-001}, {@code 002}); this class's own
 * job is resolving a real {@link ItemStack} pair down to the class/slot/occupancy triple that rule
 * needs, and, on a match, copying the base stack with the named slot's component set to the
 * attachment's own id, every other component unchanged (`ATTACH-REQ-003`). {@link
 * firearms.attach.AttachSmithingRecipe} (`FA-7`) and the deploying recipe (`FA-8`) are both thin
 * wrappers over these two methods — neither duplicates this match/merge logic itself
 * (`ATTACH-REQ-008`).
 */
public final class Attach {

    /**
     * Whether {@code addition} (an attachment item) may attach into {@code base} (a weapon stack):
     * {@code base} must carry a {@code firearms:base} component naming a currently-loaded weapon,
     * {@code addition} must be one of the five {@link AttachmentItem}s, and that attachment's own
     * slot must be one {@code base}'s class has and does not yet carry a component for
     * (`ATTACH-REQ-001`, `ATTACH-FAIL-001`, `ATTACH-REQ-002`/`ATTACH-FAIL-003`).
     */
    public static boolean matches(ItemStack base, ItemStack addition) {
        if (!(base.getItem() instanceof WeaponItem) || !(addition.getItem() instanceof AttachmentItem attachmentItem)) {
            return false;
        }
        Optional<WeaponBase> weaponBase = resolveWeaponBase(base);
        if (weaponBase.isEmpty()) {
            return false;
        }
        Slot slot = attachmentItem.slot();
        boolean slotOccupied = base.get(ComponentRegistration.attachmentComponent(slot)) != null;
        return AttachRule.canAttach(weaponBase.get().weaponClass(), slot, slotOccupied);
    }

    /**
     * A copy of {@code base} with {@code addition}'s own slot component set to {@code addition}'s
     * attachment id, every other component of {@code base} unchanged (`ATTACH-REQ-003`). Callers
     * must have already confirmed {@link #matches(ItemStack, ItemStack)}; this method does not
     * re-check the class/slot/occupancy rule, mirroring vanilla's own {@code
     * SmithingTrimRecipe#applyTrim} pattern (`docs/spec/04-architecture.md` `ARCH-DEC-002`).
     * Returns {@link ItemStack#EMPTY} if {@code addition} carries no slot component of its own to
     * copy (a bare, uncrafted attachment stack).
     */
    public static ItemStack assemble(ItemStack base, ItemStack addition) {
        AttachmentItem attachmentItem = (AttachmentItem) addition.getItem();
        Slot slot = attachmentItem.slot();
        Identifier attachmentId = addition.get(ComponentRegistration.attachmentComponent(slot));
        if (attachmentId == null) {
            return ItemStack.EMPTY;
        }
        ItemStack result = base.copyWithCount(1);
        result.set(ComponentRegistration.attachmentComponent(slot), attachmentId);
        return result;
    }

    private static Optional<WeaponBase> resolveWeaponBase(ItemStack base) {
        Base baseComponent = base.get(ComponentRegistration.BASE);
        if (baseComponent == null) {
            return Optional.empty();
        }
        return WeaponRegistry.get(baseComponent.weaponId());
    }

    private Attach() {
    }
}
