package firearms.mixin.client;

import firearms.client.scope.ScopeOverlay;
import firearms.client.scope.ScopeZoom;
import firearms.client.scope.ScopedWeapon;
import firearms.model.Attachment;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Swaps the texture {@code Hud.extractSpyglassOverlay(GuiGraphicsExtractor, float)} draws off
 * vanilla's own hardcoded {@code SPYGLASS_SCOPE_LOCATION} static field for the held weapon's optic
 * overlay (`COMBAT-REQ-008`). `javap -p -c` against the 26.2 merged-deobf jar confirms the field is
 * read once, directly at the {@code blit(...)} call site — {@code getstatic
 * SPYGLASS_SCOPE_LOCATION} immediately followed by the {@code blit} invocation — so a {@code
 * @Redirect} on that one field read is the entire change needed; nothing else in the method touches
 * the field. `extractSpyglassOverlay` is itself only ever called from {@code
 * extractCameraOverlays}, behind {@code LocalPlayer.isScoping()} (the same gate {@link
 * PlayerScopingMixin} widens), so no further gating belongs here beyond "does the local player's
 * used item resolve to one of our own magnifying optics" — everything else (the vanilla spyglass,
 * another mod's own scoping stand-in, or a firearm with red dot/holo that never reaches {@code
 * isScoping() == true} at all, `COMBAT-DEC-004`) falls back to vanilla's own texture unchanged.
 */
@Mixin(Hud.class)
public abstract class ScopeOverlayMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private static Identifier SPYGLASS_SCOPE_LOCATION;

    @Redirect(
        method = "extractSpyglassOverlay",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/gui/Hud;SPYGLASS_SCOPE_LOCATION:Lnet/minecraft/resources/Identifier;"
        )
    )
    private Identifier firearms$scopeOverlayTexture() {
        LocalPlayer player = this.minecraft.player;
        if (player == null) {
            return SPYGLASS_SCOPE_LOCATION;
        }
        ItemStack used = player.getUseItem();
        Optional<Attachment> optic = ScopedWeapon.optic(used);
        if (optic.isPresent() && ScopeZoom.of(optic.get()).isPresent()) {
            return ScopeOverlay.texture(optic.get());
        }
        return SPYGLASS_SCOPE_LOCATION;
    }
}
