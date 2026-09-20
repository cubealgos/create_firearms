package firearms.model;

/**
 * The three fire modes the 1.0 roster uses (`docs/spec/domains/weapon.md` §3, {@code
 * WEAPON-REQ-004}). The spec's open question on whether the AWM's "semi (bolt-cycle)" needs a
 * distinct fourth value is resolved here per that same section's own proposal: it reuses {@link
 * #SEMI}, the observable difference being fully captured by a longer {@code fireRateTicks} already
 * in the roster table — no separate bolt-action value exists.
 */
public enum FireMode {
    /** One shot per press; the next is blocked until the fire-rate cooldown clears. */
    SEMI,
    /** Repeats at the fire-rate interval while the control is held and ammo remains. */
    AUTO,
    /** {@code semi} for cooldown purposes; the shotgun's own multi-pellet spawn is `combat`'s concern. */
    PUMP
}
