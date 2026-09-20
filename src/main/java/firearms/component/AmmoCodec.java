package firearms.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * The {@code firearms:ammo} component's persistent and network codecs. {@code loaded} is rejected
 * at decode time when negative, via {@link Codec#validate}, rather than left to a throwing
 * constructor — a decode failure is what lets a malformed value degrade to "component absent" per
 * `docs/spec/contracts/data-contract.md` {@code DATA-REQ-004}, instead of crashing the read.
 */
public final class AmmoCodec {
    private static final Codec<Integer> NON_NEGATIVE_INT = Codec.INT.validate(loaded ->
        loaded < 0
            ? DataResult.error(() -> "loaded must not be negative, was " + loaded)
            : DataResult.success(loaded));

    public static final Codec<Ammo> CODEC = RecordCodecBuilder.create(b -> b.group(
        Identifier.CODEC.fieldOf("caliber").forGetter(Ammo::caliber),
        NON_NEGATIVE_INT.fieldOf("loaded").forGetter(Ammo::loaded)
    ).apply(b, Ammo::new));

    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Ammo> STREAM_CODEC =
        ByteBufCodecs.fromCodecWithRegistries(CODEC);

    private AmmoCodec() {
    }
}
