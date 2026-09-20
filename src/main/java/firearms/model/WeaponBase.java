package firearms.model;

/**
 * A base weapon: its id, class, calibre and bare {@link Stats}, with no attachment folded in yet.
 * The six 1.0 constants below are `docs/spec/domains/weapon.md` §3's roster table, literally
 * (`WEAPON-REQ-002`) — proposed starting numbers, not balanced, per that section's own note; this
 * ticket ships them as-is and defers retuning to the balance sweep (`FA-14`). A datapack's own
 * weapon data files are `FA-3`'s territory, not this record's.
 *
 * @param id the weapon's own identifier, matching its real-world designation
 * @param weaponClass the class fixing this weapon's slot set (`WeaponClass`; field named to avoid
 *     the {@code class} keyword)
 * @param caliber the calibre this weapon fires
 * @param baseStats the bare stats before any attachment is folded in
 */
public record WeaponBase(String id, WeaponClass weaponClass, Caliber caliber, Stats baseStats) {

    /** M1911 — Pistol, `.45 ACP` (`docs/spec/domains/weapon.md` §3 roster row 1). */
    public static final WeaponBase M1911 = new WeaponBase(
        "m1911", WeaponClass.PISTOL, Caliber.ACP_45,
        new Stats(3, 4.0, 3.0, 3.0, 6, FireMode.SEMI, 7, 30, 1.5, 0.0, 250, 1, 1.0)
    );

    /** Micro Uzi — SMG, `9mm` (roster row 2). */
    public static final WeaponBase MICRO_UZI = new WeaponBase(
        "micro_uzi", WeaponClass.SMG, Caliber.MM_9,
        new Stats(2, 4.0, 4.5, 4.5, 2, FireMode.AUTO, 32, 40, 1.0, 0.0, 300, 1, 1.0)
    );

    /** AKM — Assault rifle, `7.62mm` (roster row 3). */
    public static final WeaponBase AKM = new WeaponBase(
        "akm", WeaponClass.ASSAULT_RIFLE, Caliber.MM_7_62,
        new Stats(4, 6.0, 3.5, 3.5, 4, FireMode.AUTO, 30, 50, 2.5, 0.0, 400, 1, 1.0)
    );

    /** Ruger Mini-14 — DMR, `5.56mm` (roster row 4). */
    public static final WeaponBase RUGER_MINI_14 = new WeaponBase(
        "ruger_mini_14", WeaponClass.DMR, Caliber.MM_5_56,
        new Stats(5, 7.0, 2.0, 2.0, 8, FireMode.SEMI, 20, 45, 2.0, 0.0, 350, 1, 1.0)
    );

    /**
     * AWM — Sniper rifle, `.300 Magnum` (roster row 5). Fire mode is {@code SEMI}: the roster
     * table's "semi (bolt-cycle)" resolves to plain {@link FireMode#SEMI} per {@code
     * FireMode}'s own Javadoc — the bolt-cycle feel is fully carried by the 30-tick fire rate.
     */
    public static final WeaponBase AWM = new WeaponBase(
        "awm", WeaponClass.SNIPER_RIFLE, Caliber.MAGNUM_300,
        new Stats(12, 10.0, 0.5, 0.5, 30, FireMode.SEMI, 5, 60, 4.0, 0.0, 300, 1, 1.0)
    );

    /**
     * Winchester Model 1897 — Shotgun, `12 gauge` (roster row 6). {@code damage} and {@code spread}
     * are per pellet, per the roster table's own "2 ×8 pellets" / "8.0 (per pellet)" notation;
     * {@code pellets = 8}.
     */
    public static final WeaponBase WINCHESTER_MODEL_1897 = new WeaponBase(
        "winchester_model_1897", WeaponClass.SHOTGUN, Caliber.GAUGE_12,
        new Stats(2, 4.0, 8.0, 8.0, 15, FireMode.PUMP, 6, 55, 3.5, 0.0, 200, 8, 1.0)
    );
}
