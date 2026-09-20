package firearms.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The pure function deriving a weapon's final stats from its base plus every present attachment
 * (`docs/spec/domains/weapon.md` {@code WEAPON-REQ-003}): folds each present attachment's own
 * modifiers onto the base stats in the fixed slot order {@link Slot} declares — muzzle, optic,
 * magazine, grip, stock — for determinism. Re-run at every point stats are needed and never written
 * back to the item; this class holds no state of its own. Also the out-of-range clamp-and-log path
 * a datapack's own data can trigger (`docs/spec/contracts/public-surface.md` {@code
 * SURFACE-REQ-002}).
 */
public final class StatDerivation {
    private StatDerivation() {
    }

    /**
     * Derives {@code loadout}'s final stats, folding every present attachment's modifiers over the
     * base weapon's own stats in fixed slot order, then clamping the result into range.
     *
     * @param loadout the base weapon plus whichever slots are currently filled
     * @param onClamped called once per field the fold pushed out of range (`SURFACE-REQ-002`);
     *     never called when every field lands in range
     * @return the final, clamped stats
     */
    public static Stats stats(Loadout loadout, Consumer<String> onClamped) {
        Stats base = loadout.base().baseStats();
        Map<Stat, Double> running = new EnumMap<>(Stat.class);
        running.put(Stat.DAMAGE, base.damage());
        running.put(Stat.MUZZLE_VELOCITY, base.muzzleVelocity());
        running.put(Stat.SPREAD, base.spread());
        running.put(Stat.SPREAD_WHILE_AIMING, base.spreadWhileAiming());
        running.put(Stat.FIRE_RATE_TICKS, (double) base.fireRateTicks());
        running.put(Stat.MAGAZINE_SIZE, (double) base.magazineSize());
        running.put(Stat.RELOAD_TICKS, (double) base.reloadTicks());
        running.put(Stat.RECOIL_VERTICAL, base.recoilVertical());
        running.put(Stat.RECOIL_HORIZONTAL, base.recoilHorizontal());
        double zoom = base.zoom();

        for (Slot slot : Slot.values()) {
            Attachment attachment = loadout.attachment(slot).orElse(null);
            if (attachment == null) {
                continue;
            }
            for (Modifier modifier : attachment.modifiers()) {
                running.put(modifier.stat(), modifier.apply(running.get(modifier.stat())));
            }
            if (slot == Slot.OPTIC) {
                zoom = attachment.zoom();
            }
        }

        double damage = Clamp.atLeast("damage", running.get(Stat.DAMAGE), 0.0, onClamped);
        double spread = Clamp.atLeast("spread", running.get(Stat.SPREAD), 0.0, onClamped);
        double spreadWhileAiming = Clamp.atLeast("spreadWhileAiming", running.get(Stat.SPREAD_WHILE_AIMING), 0.0, onClamped);
        // Floored, not rounded to nearest: the spec is silent on sub-integer rounding for these
        // three tick/round-count fields (proposed at FA-2, retune at the sweep), and flooring
        // guarantees every strictly-less-than-1 multiplier registers as at least one fewer tick —
        // matching e.g. attach.md's own "fire rate ×0.95 (faster)" wording, which Math.round's
        // nearest-integer behaviour would silently swallow on the roster's smallest fire rates.
        int fireRateTicks = Clamp.atLeast("fireRateTicks", (int) Math.floor(running.get(Stat.FIRE_RATE_TICKS)), 1, onClamped);
        int magazineSize = Clamp.atLeast("magazineSize", (int) Math.floor(running.get(Stat.MAGAZINE_SIZE)), 1, onClamped);
        int reloadTicks = Clamp.atLeast("reloadTicks", (int) Math.floor(running.get(Stat.RELOAD_TICKS)), 0, onClamped);

        return new Stats(
            damage,
            running.get(Stat.MUZZLE_VELOCITY),
            spread,
            spreadWhileAiming,
            fireRateTicks,
            base.fireMode(),
            magazineSize,
            reloadTicks,
            running.get(Stat.RECOIL_VERTICAL),
            running.get(Stat.RECOIL_HORIZONTAL),
            base.durability(),
            base.pellets(),
            zoom
        );
    }
}
