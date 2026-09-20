package firearms.client.fire;

import firearms.fire.RecoilPacket;
import java.util.random.RandomGenerator;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Applies {@link RecoilPacket}'s cosmetic vertical/horizontal camera kick to the local player's own
 * rotation on receipt, then eases it back off over {@link RecoilKick#RECOVERY_TICKS} client ticks
 * (`COMBAT-DEC-003`, `COMBAT-REQ-011`, `FA-11`: "recovering over a few ticks"). A direct {@code
 * setXRot}/{@code setYRot} nudge, not {@link net.minecraft.world.entity.Entity#turn(double, double)}
 * — {@code turn} scales its input by vanilla's own 0.15 mouse-sensitivity constant
 * (`Entity.turn(double, double)`, confirmed by disassembly), which is the wrong unit for an
 * already-in-degrees recoil value. No easing curve beyond {@link RecoilKick}'s own even per-tick
 * division, no viewmodel animation — that belongs to the visible recoil animation rendering `FA-11`
 * scopes separately; this class only moves the camera.
 *
 * <p>The horizontal kick's own sign is randomized per shot rather than read directly off {@link
 * RecoilPacket#horizontalDegrees()}'s own sign: every 1.0 base and attachment holds {@code
 * recoilHorizontal} at {@code 0.0} ({@code firearms.model.Stats}'s own Javadoc), so there is no
 * server-decided left/right direction to preserve yet, and a real automatic weapon's horizontal
 * climb sways both ways rather than pulling one direction every shot. The packet's own magnitude
 * (its absolute value) is still what is applied, so a future non-zero horizontal stat is honoured
 * in scale, only not in sign. All of this is client-only cosmetics with no server round trip and no
 * authority over any hit, damage or ammo state.
 */
public final class RecoilHandler {
    private static RecoilKick active = RecoilKick.NONE;

    private RecoilHandler() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(RecoilPacket.TYPE, (payload, context) -> apply(context.player(), payload));
        ClientTickEvents.END_CLIENT_TICK.register(RecoilHandler::tick);
    }

    private static void apply(LocalPlayer player, RecoilPacket payload) {
        float horizontal = randomSign() * Math.abs(payload.horizontalDegrees());
        player.setXRot(player.getXRot() - payload.verticalDegrees());
        player.setYRot(player.getYRot() + horizontal);
        active = active.apply(payload.verticalDegrees(), horizontal);
    }

    /** One client tick's recovery: eases the local player's rotation back by exactly what {@link RecoilKick#tick()} recovered. */
    private static void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || active.done()) {
            return;
        }
        RecoilKick next = active.tick();
        float verticalRecovered = active.verticalRemaining() - next.verticalRemaining();
        float horizontalRecovered = active.horizontalRemaining() - next.horizontalRemaining();
        player.setXRot(player.getXRot() + verticalRecovered);
        player.setYRot(player.getYRot() - horizontalRecovered);
        active = next;
    }

    private static float randomSign() {
        return RandomGenerator.getDefault().nextBoolean() ? 1f : -1f;
    }
}
