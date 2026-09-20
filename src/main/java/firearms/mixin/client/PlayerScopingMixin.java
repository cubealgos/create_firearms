package firearms.mixin.client;

import firearms.client.scope.ScopedWeapon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Widens {@code Player.isScoping()} — in vanilla an exact {@code Items.SPYGLASS} identity check,
 * {@code isUsingItem() && getUseItem().is(Items.SPYGLASS)}, never keyed on the use-animation
 * (confirmed by `javap -p -c` against the 26.2 merged-deobf jar; research
 * `smithing-and-item-model-layers-26-2.md` §C.1) — to also return {@code true} while a player is
 * using a {@code firearms:weapon} stack with a magnifying optic attached (`COMBAT-REQ-006`). This
 * one method is the single shared gate {@code FieldOfViewMixin} and {@code ScopeOverlayMixin} both
 * read, and the gate {@code ItemInHandRenderer.submitArmWithItem}'s own {@code isScoping(); ifeq;
 * return} already checks with no mixin of its own needed — widening it here is the entire cost of
 * getting the FOV zoom, the overlay, and the held-item/arm suppression together
 * (`docs/spec/decisions/DEC-009-scope-mechanic.md`).
 *
 * <p>Injects at {@code RETURN} rather than replacing the method outright, and only ever flips a
 * {@code false} to {@code true} — never the reverse — so this stays additive alongside a second
 * mod's own {@code isScoping()} widening rather than colliding with it (`COMBAT-FAIL-003`).
 */
@Mixin(Player.class)
public abstract class PlayerScopingMixin {

    @Inject(method = "isScoping", at = @At("RETURN"), cancellable = true)
    private void firearms$widenForZoomingOptic(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            return; // already scoping (vanilla spyglass, or another mod's own widening) — additive only.
        }
        Player self = (Player) (Object) this;
        if (!self.isUsingItem()) {
            return;
        }
        ItemStack used = self.getUseItem();
        if (ScopedWeapon.hasZoomingOptic(used)) {
            cir.setReturnValue(true);
        }
    }
}
