package firearms.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** `COMBAT-REQ-001`'s roll, exercised with fixed inputs — no Minecraft, no game boot. */
final class SpreadMathTest {
    private static final double EPSILON = 1e-9;

    @Test
    void zeroSpreadReturnsTheNormalizedAxisUnchanged() {
        double[] result = SpreadMath.applySpread(0, 0, 5, 0, 0.7, 0.3);
        assertEquals(0, result[0], EPSILON);
        assertEquals(0, result[1], EPSILON);
        assertEquals(1, result[2], EPSILON);
    }

    @Test
    void zeroRadiusSampleReturnsTheNormalizedAxisUnchangedRegardlessOfSpread() {
        double[] result = SpreadMath.applySpread(1, 0, 0, 8.0, 0, 0.9);
        assertEquals(1, result[0], EPSILON);
        assertEquals(0, result[1], EPSILON);
        assertEquals(0, result[2], EPSILON);
    }

    @Test
    void theResultIsAlwaysNormalized() {
        double[] result = SpreadMath.applySpread(1, 2, 3, 4.5, 0.6, 0.2);
        double len = Math.sqrt(result[0] * result[0] + result[1] * result[1] + result[2] * result[2]);
        assertEquals(1, len, EPSILON);
    }

    @Test
    void fullRadiusSamplePlacesTheResultExactlyAtTheConesEdge() {
        double[] axis = {0, 0, 1};
        double[] result = SpreadMath.applySpread(axis[0], axis[1], axis[2], 8.0, 1.0, 0.4);
        double cosAngle = axis[0] * result[0] + axis[1] * result[1] + axis[2] * result[2];
        double angleDegrees = Math.toDegrees(Math.acos(Math.min(1.0, Math.max(-1.0, cosAngle))));
        assertEquals(8.0, angleDegrees, 1e-6);
    }

    @Test
    void theOffsetAngleNeverExceedsTheSpreadHalfAngle() {
        double[] axis = {0, 1, 0};
        for (double radius = 0.0; radius <= 1.0; radius += 0.1) {
            for (double azimuth = 0.0; azimuth < 1.0; azimuth += 0.25) {
                double[] result = SpreadMath.applySpread(axis[0], axis[1], axis[2], 3.5, radius, azimuth);
                double cosAngle = axis[0] * result[0] + axis[1] * result[1] + axis[2] * result[2];
                double angleDegrees = Math.toDegrees(Math.acos(Math.min(1.0, Math.max(-1.0, cosAngle))));
                assertTrue(angleDegrees <= 3.5 + 1e-6, "angle " + angleDegrees + " exceeds the 3.5 degree cone");
            }
        }
    }

    @Test
    void aNonNormalizedAxisIsNormalizedBeforeRotation() {
        double[] result = SpreadMath.applySpread(0, 0, 5, 0, 0, 0);
        double len = Math.sqrt(result[0] * result[0] + result[1] * result[1] + result[2] * result[2]);
        assertEquals(1, len, EPSILON);
    }

    @Test
    void aZeroAxisIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> SpreadMath.applySpread(0, 0, 0, 5, 0.5, 0.5));
    }
}
