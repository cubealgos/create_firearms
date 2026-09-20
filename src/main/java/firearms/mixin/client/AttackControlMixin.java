package firearms.mixin.client;

import firearms.item.ItemRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cancels vanilla's own attack-control handling while a {@code firearms:weapon} is held
 * (`docs/spec/decisions/DEC-019-controls.md`, `docs/spec/domains/weapon.md` {@code
 * WEAPON-REQ-018}'s "the attack control shall not swing, break blocks or melee-hit"): firing is
 * now this mod's own {@code firearms.client.fire.FireInputHandler}, sending {@code
 * firearms.fire.ServerboundFirePayload} off the same {@code Options.keyAttack} state, so vanilla's
 * own swing/break/melee handling for that same key press must not also run for a firearm — a bullet
 * does the hitting, never the swing.
 *
 * <p>{@code Minecraft.startAttack()} (called once per left-click press, drives block-breaking and
 * the single-press melee attack) and {@code Minecraft.continueAttack(boolean)} (called every client
 * tick to continue an in-progress break or re-check the held-down attack) are both {@code private}
 * instance methods with no public seam to override or redirect around instead — confirmed by
 * {@code javap -p} against the 26.2 merged-deobf jar, `net/minecraft/client/Minecraft.class`. Mixin
 * injection into a private method works at the bytecode level regardless, exactly like {@link
 * PlayerScopingMixin}'s own injection into a vanilla method this mod does not own the source of.
 * Both injections are at {@code HEAD}: {@code startAttack} returns {@code boolean} and is cancelled
 * via {@code setReturnValue(false)} (vanilla's own "nothing happened" return for every other reason
 * an attack does not start); {@code continueAttack} returns {@code void} and is cancelled via
 * {@code cancel()}.
 */
@Mixin(Minecraft.class)
public abstract class AttackControlMixin {

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void firearms$cancelStartAttackForFirearm(CallbackInfoReturnable<Boolean> cir) {
        if (firearms$holdingFirearm()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void firearms$cancelContinueAttackForFirearm(boolean leftClick, CallbackInfo ci) {
        if (firearms$holdingFirearm()) {
            ci.cancel();
        }
    }

    private boolean firearms$holdingFirearm() {
        Minecraft self = (Minecraft) (Object) this;
        LocalPlayer player = self.player;
        if (player == null) {
            return false;
        }
        ItemStack mainHand = player.getMainHandItem();
        return mainHand.is(ItemRegistration.WEAPON);
    }
}
