package firearms.fire;

import firearms.Firearms;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * The dedicated reload key's own client-to-server payload (`docs/spec/decisions/DEC-019-controls.md`
 * `UC-010`, `docs/spec/domains/weapon.md` {@code WEAPON-REQ-010}, {@code 011}): sent once per press
 * of the reload {@code KeyMapping} while a firearm is held ({@code firearms.client.fire
 * .FireInputHandler}). Carries no fields, for the same reason {@link ServerboundFirePayload} does
 * not: the server already knows who pressed it and what is in their main hand
 * ({@code firearms.fire.FireNetworking#handleReload}); the reload decision itself (whether ammo is
 * actually needed, which cartridges match, how many load) stays entirely server-side in
 * {@code firearms.fire.FiringLogic#reload}.
 */
public record ServerboundReloadPayload() implements CustomPacketPayload {
    private static final ServerboundReloadPayload INSTANCE = new ServerboundReloadPayload();

    public static final Type<ServerboundReloadPayload> TYPE = new Type<>(Firearms.id("reload"));

    public static final StreamCodec<ByteBuf, ServerboundReloadPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Registers this payload's type and codec on the serverbound play channel; common code, called
     * once from {@link Firearms#onInitialize()}, mirroring {@link ServerboundFirePayload#register()}.
     */
    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(TYPE, STREAM_CODEC);
    }
}
