package firearms.combat;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * The entry point the firing code (FA-6) calls to put a fired shot into the world: one bullet,
 * or a shotgun's independently-spread pellets (`COMBAT-REQ-005`). Spread is rolled here,
 * server-side, from the level's own {@link RandomSource} and {@link SpreadMath}'s pure
 * trigonometry — the client never supplies or influences the roll (`COMBAT-REQ-001`).
 */
public final class BulletSpawner {
    private BulletSpawner() {
    }

    /**
     * Spawns one bullet along {@code lookDirection}, spread within {@code spreadDegrees} of it.
     *
     * @param origin the muzzle position the firing code has already computed
     * @param lookDirection the shooter's aim vector before spread is applied
     * @param muzzleVelocity blocks/tick (`domains/weapon.md` §3)
     * @param spreadDegrees the derived cone half-angle in degrees
     * @param damage the weapon's derived per-hit damage
     */
    public static BulletEntity spawnBullet(
            ServerLevel level, LivingEntity shooter, Vec3 origin, Vec3 lookDirection,
            double muzzleVelocity, double spreadDegrees, float damage) {
        return spawnOne(level, shooter, origin, lookDirection, muzzleVelocity, spreadDegrees, damage, false);
    }

    /**
     * Spawns {@code pelletCount} independently-spread pellets from one trigger pull
     * (`COMBAT-REQ-005`); the caller consumes exactly one round regardless of {@code pelletCount}.
     *
     * @param spreadDegrees the shotgun's own, wider, per-pellet cone (`domains/weapon.md` roster table)
     * @param damagePerPellet the shotgun's per-pellet damage
     */
    public static List<BulletEntity> spawnPellets(
            ServerLevel level, LivingEntity shooter, Vec3 origin, Vec3 lookDirection,
            double muzzleVelocity, double spreadDegrees, float damagePerPellet, int pelletCount) {
        List<BulletEntity> pellets = new ArrayList<>(pelletCount);
        for (int i = 0; i < pelletCount; i++) {
            pellets.add(spawnOne(level, shooter, origin, lookDirection, muzzleVelocity, spreadDegrees, damagePerPellet, true));
        }
        return pellets;
    }

    private static BulletEntity spawnOne(
            ServerLevel level, LivingEntity shooter, Vec3 origin, Vec3 lookDirection,
            double muzzleVelocity, double spreadDegrees, float damage, boolean pellet) {
        RandomSource random = level.getRandom();
        double[] spread = SpreadMath.applySpread(
            lookDirection.x, lookDirection.y, lookDirection.z,
            spreadDegrees, random.nextDouble(), random.nextDouble());

        BulletEntity bullet = new BulletEntity(CombatRegistration.BULLET, level);
        bullet.setOwner(shooter);
        bullet.shootFrom(origin, new Vec3(spread[0], spread[1], spread[2]), muzzleVelocity);
        bullet.setDamage(damage);
        bullet.setPellet(pellet);
        level.addFreshEntity(bullet);
        return bullet;
    }
}
