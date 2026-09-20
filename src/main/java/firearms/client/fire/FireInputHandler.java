package firearms.client.fire;

import com.mojang.blaze3d.platform.InputConstants;
import firearms.Firearms;
import firearms.fire.ServerboundFirePayload;
import firearms.fire.ServerboundReloadPayload;
import firearms.item.ItemRegistration;
import firearms.model.FireMode;
import firearms.model.Stats;
import java.util.Optional;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * The client's own read of the two controls `docs/spec/decisions/DEC-019-controls.md`'s table
 * defines (`docs/spec/domains/weapon.md` {@code WEAPON-REQ-004}, {@code 018}): every client tick,
 * translates {@code Options.keyAttack} (fire, mode-aware) and the dedicated reload {@link
 * KeyMapping} into {@link ServerboundFirePayload}/{@link ServerboundReloadPayload} sends. This
 * class owns no server-authoritative state of its own — {@code semi}/{@code pump} send one payload
 * per press, {@code auto} sends one payload per {@code fireRateTicks}-tick interval while held, and
 * every payload's real effect (whether it actually fires, whether ammo/cooldown/durability move at
 * all) is decided entirely server-side by {@code firearms.fire.FiringLogic}, unchanged from before
 * this ticket — a lost, duplicated, or too-fast payload changes nothing about what the server
 * ultimately does.
 */
public final class FireInputHandler {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Firearms.id("firearms"));
    private static final KeyMapping RELOAD = new KeyMapping(
        "key.firearms.reload", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);

    /** {@code auto}'s own per-tick countdown until the next payload send; single-player-client scoped, reset whenever the attack key is not held. */
    private static int autoFireCountdown = 0;

    private FireInputHandler() {
    }

    public static void register() {
        KeyMappingHelper.registerKeyMapping(RELOAD);
        ClientTickEvents.END_CLIENT_TICK.register(FireInputHandler::tick);
    }

    private static void tick(Minecraft client) {
        if (client.player == null) {
            return;
        }
        ItemStack main = client.player.getItemInHand(InteractionHand.MAIN_HAND);

        if (RELOAD.consumeClick() && main.is(ItemRegistration.WEAPON)) {
            ClientPlayNetworking.send(new ServerboundReloadPayload());
        }

        if (!main.is(ItemRegistration.WEAPON)) {
            // Not holding a firearm: leave options.keyAttack alone entirely. The attack-swing-cancel
            // mixin only ever cancels Minecraft.startAttack()/continueAttack() while the main hand
            // holds firearms:weapon, so vanilla's own attack handling still runs uncancelled for
            // swords, tools and everything else — consuming or polling the key here would starve it.
            autoFireCountdown = 0;
            return;
        }

        Optional<Stats> stats = ClientLoadouts.stats(main);
        FireMode mode = stats.map(Stats::fireMode).orElse(FireMode.SEMI);
        // A missing resolution (an unrecognized weapon/attachment id, ClientLoadouts's own accepted
        // limitation) falls back to "try every tick": harmless, since the server's own ItemCooldowns
        // check in FiringLogic#attempt always rejects an attempt faster than the real derived fire
        // rate as a plain no-op — this fallback can only under- or over-poll, never over-fire.
        int fireRateTicks = stats.map(Stats::fireRateTicks).orElse(1);

        if (mode == FireMode.AUTO) {
            if (client.options.keyAttack.isDown()) {
                if (autoFireCountdown <= 0) {
                    ClientPlayNetworking.send(new ServerboundFirePayload());
                    autoFireCountdown = fireRateTicks;
                } else {
                    autoFireCountdown--;
                }
            } else {
                // Not held: reset to 0 rather than letting a stale countdown linger, so the next
                // press fires immediately instead of waiting out whatever was left over.
                autoFireCountdown = 0;
            }
        } else if (client.options.keyAttack.consumeClick()) {
            ClientPlayNetworking.send(new ServerboundFirePayload());
        }
    }
}
