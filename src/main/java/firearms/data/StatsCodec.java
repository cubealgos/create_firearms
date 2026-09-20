package firearms.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import firearms.model.FireMode;
import firearms.model.Stats;

/**
 * A base weapon's bare {@link Stats} (`docs/spec/domains/weapon.md` §3's roster table columns).
 * {@code spread_while_aiming}, {@code fire_rate_ticks}, {@code magazine_size} and {@code
 * reload_ticks} are required fields, spelled out in every 1.0 weapon file exactly as {@code
 * firearms.model.WeaponBase}'s own constants do; {@code recoil_horizontal}, {@code pellets} and
 * {@code zoom} are optional, defaulting to the values every 1.0 base already uses ({@link
 * Stats#recoilHorizontal()}'s and {@link Stats#zoom()}'s own Javadoc), so a datapack weapon that
 * does not care about them need not name them.
 */
final class StatsCodec {
    static final Codec<FireMode> FIRE_MODE = EnumCodec.of(FireMode.class);

    static final Codec<Stats> CODEC = RecordCodecBuilder.create(b -> b.group(
        Codec.DOUBLE.fieldOf("damage").forGetter(Stats::damage),
        Codec.DOUBLE.fieldOf("muzzle_velocity").forGetter(Stats::muzzleVelocity),
        Codec.DOUBLE.fieldOf("spread").forGetter(Stats::spread),
        Codec.DOUBLE.fieldOf("spread_while_aiming").forGetter(Stats::spreadWhileAiming),
        Codec.INT.fieldOf("fire_rate_ticks").forGetter(Stats::fireRateTicks),
        FIRE_MODE.fieldOf("fire_mode").forGetter(Stats::fireMode),
        Codec.INT.fieldOf("magazine_size").forGetter(Stats::magazineSize),
        Codec.INT.fieldOf("reload_ticks").forGetter(Stats::reloadTicks),
        Codec.DOUBLE.fieldOf("recoil_vertical").forGetter(Stats::recoilVertical),
        Codec.DOUBLE.optionalFieldOf("recoil_horizontal", 0.0).forGetter(Stats::recoilHorizontal),
        Codec.INT.fieldOf("durability").forGetter(Stats::durability),
        Codec.INT.optionalFieldOf("pellets", 1).forGetter(Stats::pellets),
        Codec.DOUBLE.optionalFieldOf("zoom", 1.0).forGetter(Stats::zoom)
    ).apply(b, Stats::new));

    private StatsCodec() {
    }
}
