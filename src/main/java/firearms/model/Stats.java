package firearms.model;

/**
 * A weapon's full stat set, either a base weapon's own numbers (`docs/spec/domains/weapon.md` §3)
 * or the result of folding a {@link Loadout}'s present attachments over them
 * ({@link StatDerivation}). Every field but {@code spreadWhileAiming} and {@code zoom} is a direct
 * column of the roster table.
 *
 * @param damage half-hearts per hit (`WEAPON-REQ-002`); for the shotgun this is per-pellet damage,
 *     {@code pellets} names how many
 * @param muzzleVelocity blocks/tick
 * @param spread the hip-fire cone half-angle in degrees
 * @param spreadWhileAiming the aim-down-sights cone half-angle in degrees; the roster table has no
 *     separate column for this, so a bare weapon's starting value is defined equal to {@code
 *     spread} — this sheet's reading of `WEAPON-REQ-005`'s "aiming narrows spread by no amount
 *     beyond the weapon's own hip-fire value" absent any optic. Only an attached optic's own
 *     modifier narrows it further; no other attachment targets this field.
 * @param fireRateTicks the minimum ticks between shots, the {@code ItemCooldowns} duration
 * @param fireMode semi, auto or pump
 * @param magazineSize rounds per full magazine
 * @param reloadTicks the reload cooldown's duration
 * @param recoilVertical the cosmetic vertical camera kick in degrees per shot
 * @param recoilHorizontal the cosmetic horizontal camera kick in degrees per shot; the spec's
 *     roster and attachment tables name only a single, vertical "Recoil (°)" column and no
 *     attachment modifies a horizontal component — proposed at FA-2, retune at the sweep, held at
 *     {@code 0.0} for every 1.0 base and touched by no 1.0 modifier
 * @param durability shots before the weapon breaks
 * @param pellets bullets spawned per trigger pull; {@code 1} for every class but the shotgun, which
 *     spawns {@code 8} (`WEAPON-REQ-002` roster note)
 * @param zoom the scope magnification `domains/combat.md`'s scope mechanism reads, {@code 1.0}
 *     meaning "no magnification" (`domains/attach.md` §3's "none (1.0)"); a bare weapon or a weapon
 *     with no optic attached is always {@code 1.0}
 */
public record Stats(
    double damage,
    double muzzleVelocity,
    double spread,
    double spreadWhileAiming,
    int fireRateTicks,
    FireMode fireMode,
    int magazineSize,
    int reloadTicks,
    double recoilVertical,
    double recoilHorizontal,
    int durability,
    int pellets,
    double zoom
) {
}
