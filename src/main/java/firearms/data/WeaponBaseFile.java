package firearms.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import firearms.model.Caliber;
import firearms.model.Stats;
import firearms.model.WeaponBase;
import firearms.model.WeaponClass;

/**
 * A {@code data/firearms/weapon/*.json} file's own fields — {@code class}, {@code caliber} and
 * {@code base_stats} — without the weapon's own id, which is its file's name rather than a field of
 * its own (`docs/spec/domains/weapon.md` §3, `WEAPON-DEC-003`). {@link WeaponDataLoader#apply}
 * combines one of these with its file id into the real {@link WeaponBase} the rest of this mod
 * reads.
 *
 * @param weaponClass the class fixing this weapon's slot set
 * @param caliber the calibre this weapon fires
 * @param baseStats the bare stats before any attachment is folded in
 */
record WeaponBaseFile(WeaponClass weaponClass, Caliber caliber, Stats baseStats) {
    static final Codec<WeaponClass> WEAPON_CLASS = EnumCodec.of(WeaponClass.class);
    static final Codec<Caliber> CALIBER = EnumCodec.of(Caliber.class);

    static final Codec<WeaponBaseFile> CODEC = RecordCodecBuilder.create(b -> b.group(
        WEAPON_CLASS.fieldOf("class").forGetter(WeaponBaseFile::weaponClass),
        CALIBER.fieldOf("caliber").forGetter(WeaponBaseFile::caliber),
        StatsCodec.CODEC.fieldOf("base_stats").forGetter(WeaponBaseFile::baseStats)
    ).apply(b, WeaponBaseFile::new));

    /** This file's data plus its own id (the file's name), as the rest of this mod reads it. */
    WeaponBase toWeaponBase(String id) {
        return new WeaponBase(id, weaponClass, caliber, baseStats);
    }
}
