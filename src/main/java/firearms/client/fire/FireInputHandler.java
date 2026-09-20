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
 *
 * <p><b>FA-26</b> (Kevin: "I can't shoot the AWM; shooting on the AKM works fine"): {@code semi}/
 * {@code pump} used to gate their one-payload-per-press on {@code Options.keyAttack.consumeClick()}
 * — the exact same shared {@link KeyMapping} vanilla's own {@code Minecraft.handleKeybinds()}
 * polls. Decompiling the shipped 26.2 client showed that method, while {@code
 * this.player.isUsingItem()} is {@code true} (aiming, per `docs/spec/decisions/
 * DEC-019-controls.md` — aim <em>is</em> the held use control now), runs exactly this and nothing
 * else for the attack key: {@code while (this.options.keyAttack.consumeClick()) { }} — draining
 * and discarding every pending click before this class's own {@code
 * ClientTickEvents.END_CLIENT_TICK} listener (which always runs later in the same tick) ever gets
 * to call {@code consumeClick()} itself. A press-while-aiming is real-world-typical for a scoped
 * sniper rifle (the AWM's own class) and atypical for hip-fired spray weapons (the AKM's own {@code
 * auto} mode, which was never affected: {@link #tick} already read {@code isDown()} for {@code
 * auto}, never {@code consumeClick()}, so it never competed for the same shared click count) —
 * matching Kevin's exact report. Any {@code semi}/{@code pump} weapon fired while aiming hit this,
 * not only the AWM; the AWM is simply the one class (`sniper_rifle`) a player naturally aims before
 * firing. The fix: track the attack key's own down/up edge in this class's own state ({@link
 * #attackWasDown}) instead of consuming vanilla's shared click count at all, the same
 * never-consumed {@code isDown()} technique {@code auto} already used — so this class no longer
 * competes with {@code Minecraft.handleKeybinds()} for the same counter regardless of whether the
 * player is aiming. Not reachable by a server-only game test (`docs/spec/operations/testing.md`'s
 * own "what is genuinely hard" section: input polling needs a running client), so no automated
 * regression proves this fix the way a game test could a server-side bug — release-checklist item
 * instead.
 */
public final class FireInputHandler {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Firearms.id("firearms"));
    private static final KeyMapping RELOAD = new KeyMapping(
        "key.firearms.reload", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);

    /** {@code auto}'s own per-tick countdown until the next payload send; single-player-client scoped, reset whenever the attack key is not held. */
    private static int autoFireCountdown = 0;

    /**
     * {@code semi}/{@code pump}'s own edge-detected state (FA-26): whether {@code
     * Options.keyAttack} was down on the previous tick this class observed it, so a press fires
     * exactly once on the down-edge without ever calling {@code consumeClick()} on that shared key
     * mapping (see this class's own Javadoc for why that raced vanilla's {@code
     * handleKeybinds()} while aiming). Reset to {@code false} whenever the main hand stops holding a
     * firearm, so drawing a weapon while the mouse button happens to already be held down never
     * fires on the very next tick.
     */
    private static boolean attackWasDown = false;

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
            attackWasDown = false;
            return;
        }

        Optional<Stats> stats = ClientLoadouts.stats(main);
        FireMode mode = stats.map(Stats::fireMode).orElse(FireMode.SEMI);
        // A missing resolution (an unrecognized weapon/attachment id, ClientLoadouts's own accepted
        // limitation) falls back to "try every tick": harmless, since the server's own ItemCooldowns
        // check in FiringLogic#attempt always rejects an attempt faster than the real derived fire
        // rate as a plain no-op — this fallback can only under- or over-poll, never over-fire.
        int fireRateTicks = stats.map(Stats::fireRateTicks).orElse(1);
        boolean attackDown = client.options.keyAttack.isDown();

        if (mode == FireMode.AUTO) {
            if (attackDown) {
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
        } else if (attackDown && !attackWasDown) {
            // FA-26: isDown()'s own down-edge, never consumeClick() — see this class's Javadoc.
            ClientPlayNetworking.send(new ServerboundFirePayload());
        }
        attackWasDown = attackDown;
    }
}
