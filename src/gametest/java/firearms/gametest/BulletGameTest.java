package firearms.gametest;

import firearms.combat.BulletEntity;
import firearms.combat.BulletSpawner;
import firearms.combat.CombatRegistration;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * The bullet entity's flight, hit resolution, block discard, despawn and pellet spawn
 * (`docs/spec/domains/combat.md`; `docs/spec/operations/testing.md` `TEST-REQ-004`).
 */
public final class BulletGameTest {
    private static final float TEST_DAMAGE = 5.0f;

    /**
     * {@code firearms_gametest:open_range} is a 4x30x60 all-air structure this mod ships
     * ({@code src/gametest/resources/data/firearms_gametest/gametest/structure/open_range.snbt}):
     * the default {@code fabric-gametest-api-v1:empty} structure is only 8x8x8, and its invisible
     * barrier walls sit exactly at that boundary — {@code padding} widens the region {@link
     * GameTestHelper#getBoundsWithPadding()} reports, but not where the barriers themselves are
     * placed (`TestInstanceBlockEntity.processStructureBoundary` builds from the raw, unpadded
     * structure bounds), so a bullet travelling any real distance needs a genuinely larger
     * structure, not a larger padding value.
     */
    @GameTest(structure = "firearms_gametest:open_range")
    public void aBulletHitsATargetTwentyBlocksAwayAndDealsTheGivenDamage(GameTestHelper helper) {
        Player shooter = helper.makeMockServerPlayer(GameType.SURVIVAL);
        Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, new Vec3(0.5, 2.0, 20.5));
        zombie.setNoAi(true);
        float healthBefore = zombie.getHealth();

        BulletSpawner.spawnBullet(
            helper.getLevel(), shooter, helper.absoluteVec(new Vec3(0.5, 2.5, 0.5)),
            new Vec3(0, 0, 1), 4.0, 0.0, TEST_DAMAGE);

        helper.succeedWhen(() -> {
            helper.assertTrue(zombie.getHealth() < healthBefore, "the zombie took damage");
            helper.assertTrue(
                zombie.getLastDamageSource() != null
                    && zombie.getLastDamageSource().is(CombatRegistration.BULLET_DAMAGE_TYPE),
                "the zombie's last damage source resolves to firearms:bullet (COMBAT-REQ-004)");
        });
    }

    /** `TEST-REQ-004`: comfortably beyond every 1.0 weapon's muzzle-velocity/despawn-life range. */
    @GameTest(maxTicks = 60, structure = "firearms_gametest:open_range")
    public void aBulletHitsATargetBeyondFortyBlocks(GameTestHelper helper) {
        Player shooter = helper.makeMockServerPlayer(GameType.SURVIVAL);
        Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, new Vec3(0.5, 2.0, 45.5));
        zombie.setNoAi(true);
        float healthBefore = zombie.getHealth();

        // The AWM's own 10.0 blocks/tick (docs/spec/domains/weapon.md roster table).
        BulletSpawner.spawnBullet(
            helper.getLevel(), shooter, helper.absoluteVec(new Vec3(0.5, 2.5, 0.5)),
            new Vec3(0, 0, 1), 10.0, 0.0, TEST_DAMAGE);

        helper.succeedWhen(() -> helper.assertTrue(zombie.getHealth() < healthBefore, "a hit registers past 40 blocks"));
    }

    /**
     * `COMBAT-REQ-002`: the swept-segment hit test covers the whole tick's movement, so an
     * unrealistically high velocity that crosses the target well within a single tick still
     * registers — a fixed-length or endpoint-only probe would miss this.
     */
    @GameTest(structure = "firearms_gametest:open_range")
    public void aVeryHighVelocityBulletDoesNotTunnelThroughATarget(GameTestHelper helper) {
        Player shooter = helper.makeMockServerPlayer(GameType.SURVIVAL);
        Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, new Vec3(0.5, 2.0, 12.5));
        zombie.setNoAi(true);
        float healthBefore = zombie.getHealth();

        BulletSpawner.spawnBullet(
            helper.getLevel(), shooter, helper.absoluteVec(new Vec3(0.5, 2.5, 0.5)),
            new Vec3(0, 0, 1), 300.0, 0.0, TEST_DAMAGE);

        helper.succeedWhen(() -> helper.assertTrue(zombie.getHealth() < healthBefore, "no tunneling at 300 blocks/tick"));
    }

    /** `COMBAT-FAIL-002`: removed on the block hit, no penetration. */
    @GameTest
    public void aBulletFiredAtAWallDiscardsWithNoPenetration(GameTestHelper helper) {
        Player shooter = helper.makeMockServerPlayer(GameType.SURVIVAL);
        helper.setBlock(new BlockPos(0, 2, 5), Blocks.STONE);

        BulletEntity bullet = BulletSpawner.spawnBullet(
            helper.getLevel(), shooter, helper.absoluteVec(new Vec3(0.5, 2.5, 0.5)),
            new Vec3(0, 0, 1), 4.0, 0.0, TEST_DAMAGE);

        helper.runAfterDelay(10, () -> {
            helper.assertTrue(bullet.isRemoved(), "the bullet is discarded on the block hit");
            helper.succeed();
        });
    }

    /**
     * `COMBAT-REQ-003`, `UC-013`: no damage, no drop, just a clean removal after the fixed life.
     * A slow, straight-up shot: gravity never pulls it back down to the floor within the life
     * span (velocity stays positive throughout, since 1.0 &gt; `GRAVITY_PER_TICK` * 40 ticks), so
     * the only thing that can end its flight is the life timer itself.
     */
    @GameTest(maxTicks = 80, structure = "firearms_gametest:open_range")
    public void aBulletWithNothingToHitDespawnsAfterItsLifeWithNoDamageOrDrop(GameTestHelper helper) {
        Player shooter = helper.makeMockServerPlayer(GameType.SURVIVAL);

        BulletEntity bullet = BulletSpawner.spawnBullet(
            helper.getLevel(), shooter, helper.absoluteVec(new Vec3(0.5, 2.5, 0.5)),
            new Vec3(0, 1, 0), 1.0, 0.0, TEST_DAMAGE);

        helper.runAfterDelay(45, () -> {
            helper.assertTrue(bullet.isRemoved(), "despawned after its fixed life (proposed 40 ticks)");
            helper.succeed();
        });
    }

    /** `COMBAT-REQ-005`: one trigger pull, eight independently-spread pellet entities. */
    @GameTest
    public void aShotgunTriggerPullSpawnsEightIndependentlySpreadPellets(GameTestHelper helper) {
        Player shooter = helper.makeMockServerPlayer(GameType.SURVIVAL);

        List<BulletEntity> pellets = BulletSpawner.spawnPellets(
            helper.getLevel(), shooter, helper.absoluteVec(new Vec3(0.5, 2.5, 0.5)),
            new Vec3(0, 0, 1), 4.0, 8.0, 2.0f, 8);

        helper.assertValueEqual(pellets.size(), 8, "eight pellets spawned");
        helper.assertTrue(pellets.stream().allMatch(BulletEntity::isPellet), "every spawned bullet is flagged as a pellet");
        helper.assertEntitiesPresent(CombatRegistration.BULLET, 8);
        helper.succeed();
    }

    /**
     * `COMBAT-FAIL-004`: an invulnerable target takes no damage — vanilla's own
     * {@code LivingEntity.hurtServer} invulnerability check runs before this mod's damage is ever
     * applied ({@code BulletEntity#onHitEntity} calls it unconditionally, with no special case of
     * its own), and the bullet is still consumed cleanly, no crash.
     */
    @GameTest(structure = "firearms_gametest:open_range")
    public void anInvulnerableTargetTakesNoDamage(GameTestHelper helper) {
        Player shooter = helper.makeMockServerPlayer(GameType.SURVIVAL);
        Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, new Vec3(0.5, 2.0, 12.5));
        zombie.setNoAi(true);
        zombie.setInvulnerable(true);
        float healthBefore = zombie.getHealth();

        BulletEntity bullet = BulletSpawner.spawnBullet(
            helper.getLevel(), shooter, helper.absoluteVec(new Vec3(0.5, 2.5, 0.5)),
            new Vec3(0, 0, 1), 4.0, 0.0, TEST_DAMAGE);

        helper.runAfterDelay(10, () -> {
            helper.assertValueEqual(zombie.getHealth(), healthBefore, "an invulnerable target must take no damage (COMBAT-FAIL-004)");
            helper.assertTrue(bullet.isRemoved(), "the bullet is still consumed cleanly on the hit, no crash");
            helper.succeed();
        });
    }
}
