package firearms.client.fire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** `RecoilKick`'s own pure recovery maths (`COMBAT-REQ-011`, `COMBAT-DEC-003`) — no Minecraft, no game boot. */
final class RecoilKickTest {
    private static final float EPSILON = 1e-4f;

    @Test
    void noneCarriesNothingOutstandingAndIsAlreadyDone() {
        assertTrue(RecoilKick.NONE.done());
        assertEquals(0f, RecoilKick.NONE.verticalRemaining(), EPSILON);
        assertEquals(0f, RecoilKick.NONE.horizontalRemaining(), EPSILON);
    }

    @Test
    void tickingADoneKickStaysAtNone() {
        assertSame(RecoilKick.NONE, RecoilKick.NONE.tick());
    }

    @Test
    void aFreshKickTakesExactlyRecoveryTicksTicksToFullyRecover() {
        RecoilKick kick = RecoilKick.NONE.apply(2.0f, 1.0f);
        int ticks = 0;
        while (!kick.done()) {
            kick = kick.tick();
            ticks++;
            assertTrue(ticks <= RecoilKick.RECOVERY_TICKS, "recovered in more than RECOVERY_TICKS ticks");
        }
        assertEquals(RecoilKick.RECOVERY_TICKS, ticks);
        assertEquals(0f, kick.verticalRemaining(), EPSILON);
        assertEquals(0f, kick.horizontalRemaining(), EPSILON);
    }

    @Test
    void theSumOfEveryTicksOwnRecoveryEqualsTheOriginalKick() {
        RecoilKick current = RecoilKick.NONE.apply(6.0f, 3.0f);
        float totalVertical = 0f;
        float totalHorizontal = 0f;
        while (!current.done()) {
            RecoilKick next = current.tick();
            totalVertical += current.verticalRemaining() - next.verticalRemaining();
            totalHorizontal += current.horizontalRemaining() - next.horizontalRemaining();
            current = next;
        }
        assertEquals(6.0f, totalVertical, EPSILON);
        assertEquals(3.0f, totalHorizontal, EPSILON);
    }

    @Test
    void everyIntermediateTickRecoversAPositiveShareWhenSomethingIsOutstanding() {
        RecoilKick current = RecoilKick.NONE.apply(4.0f, 0f);
        while (!current.done()) {
            RecoilKick next = current.tick();
            assertTrue(next.verticalRemaining() < current.verticalRemaining(), "each tick strictly reduces what remains");
            assertTrue(next.verticalRemaining() >= 0f, "recovery never overshoots past zero");
            current = next;
        }
    }

    @Test
    void applyingMidRecoveryAddsOnTopOfWhatsStillOutstanding() {
        RecoilKick afterOneShot = RecoilKick.NONE.apply(4.0f, 0f).tick();
        float outstandingBeforeSecondShot = afterOneShot.verticalRemaining();
        assertTrue(outstandingBeforeSecondShot > 0f && outstandingBeforeSecondShot < 4.0f);

        RecoilKick afterSecondShot = afterOneShot.apply(4.0f, 0f);
        assertEquals(outstandingBeforeSecondShot + 4.0f, afterSecondShot.verticalRemaining(), EPSILON);
        assertFalse(afterSecondShot.done());
    }

    @Test
    void applyingMidRecoveryResetsTheWindowToTheFullRecoveryTicksLength() {
        RecoilKick afterOneShot = RecoilKick.NONE.apply(4.0f, 0f).tick();
        RecoilKick climbed = afterOneShot.apply(4.0f, 0f);

        int ticks = 0;
        RecoilKick current = climbed;
        while (!current.done()) {
            current = current.tick();
            ticks++;
        }
        assertEquals(RecoilKick.RECOVERY_TICKS, ticks, "the window restarts at RECOVERY_TICKS from the moment of the second apply()");
    }

    @Test
    void verticalAndHorizontalRecoverIndependently() {
        RecoilKick kick = RecoilKick.NONE.apply(5.0f, 0f).tick();
        assertTrue(kick.verticalRemaining() > 0f, "vertical still owed back");
        assertEquals(0f, kick.horizontalRemaining(), EPSILON, "horizontal was never applied, stays at zero throughout");
    }
}
