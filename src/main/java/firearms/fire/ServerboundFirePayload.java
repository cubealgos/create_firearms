package firearms.fire;

import firearms.Firearms;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * The attack control's own client-to-server payload (`docs/spec/decisions/DEC-019-controls.md`,
 * `docs/spec/domains/weapon.md` {@code WEAPON-REQ-018}): sent once per press for {@code semi}/
 * {@code pump}, once per fire-rate interval while held for {@code auto}
 * ({@code firearms.client.fire.FireInputHandler}). Carries no fields — the server already knows
 * who sent it ({@code ServerPlayNetworking.Context#player()}) and what is in their main hand
 * (`firearms.fire.FireNetworking#handleFire` reads {@code player.getItemInHand}), so there is
 * nothing left for the client to report; every real decision (cooldown, ammo, spread, damage)
 * stays server-authoritative in {@code firearms.fire.FiringLogic#attempt}, unchanged from before
 * this payload existed (`DEC-019`'s "validated server-side by the same `FiringLogic`").
 */
public record ServerboundFirePayload() implements CustomPacketPayload {
    private static final ServerboundFirePayload INSTANCE = new ServerboundFirePayload();

    public static final Type<ServerboundFirePayload> TYPE = new Type<>(Firearms.id("fire"));

    public static final StreamCodec<ByteBuf, ServerboundFirePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Registers this payload's type and codec on the serverbound play channel; common code, called
     * once from {@link Firearms#onInitialize()} — {@code PayloadTypeRegistry} is common-safe (its
     * package is {@code net.fabricmc.fabric.api.networking.v1}, not a client-only one), even though
     * only the client ever sends this payload and only the server ({@code FireNetworking}) ever
     * receives it.
     */
    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(TYPE, STREAM_CODEC);
    }
}
