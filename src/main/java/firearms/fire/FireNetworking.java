package firearms.fire;

import firearms.item.ItemRegistration;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * The server-side receivers for {@link ServerboundFirePayload} and {@link ServerboundReloadPayload}
 * (`docs/spec/decisions/DEC-019-controls.md`, `docs/spec/domains/weapon.md` {@code WEAPON-REQ-018}):
 * each payload is a bare "the client's own input control fired" signal with no fields of its own,
 * so all either receiver does is resolve the sender's main-hand weapon and delegate to
 * {@link FiringLogic}, the same server-authoritative evaluation `WEAPON-DEC-007` already
 * established. Both {@link #handleFire} and {@link #handleReload} are public and independently
 * callable — a game test drives them directly with a mock {@link ServerPlayer} rather than through
 * real networking, since {@link ServerPlayNetworking#registerGlobalReceiver} itself needs a live
 * connection this codebase's own mock players do not open for an arbitrary custom payload.
 */
public final class FireNetworking {
    private FireNetworking() {
    }

    /** Registers both payloads' server-side receivers; common code, called once from {@code Firearms#onInitialize()}. */
    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(ServerboundFirePayload.TYPE,
            (payload, context) -> handleFire(context.player()));
        ServerPlayNetworking.registerGlobalReceiver(ServerboundReloadPayload.TYPE,
            (payload, context) -> handleReload(context.player()));
    }

    /**
     * One attack-control press or auto-fire tick's worth of fire attempt. A payload arriving while
     * the sender's main hand holds anything but this mod's own weapon item is a no-op — nothing
     * this mod owns exposes the fire control on any other item, but the payload itself carries no
     * proof of what was held when the client sent it, so the server re-checks fresh rather than
     * trusting the client's own gating (`docs/spec/04-architecture.md` {@code ARCH-DEC-005}).
     */
    public static void handleFire(ServerPlayer player) {
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!stack.is(ItemRegistration.WEAPON)) {
            return;
        }
        FiringLogic.attempt(player.level(), player, stack, InteractionHand.MAIN_HAND);
    }

    /**
     * One dedicated-reload-key press. Same main-hand-weapon guard as {@link #handleFire}; delegates
     * to {@link FiringLogic#reload}, never {@link FiringLogic#attempt}, so a reload press can never
     * fire a shot (`WEAPON-REQ-010`, `011`). Returns the {@link FiringLogic.Outcome} — unlike {@link
     * #handleFire}, whose own return doesn't need to be observable to a real client — so a game test
     * can assert on it directly without re-deriving the resulting component state by hand.
     */
    public static FiringLogic.Outcome handleReload(ServerPlayer player) {
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!stack.is(ItemRegistration.WEAPON)) {
            return FiringLogic.Outcome.NOT_A_WEAPON;
        }
        return FiringLogic.reload(player.level(), player, stack);
    }
}
