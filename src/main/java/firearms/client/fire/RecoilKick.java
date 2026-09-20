package firearms.client.fire;

/**
 * The client-only recoil camera kick's own recovery state: a vertical and horizontal offset still
 * owed back to the player's rotation, recovering an even fraction of itself every client tick over
 * a fixed recovery window (`docs/spec/domains/combat.md` {@code COMBAT-REQ-011}, {@code
 * COMBAT-DEC-003}: "recovering over a few ticks"). Pure value math with no Minecraft import, so a
 * plain unit test exercises it directly with no game boot, the same reasoning {@code
 * firearms.combat.SpreadMath} follows for its own pure roll. {@link firearms.client.fire.RecoilHandler}
 * is the only caller, and the only place these degrees ever touch a real camera. Immutable: every
 * method returns a new instance rather than mutating this one.
 *
 * <p>{@link #RECOVERY_TICKS} is a proposed starting value, in the same "proposed, retune at the
 * sweep" spirit `docs/spec/domains/weapon.md` §3 and `docs/spec/domains/attach.md` §3 mark their
 * own numbers with — `docs/spec/domains/combat.md` asks only for "recovering over a few ticks" and
 * fixes no exact count. Not written back into the spec by this change (out of this ticket's file
 * scope); flagged in the ticket's own report for the balance sweep to confirm or retune, the same
 * open-question shape `docs/spec/domains/combat.md` §7 already uses for the bullet despawn-life
 * tick count.
 */
public final class RecoilKick {

    /** Proposed: the number of client ticks a kick takes to fully recover — see this class's own Javadoc. */
    public static final int RECOVERY_TICKS = 8;

    /** No kick outstanding: both axes at rest, nothing left to recover. */
    public static final RecoilKick NONE = new RecoilKick(0f, 0f, 0);

    private final float verticalRemaining;
    private final float horizontalRemaining;
    private final int ticksRemaining;

    private RecoilKick(float verticalRemaining, float horizontalRemaining, int ticksRemaining) {
        this.verticalRemaining = verticalRemaining;
        this.horizontalRemaining = horizontalRemaining;
        this.ticksRemaining = ticksRemaining;
    }

    /**
     * Adds a fresh kick on top of whatever this instance still owes back, and resets the recovery
     * window to its full length: a second shot arriving mid-recovery (an automatic weapon's own
     * fire rate, typically faster than {@link #RECOVERY_TICKS}) climbs the outstanding kick rather
     * than restarting it from zero — recoil climbing while firing, then settling once the trigger
     * is released, the shape a real automatic weapon's recoil pattern has.
     *
     * @param verticalDegrees the additional upward kick to add, in degrees
     * @param horizontalDegrees the additional sideways kick to add, in degrees (signed)
     * @return a new instance carrying the combined outstanding kick
     */
    public RecoilKick apply(float verticalDegrees, float horizontalDegrees) {
        return new RecoilKick(verticalRemaining + verticalDegrees, horizontalRemaining + horizontalDegrees, RECOVERY_TICKS);
    }

    /**
     * One client tick's recovery: this instance's own remaining degrees divided evenly over its
     * own remaining ticks, so whatever is outstanding at a given moment recovers in exactly {@link
     * #RECOVERY_TICKS} more ticks from that moment. The caller (only ever {@link RecoilHandler})
     * reads how much this step recovered as the difference between this instance's and the
     * result's own {@link #verticalRemaining()}/{@link #horizontalRemaining()}.
     *
     * @return the next tick's state; {@link #NONE} once nothing is left to recover
     */
    public RecoilKick tick() {
        if (done()) {
            return NONE;
        }
        float verticalStep = verticalRemaining / ticksRemaining;
        float horizontalStep = horizontalRemaining / ticksRemaining;
        return new RecoilKick(verticalRemaining - verticalStep, horizontalRemaining - horizontalStep, ticksRemaining - 1);
    }

    /** Whether every tick of this kick's own recovery window has already elapsed. */
    public boolean done() {
        return ticksRemaining <= 0;
    }

    /** The upward degrees still owed back to the player's pitch. */
    public float verticalRemaining() {
        return verticalRemaining;
    }

    /** The sideways degrees still owed back to the player's yaw (signed). */
    public float horizontalRemaining() {
        return horizontalRemaining;
    }
}
