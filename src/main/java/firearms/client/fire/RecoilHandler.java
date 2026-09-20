package firearms.client.fire;

import firearms.fire.RecoilPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.player.LocalPlayer;

/**
 * Applies {@link RecoilPacket}'s cosmetic vertical/horizontal camera kick to the local player's own
 * rotation on receipt (`COMBAT-DEC-003`, `COMBAT-REQ-011`). Deliberately minimal: a direct {@code
 * setXRot}/{@code setYRot} nudge, not {@link net.minecraft.world.entity.Entity#turn(double, double)}
 * — {@code turn} scales its input by vanilla's own 0.15 mouse-sensitivity constant
 * (`Entity.turn(double, double)`, confirmed by disassembly), which is the wrong unit for an
 * already-in-degrees recoil value. No smoothing, no easing, no viewmodel animation — that belongs
 * to the visible recoil animation rendering `FA-11` scopes separately; this class only moves the
 * camera.
 */
public final class RecoilHandler {
    private RecoilHandler() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(RecoilPacket.TYPE, (payload, context) -> apply(context.player(), payload));
    }

    private static void apply(LocalPlayer player, RecoilPacket payload) {
        player.setXRot(player.getXRot() - payload.verticalDegrees());
        player.setYRot(player.getYRot() + payload.horizontalDegrees());
    }
}
