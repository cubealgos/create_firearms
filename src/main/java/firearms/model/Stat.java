package firearms.model;

/**
 * Every field of {@link Stats} an {@link Modifier} may target (`docs/spec/domains/attach.md` §3).
 * {@code SPREAD_WHILE_AIMING} is distinct from {@code SPREAD}: the roster table's {@code spread}
 * column is the hip-fire cone, while every optic's own modifier narrows only the aim-down-sights
 * cone (`docs/spec/domains/weapon.md` {@code WEAPON-REQ-005}) — conflating the two would let an
 * optic quietly narrow hip-fire spread, which the spec explicitly says does not happen. Zoom is not
 * a {@code Stat}: `domains/attach.md` §3 states it is "a separate field, read only by
 * `domains/combat.md`'s scope mechanism, not by the stat function's own multiplication," so it is
 * carried on {@link Attachment} directly and applied by {@link StatDerivation} as a slot override,
 * never folded through a {@link Modifier}.
 */
public enum Stat {
    DAMAGE,
    MUZZLE_VELOCITY,
    SPREAD,
    SPREAD_WHILE_AIMING,
    FIRE_RATE_TICKS,
    MAGAZINE_SIZE,
    RELOAD_TICKS,
    RECOIL_VERTICAL,
    RECOIL_HORIZONTAL,
    DURABILITY,
    PELLETS
}
