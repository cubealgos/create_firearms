package firearms.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

/**
 * Each of the six 1.0 bare weapons (no attachment present) derives to exactly its own roster row
 * (`docs/spec/domains/weapon.md` §3, `WEAPON-REQ-002`), and never trips the clamp-and-log path
 * (`docs/spec/contracts/public-surface.md` `SURFACE-REQ-002`) since every roster number is already
 * in range.
 */
final class WeaponBaseStatsTest {
    private static final Consumer<String> NO_CLAMP = message -> {
        throw new AssertionError("a bare 1.0 weapon should never clamp, but: " + message);
    };

    private static Stats bare(WeaponBase base) {
        return StatDerivation.stats(Loadout.bare(base), NO_CLAMP);
    }

    @Test
    void m1911BareMatchesTheRosterRow() {
        Stats stats = bare(WeaponBase.M1911);
        assertEquals(new Stats(3, 4.0, 3.0, 3.0, 6, FireMode.SEMI, 7, 30, 1.5, 0.0, 250, 1, 1.0), stats);
    }

    @Test
    void microUziBareMatchesTheRosterRow() {
        Stats stats = bare(WeaponBase.MICRO_UZI);
        assertEquals(new Stats(2, 4.0, 4.5, 4.5, 2, FireMode.AUTO, 32, 40, 1.0, 0.0, 300, 1, 1.0), stats);
    }

    @Test
    void akmBareMatchesTheRosterRow() {
        Stats stats = bare(WeaponBase.AKM);
        assertEquals(new Stats(4, 6.0, 3.5, 3.5, 4, FireMode.AUTO, 30, 50, 2.5, 0.0, 400, 1, 1.0), stats);
    }

    @Test
    void rugerMini14BareMatchesTheRosterRow() {
        Stats stats = bare(WeaponBase.RUGER_MINI_14);
        assertEquals(new Stats(5, 7.0, 2.0, 2.0, 8, FireMode.SEMI, 20, 45, 2.0, 0.0, 350, 1, 1.0), stats);
    }

    @Test
    void awmBareMatchesTheRosterRowAndReusesSemiForBoltCycle() {
        Stats stats = bare(WeaponBase.AWM);
        assertEquals(new Stats(12, 10.0, 0.5, 0.5, 30, FireMode.SEMI, 5, 60, 4.0, 0.0, 300, 1, 1.0), stats);
        assertTrue(stats.fireMode() == FireMode.SEMI, "the roster's 'semi (bolt-cycle)' resolves to plain SEMI");
    }

    @Test
    void winchesterModel1897BareMatchesTheRosterRowIncludingEightPellets() {
        Stats stats = bare(WeaponBase.WINCHESTER_MODEL_1897);
        assertEquals(new Stats(2, 4.0, 8.0, 8.0, 15, FireMode.PUMP, 6, 55, 3.5, 0.0, 200, 8, 1.0), stats);
    }
}
