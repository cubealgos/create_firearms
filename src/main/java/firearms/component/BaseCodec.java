package firearms.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * The {@code firearms:base} component's codecs (`docs/spec/contracts/data-contract.md`), following
 * {@code create_metered_motor}'s {@code StatsCodec} precedent exactly: {@code version} defaults to
 * {@link #VERSION} so an unversioned value still reads (`DATA-REQ-001`), and {@link #normalise}
 * migrates any version this build can read up to {@link #VERSION} on the spot (`DATA-REQ-002`) —
 * a placeholder migration, since 1.0 has never shipped a version below its own. A version this
 * build cannot read (greater than {@link #VERSION}) is kept exactly as saved, at its own number, so
 * {@link Base#readOnly()} reports it intact and unread (`DATA-REQ-003`).
 */
public final class BaseCodec {
    /** The schema this build writes (`docs/spec/contracts/data-contract.md` version 1). */
    public static final int VERSION = 1;

    public static final Codec<Base> CODEC = RecordCodecBuilder.create(b -> b.group(
        Codec.INT.optionalFieldOf("version", VERSION).forGetter(Base::version),
        Identifier.CODEC.fieldOf("weapon_id").forGetter(Base::weaponId)
    ).apply(b, BaseCodec::normalise));

    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Base> STREAM_CODEC =
        ByteBufCodecs.fromCodecWithRegistries(CODEC);

    private static Base normalise(int version, Identifier weaponId) {
        return new Base(version <= VERSION ? VERSION : version, weaponId);
    }

    private BaseCodec() {
    }
}
