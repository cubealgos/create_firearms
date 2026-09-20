package firearms.mixin.client;

import firearms.client.scope.ScopedWeapon;
import java.util.OptionalDouble;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Reads the attached optic's own zoom factor for the FOV modifier, in place of vanilla's fresh
 * hardcoded {@code 0.1f} literal inside {@code AbstractClientPlayer.getFieldOfViewModifier(boolean,
 * float)} (`COMBAT-REQ-007`). `javap -p -c` against the 26.2 merged-deobf jar confirms this literal
 * is a second, independent {@code 0.1f} — never a read of {@code SpyglassItem.ZOOM_FOV_MODIFIER} —
 * the only occurrence of that value in the method, sitting behind {@code isFirstPerson &&
 * isScoping()} (the same gate {@link PlayerScopingMixin} widens):
 *
 * <pre>
 * 105: iload_1                          // isFirstPerson
 * 106: ifeq 119
 * 109: invokevirtual isScoping:()Z
 * 113: ifeq 119
 * 116: ldc #167  // float 0.1f
 * 118: freturn
 * </pre>
 *
 * <p>FOV modifier is the reciprocal of zoom — the spyglass's own {@code 0.1f} is exactly {@code 1 /
 * 10}, so a 4x optic reports {@code 0.25f}, a 2x optic {@code 0.5f}, and so on
 * (`docs/spec/domains/attach.md` §3). Only overrides the constant while the scoping item is a
 * {@code firearms:weapon} with a magnifying optic attached; every other cause of {@code
 * isScoping()} returning {@code true} (the vanilla spyglass, or another mod's own stand-in) still
 * gets vanilla's own {@code 0.1f} unchanged. Red dot and holo never reach this override at all —
 * {@code ScopedWeapon#zoom} is empty for them, so {@code isScoping()} itself never returns {@code
 * true} for them either (`COMBAT-DEC-004`).
 */
@Mixin(AbstractClientPlayer.class)
public abstract class FieldOfViewMixin {

    @ModifyConstant(method = "getFieldOfViewModifier", constant = @Constant(floatValue = 0.1f))
    private float firearms$zoomFovModifier(float vanillaSpyglassModifier) {
        AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
        ItemStack used = self.getUseItem();
        OptionalDouble zoom = ScopedWeapon.zoom(used);
        if (zoom.isEmpty()) {
            return vanillaSpyglassModifier;
        }
        return (float) (1.0 / zoom.getAsDouble());
    }
}
