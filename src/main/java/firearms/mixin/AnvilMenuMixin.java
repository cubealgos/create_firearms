package firearms.mixin;

import firearms.item.ItemRegistration;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Closes `WEAPON-FAIL-006`, the residual gap `docs/spec/decisions/DEC-017-no-detach-durability.md`
 * names and does not close by component omission alone: vanilla {@code AnvilMenu.createResult()}
 * carries a second, independent repair path that combines two {@link ItemStack}s of the exact same
 * {@link net.minecraft.world.item.Item} — checked by identity and {@code isDamageableItem()} only,
 * never {@code ItemStack.isValidRepairItem(ItemStack)} — and restores durability to the first from
 * the second regardless of whether either stack carries {@code DataComponents.REPAIRABLE}
 * (`docs/spec/domains/weapon.md` `WEAPON-REQ-014`). Omitting {@code .repairable(...)} at
 * registration (`firearms.item.ItemRegistration`) closes the material-repair branch but never this
 * one, since it never reaches {@code isValidRepairItem} at all.
 *
 * <p>Disassembly of {@code AnvilMenu.createResult()} against the 26.2 merged-deobf jar
 * (`javap -p -c`) shows the same-item combine branch is reached only after the method has already
 * run several sequential state mutations against its own {@code onlyRenaming}, {@code cost} and
 * {@code repairItemCountCost} fields, and the accept/reject decision for that branch itself sits
 * mid-method behind an {@code ItemStack.is(Item)}/{@code isDamageableItem()} pair with no single
 * call site a {@code @Redirect} could retarget without also matching the method's other, unrelated
 * {@code isDamageableItem()} calls (the material-repair branch above it calls it too). Injecting at
 * {@code HEAD} instead — cancelling before any of that runs once both input stacks are already known
 * to be {@code firearms:weapon} — is the narrower option of the two the ticket named: it touches
 * nothing about the method's normal control flow for any other item, vanilla or modded, and mirrors
 * exactly what vanilla itself does on every other "no result" branch of this same method
 * ({@code resultSlots.setItem(RESULT_SLOT, ItemStack.EMPTY)} then {@code cost.set(0)}).
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

    @Shadow
    @Final
    private DataSlot cost;

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void firearms$refuseWeaponCombine(CallbackInfo ci) {
        AnvilMenu self = (AnvilMenu) (Object) this;
        ItemStack left = self.getSlot(AnvilMenu.INPUT_SLOT).getItem();
        ItemStack right = self.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem();
        if (left.is(ItemRegistration.WEAPON) && right.is(ItemRegistration.WEAPON)) {
            self.getSlot(AnvilMenu.RESULT_SLOT).set(ItemStack.EMPTY);
            this.cost.set(0);
            ci.cancel();
        }
    }
}
