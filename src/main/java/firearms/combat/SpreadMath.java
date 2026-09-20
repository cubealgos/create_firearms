package firearms.combat;

/**
 * The spread-cone maths a fired shot's initial vector is rolled through (`COMBAT-REQ-001`): a
 * fixed axis direction is rotated by a random angle within a cone half-angle, given two
 * independent uniform samples in {@code [0, 1)} the caller draws from the server's own random
 * source. Deliberately plain doubles with no Minecraft import — {@code firearms.model} is FA-2's
 * own package (`docs/spec/04-architecture.md` `ARCH-DEC-001`), so this stays in {@code
 * firearms.combat} instead, unit-tested directly per the ticket's fallback instruction.
 */
public final class SpreadMath {
    private SpreadMath() {
    }

    /**
     * Rotates the normalized {@code (dx, dy, dz)} axis by a random offset angle in
     * {@code [0, spreadDegrees]} of arc from that axis.
     *
     * <p>{@code radiusSample} picks the offset angle via a square-root distribution, so results
     * are uniform over the cone's solid angle rather than bunched near the axis; {@code
     * azimuthSample} picks the direction around the axis. Both are expected uniform in
     * {@code [0, 1)}; {@code radiusSample == 0} (or {@code spreadDegrees <= 0}) returns the
     * un-rotated, normalized axis exactly.
     *
     * @return a normalized {@code {x, y, z}} array
     * @throws IllegalArgumentException if the axis is the zero vector
     */
    public static double[] applySpread(
            double dx, double dy, double dz,
            double spreadDegrees, double radiusSample, double azimuthSample) {
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len == 0) {
            throw new IllegalArgumentException("the spread axis must be non-zero");
        }
        double ax = dx / len;
        double ay = dy / len;
        double az = dz / len;
        if (spreadDegrees <= 0 || radiusSample <= 0) {
            return new double[] {ax, ay, az};
        }

        // An arbitrary vector not parallel to the axis, to build a perpendicular basis (u, v).
        double[] arbitrary = Math.abs(ay) < 0.999 ? new double[] {0, 1, 0} : new double[] {1, 0, 0};
        double[] u = normalize(cross(ax, ay, az, arbitrary[0], arbitrary[1], arbitrary[2]));
        // axis and u are already orthonormal, so their cross product v is unit length too.
        double[] v = cross(ax, ay, az, u[0], u[1], u[2]);

        double offsetRad = Math.toRadians(spreadDegrees) * Math.sqrt(radiusSample);
        double azimuthRad = 2 * Math.PI * azimuthSample;
        double sinOffset = Math.sin(offsetRad);
        double cosOffset = Math.cos(offsetRad);
        double cosAz = Math.cos(azimuthRad);
        double sinAz = Math.sin(azimuthRad);

        return new double[] {
            ax * cosOffset + (u[0] * cosAz + v[0] * sinAz) * sinOffset,
            ay * cosOffset + (u[1] * cosAz + v[1] * sinAz) * sinOffset,
            az * cosOffset + (u[2] * cosAz + v[2] * sinAz) * sinOffset
        };
    }

    private static double[] cross(double ax, double ay, double az, double bx, double by, double bz) {
        return new double[] {ay * bz - az * by, az * bx - ax * bz, ax * by - ay * bx};
    }

    private static double[] normalize(double[] v) {
        double len = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return new double[] {v[0] / len, v[1] / len, v[2] / len};
    }
}
