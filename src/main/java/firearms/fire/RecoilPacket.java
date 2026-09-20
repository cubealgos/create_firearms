package firearms.fire;

import firearms.Firearms;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * The cosmetic recoil kick sent to the shooting player only, decoupled from the server-authoritative
 * spread roll {@code firearms.combat.BulletSpawner} already applied (`docs/spec/domains/combat.md`
 * {@code COMBAT-DEC-003}, {@code COMBAT-REQ-011}): a vertical and a horizontal camera-angle nudge in
 * degrees, applied client-side by {@code firearms.client.fire.RecoilHandler}. Carries no authority
 * over any hit, damage or ammo state — losing or delaying this packet changes nothing about what
 * already happened on the server, mirroring the potato cannon's own cosmetic-packet split (research
 * `create-fly-potato-cannon-and-deploying-26-2.md` §A.4, §A.5).
 *
 * @param verticalDegrees the upward camera kick, in degrees (`domains/weapon.md` §3's "Recoil (°)" column)
 * @param horizontalDegrees the sideways camera kick, in degrees ({@code Stats#recoilHorizontal()}, {@code 0.0} for every 1.0 base and attachment)
 */
public record RecoilPacket(float verticalDegrees, float horizontalDegrees) implements CustomPacketPayload {
    public static final Type<RecoilPacket> TYPE = new Type<>(Firearms.id("recoil"));

    public static final StreamCodec<ByteBuf, RecoilPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT, RecoilPacket::verticalDegrees,
        ByteBufCodecs.FLOAT, RecoilPacket::horizontalDegrees,
        RecoilPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Registers this payload's type and codec on the clientbound play channel; common code, called
     * once from {@link Firearms#onInitialize()} — both logical sides need the type/codec
     * registration to negotiate the channel, even though only the server ever sends one and only
     * {@code firearms.client.fire.RecoilHandler} (client-only code) ever receives one.
     */
    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(TYPE, STREAM_CODEC);
    }
}
