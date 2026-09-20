package firearms.gametest;

import firearms.Firearms;
import firearms.combat.BulletEntity;
import firearms.component.Ammo;
import firearms.component.Base;
import firearms.component.ComponentRegistration;
import firearms.fire.FiringLogic;
import firearms.item.ItemRegistration;
import firearms.model.Slot;
import firearms.model.WeaponBase;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * `docs/spec/domains/weapon.md` `WEAPON-REQ-005`: while the player holds the aim control, the
 * derived spread cone narrows by the attached optic's own aim-spread modifier; with no optic
 * attached, aiming narrows spread by no amount beyond the hip-fire value. `FiringLogic.isAiming`
 * (no dedicated control yet, `WEAPON-REQ-005`'s own javadoc) reads sneaking as the interim aim
 * signal — proven directly here — and `FiringLogic#fire`'s own ternary selects between
 * {@code Stats#spread()} and {@code Stats#spreadWhileAiming()} on that signal, feeding the choice
 * into the server-rolled bullet vector (`COMBAT-REQ-001`). The spread roll itself is proven, for
 * every radius and azimuth, to never exceed its own cone in `SpreadMathTest`
 * ("theOffsetAngleNeverExceedsTheSpreadHalfAngle"); that bound is deterministic and reused here.
 * What is not otherwise proven anywhere is that firing actually *selects* the narrower value while
 * aiming and the wider one while not — proven here by firing an M1911 with a red dot optic
 * (spread 3.0°, spread-while-aiming 3.0° x 0.85 = 2.55°, `docs/spec/domains/attach.md` §3) many
 * times in each state: every aiming shot's actual bullet vector must fall within the tighter
 * 2.55° bound (a hard, deterministic consequence of `SpreadMathTest`'s own proof once the correct
 * value is fed in), and at least one hip-fire shot, among enough trials, must exceed that same
 * 2.55° bound — which could only happen if hip-fire is using the wider 3.0° cone, not the narrower
 * one. The sample count (60 hip-fire trials) keeps the chance of that second check flaking under
 * correct code below one in ten thousand.
 */
public final class AimSpreadSelectionGameTest {
    private static final int HIP_FIRE_TRIALS = 60;
    private static final int AIMING_TRIALS = 20;
    private static final double SPREAD_WHILE_AIMING_DEGREES = 3.0 * 0.85; // AKM/M1911 spread x red dot's 0.85 (attach.md §3)
    private static final double ANGLE_EPSILON_DEGREES = 0.01;

    @GameTest
    public void isAimingReadsTheSneakKeyAsTheInterimAimSignal(GameTestHelper helper) {
        ServerPlayer player = mockShooter(helper);

        player.setShiftKeyDown(false);
        helper.assertFalse(FiringLogic.isAiming(player), "not sneaking must not read as aiming (WEAPON-REQ-005)");

        player.setShiftKeyDown(true);
        helper.assertTrue(FiringLogic.isAiming(player), "sneaking must read as aiming (WEAPON-REQ-005)");

        helper.succeed();
    }

    @GameTest(structure = "firearms_gametest:open_range")
    public void hipFireUsesTheWiderConeAndAimingUsesTheNarrowerOne(GameTestHelper helper) {
        ServerPlayer player = mockShooter(helper);
        ItemStack stack = weaponStack();
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        player.setShiftKeyDown(false);
        boolean anyHipShotExceededTheAimingCone = false;
        for (int i = 0; i < HIP_FIRE_TRIALS; i++) {
            double angle = fireOnceAndMeasureAngle(helper, player, stack);
            if (angle > SPREAD_WHILE_AIMING_DEGREES + ANGLE_EPSILON_DEGREES) {
                anyHipShotExceededTheAimingCone = true;
                break;
            }
        }
        helper.assertTrue(anyHipShotExceededTheAimingCone,
            "WEAPON-REQ-005: hip-fire must use the wider 3.0 degree cone, not the aiming-narrowed 2.55 degree one — "
                + HIP_FIRE_TRIALS + " hip-fire shots never once exceeded the aiming cone");

        player.setShiftKeyDown(true);
        for (int i = 0; i < AIMING_TRIALS; i++) {
            double angle = fireOnceAndMeasureAngle(helper, player, stack);
            helper.assertTrue(angle <= SPREAD_WHILE_AIMING_DEGREES + ANGLE_EPSILON_DEGREES,
                "WEAPON-REQ-005: an aiming shot's angle " + angle + " must never exceed the red dot's own 2.55 degree aim cone");
        }

        helper.succeed();
    }

    /**
     * Fires one shot, captures the one spawned bullet's actual angle off the player's own look
     * vector at that exact moment (read fresh, immediately after firing rather than cached
     * beforehand — {@link FiringLogic#fire} reads {@code player.getLookAngle()} itself at the same
     * point in the same tick, so a fresh read here can never disagree with what it used), discards
     * the bullet, and clears the cooldown for the next shot.
     */
    private static double fireOnceAndMeasureAngle(GameTestHelper helper, ServerPlayer player, ItemStack stack) {
        Vec3 look = player.getLookAngle();
        FiringLogic.Outcome outcome = FiringLogic.attempt(helper.getLevel(), player, stack, InteractionHand.MAIN_HAND);
        helper.assertTrue(outcome == FiringLogic.Outcome.FIRED, "set up: every trial shot must fire, got " + outcome);

        List<BulletEntity> bullets = helper.getLevel().getEntitiesOfClass(BulletEntity.class,
            new AABB(helper.absoluteVec(new Vec3(-64, -64, -64)), helper.absoluteVec(new Vec3(64, 64, 64))));
        helper.assertTrue(bullets.size() == 1, "set up: exactly one bullet must be in flight per trial, found " + bullets.size());
        BulletEntity bullet = bullets.get(0);

        Vec3 velocity = bullet.getDeltaMovement().normalize();
        double cosAngle = look.x * velocity.x + look.y * velocity.y + look.z * velocity.z;
        double angleDegrees = Math.toDegrees(Math.acos(Math.min(1.0, Math.max(-1.0, cosAngle))));
        bullet.discard();

        // WEAPON-REQ-009: clear the M1911's own 6-tick fire-rate cooldown before the next trial.
        int fireRateTicks = WeaponBase.M1911.baseStats().fireRateTicks();
        for (int tick = 0; tick < fireRateTicks; tick++) {
            player.getCooldowns().tick();
        }
        return angleDegrees;
    }

    private static ServerPlayer mockShooter(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setPos(helper.absoluteVec(new Vec3(0.5, 2.5, 0.5)));
        player.getAbilities().instabuild = false;
        return player;
    }

    private static ItemStack weaponStack() {
        ItemStack stack = new ItemStack(ItemRegistration.WEAPON);
        stack.set(ComponentRegistration.BASE, Base.of(Firearms.id("m1911")));
        stack.set(ComponentRegistration.attachmentComponent(Slot.OPTIC), Firearms.id("red_dot"));
        stack.set(DataComponents.MAX_DAMAGE, WeaponBase.M1911.baseStats().durability());
        stack.set(DataComponents.DAMAGE, 0);
        // Plenty of rounds for every trial (60 + 20), set directly rather than reloaded (WEAPON-REQ-010 is FiringGameTest's own territory).
        stack.set(ComponentRegistration.AMMO, new Ammo(Firearms.id("acp_45"), 200));
        return stack;
    }
}
