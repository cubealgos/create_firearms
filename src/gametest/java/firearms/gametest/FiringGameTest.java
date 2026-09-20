package firearms.gametest;

import firearms.Firearms;
import firearms.combat.CombatRegistration;
import firearms.component.Ammo;
import firearms.component.Base;
import firearms.component.ComponentRegistration;
import firearms.fire.FireNetworking;
import firearms.fire.FireSounds;
import firearms.fire.FiringLogic;
import firearms.fire.WeaponLoadouts;
import firearms.item.ItemRegistration;
import firearms.model.Loadout;
import firearms.model.Slot;
import firearms.model.WeaponBase;
import firearms.model.WeaponClass;
import firearms.support.Ids;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * The fire-control loop (`FA-6`, `FA-24`, `docs/spec/domains/weapon.md` `WEAPON-REQ-004`,
 * `007`-`013`, `018`, `019`; `docs/spec/operations/testing.md`): a loaded weapon fires and moves
 * ammo, durability and the cooldown together; an empty weapon reloads from matching cartridges or
 * clicks empty; a held {@code auto} weapon repeats at its own fire-rate interval; a {@code pump}
 * weapon refuses a second shot inside its own delay; a shotgun spawns its full pellet count from
 * one round; a suppressor changes the sound event {@code FireSounds} selects. Since
 * `docs/spec/decisions/DEC-019-controls.md`, firing is reached through {@code
 * firearms.fire.FireNetworking#handleFire}/{@code #handleReload} — the real serverbound-payload
 * receivers a game test drives directly with a mock player, real networking being out of scope for
 * this harness — rather than {@code FiringLogic#attempt} alone; several tests below exercise that
 * path explicitly, and a dedicated test proves the aim/fire split structurally: starting the use
 * session alone never fires.
 */
public final class FiringGameTest {

    @GameTest(structure = "firearms_gametest:open_range")
    public void aLoadedWeaponFiresOnceAndMovesAmmoDurabilityAndCooldownTogether(GameTestHelper helper) {
        ServerPlayer player = mockShooter(helper);
        ItemStack stack = weaponStack(WeaponBase.M1911, 7);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        FiringLogic.Outcome outcome = FiringLogic.attempt(helper.getLevel(), player, stack, InteractionHand.MAIN_HAND);
        helper.assertTrue(outcome == FiringLogic.Outcome.FIRED, "a loaded, off-cooldown weapon must fire: got " + outcome);

        Ammo ammo = stack.get(ComponentRegistration.AMMO);
        helper.assertTrue(ammo != null && ammo.loaded() == 6, "WEAPON-REQ-008: ammo must drop by exactly one round");
        helper.assertValueEqual(stack.getDamageValue(), 1, "WEAPON-REQ-008: durability must drop by exactly one point");
        helper.assertEntitiesPresent(CombatRegistration.BULLET, 1);

        FiringLogic.Outcome second = FiringLogic.attempt(helper.getLevel(), player, stack, InteractionHand.MAIN_HAND);
        helper.assertTrue(second == FiringLogic.Outcome.ON_COOLDOWN,
            "WEAPON-REQ-009: an immediate second attempt must be blocked by the fire-rate cooldown, got " + second);

        helper.succeed();
    }

    @GameTest(structure = "firearms_gametest:open_range")
    public void anEmptyWeaponWithMatchingCartridgesReloadsToMagazineSizeAndConsumesThem(GameTestHelper helper) {
        ServerPlayer player = mockShooter(helper);
        ItemStack stack = weaponStack(WeaponBase.M1911, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.getInventory().add(new ItemStack(ItemRegistration.cartridge(WeaponBase.M1911.caliber()), 10));

        FiringLogic.Outcome outcome = FiringLogic.attempt(helper.getLevel(), player, stack, InteractionHand.MAIN_HAND);
        helper.assertTrue(outcome == FiringLogic.Outcome.RELOADED, "WEAPON-REQ-010: an empty weapon with matching cartridges must reload, got " + outcome);

        Ammo ammo = stack.get(ComponentRegistration.AMMO);
        int magazineSize = WeaponBase.M1911.baseStats().magazineSize();
        helper.assertTrue(ammo != null && ammo.loaded() == magazineSize,
            "WEAPON-REQ-010: loaded must reach the derived magazine size (" + magazineSize + "), got " + ammo);

        int remaining = countCartridges(player, WeaponBase.M1911);
        helper.assertValueEqual(remaining, 10 - magazineSize, "exactly magazineSize cartridges must be consumed from inventory");

        helper.succeed();
    }

    @GameTest(structure = "firearms_gametest:open_range")
    public void anEmptyWeaponWithNoMatchingCartridgesFiresNothingAndStaysEmpty(GameTestHelper helper) {
        ServerPlayer player = mockShooter(helper);
        ItemStack stack = weaponStack(WeaponBase.M1911, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        // No cartridges of any calibre anywhere in inventory (WEAPON-FAIL-003).

        FiringLogic.Outcome outcome = FiringLogic.attempt(helper.getLevel(), player, stack, InteractionHand.MAIN_HAND);
        helper.assertTrue(outcome == FiringLogic.Outcome.EMPTY_CLICK,
            "WEAPON-REQ-011: an empty weapon with no matching cartridge must click empty, got " + outcome);

        Ammo ammo = stack.get(ComponentRegistration.AMMO);
        helper.assertTrue(ammo == null || ammo.loaded() == 0, "ammo must stay unchanged (still empty)");
        helper.assertEntitiesPresent(CombatRegistration.BULLET, 0);

        helper.succeed();
    }

    /** `AMMO-FAIL-001`, `AMMO-REQ-004`: a wrong-calibre cartridge present is never matched, exactly as if no cartridge existed at all. */
    @GameTest(structure = "firearms_gametest:open_range")
    public void anEmptyWeaponWithOnlyWrongCalibreCartridgesFiresNothingAndLeavesThemUnconsumed(GameTestHelper helper) {
        ServerPlayer player = mockShooter(helper);
        ItemStack stack = weaponStack(WeaponBase.M1911, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        // Micro Uzi's own 9mm, never M1911's .45 ACP (AMMO-FAIL-001).
        player.getInventory().add(new ItemStack(ItemRegistration.cartridge(WeaponBase.MICRO_UZI.caliber()), 10));

        FiringLogic.Outcome outcome = FiringLogic.attempt(helper.getLevel(), player, stack, InteractionHand.MAIN_HAND);
        helper.assertTrue(outcome == FiringLogic.Outcome.EMPTY_CLICK,
            "AMMO-REQ-004: a wrong-calibre cartridge must never match a reload, got " + outcome);

        Ammo ammo = stack.get(ComponentRegistration.AMMO);
        helper.assertTrue(ammo == null || ammo.loaded() == 0, "ammo must stay unchanged (still empty)");

        int wrongCalibreRemaining = countCartridges(player, WeaponBase.MICRO_UZI);
        helper.assertValueEqual(wrongCalibreRemaining, 10, "the wrong-calibre cartridges must be left untouched, not consumed");

        helper.succeed();
    }

    /**
     * `UC-008`, `FA-24`: an {@code auto} weapon fires 3 shots over exactly {@code 3 * fireRateTicks}
     * simulated ticks (the Micro Uzi's own {@code fireRateTicks == 2}). Since
     * `docs/spec/decisions/DEC-019-controls.md`, the pacing itself is client-side
     * ({@code firearms.client.fire.FireInputHandler}'s own per-tick countdown, driven by real
     * client input polling), which this server-only harness cannot exercise directly — this test
     * instead proves the *server-side* shape of what that client loop produces: one {@code
     * FiringLogic#attempt} call per fire-rate interval, with {@code ItemCooldowns.tick()} advanced
     * {@code fireRateTicks} times between attempts (mirroring {@code
     * AimSpreadSelectionGameTest#fireOnceAndMeasureAngle}'s own cooldown-clearing convention), is
     * exactly what the server-authoritative cooldown check needs to accept 3 shots and reject
     * nothing in between.
     */
    @GameTest(structure = "firearms_gametest:open_range")
    public void anAutoWeaponFiresNBulletsOverNTimesFireRateTicksOfHeldUse(GameTestHelper helper) {
        ServerPlayer player = mockShooter(helper);
        ItemStack stack = weaponStack(WeaponBase.MICRO_UZI, WeaponBase.MICRO_UZI.baseStats().magazineSize());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        int fireRateTicks = WeaponBase.MICRO_UZI.baseStats().fireRateTicks();
        int expectedShots = 3;

        for (int shot = 0; shot < expectedShots; shot++) {
            FiringLogic.Outcome outcome = FiringLogic.attempt(helper.getLevel(), player, stack, InteractionHand.MAIN_HAND);
            helper.assertTrue(outcome == FiringLogic.Outcome.FIRED, "every attempt spaced a full fireRateTicks apart must fire, got " + outcome);
            for (int tick = 0; tick < fireRateTicks; tick++) {
                player.getCooldowns().tick();
            }
        }

        helper.assertEntitiesPresent(CombatRegistration.BULLET, expectedShots);
        Ammo ammo = stack.get(ComponentRegistration.AMMO);
        helper.assertTrue(ammo != null && ammo.loaded() == WeaponBase.MICRO_UZI.baseStats().magazineSize() - expectedShots,
            "ammo must drop by exactly one round per shot fired");

        helper.succeed();
    }

    /**
     * `FA-24`, `docs/spec/decisions/DEC-019-controls.md`: {@code FireNetworking#handleFire} — the
     * real {@code ServerboundFirePayload} receiver, not {@code FiringLogic#attempt} called directly
     * — drives the exact same fire-or-reject path {@link #aLoadedWeaponFiresOnceAndMovesAmmoDurabilityAndCooldownTogether}
     * already proves for {@code attempt} itself: a loaded, off-cooldown weapon fires exactly once,
     * and an immediate second payload lands on the same fire-rate cooldown and is rejected —
     * neither ammo, durability nor the bullet count move any further on that second call.
     */
    @GameTest(structure = "firearms_gametest:open_range")
    public void handleFireOnALoadedOffCooldownWeaponFiresOnceAndRejectsAnImmediateSecondCall(GameTestHelper helper) {
        ServerPlayer player = mockShooter(helper);
        ItemStack stack = weaponStack(WeaponBase.M1911, 7);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        FireNetworking.handleFire(player);

        Ammo ammoAfterFirst = stack.get(ComponentRegistration.AMMO);
        helper.assertTrue(ammoAfterFirst != null && ammoAfterFirst.loaded() == 6, "WEAPON-REQ-008: ammo must drop by exactly one round");
        helper.assertValueEqual(stack.getDamageValue(), 1, "WEAPON-REQ-008: durability must drop by exactly one point");
        helper.assertEntitiesPresent(CombatRegistration.BULLET, 1);

        FireNetworking.handleFire(player);

        Ammo ammoAfterSecond = stack.get(ComponentRegistration.AMMO);
        helper.assertTrue(ammoAfterSecond != null && ammoAfterSecond.loaded() == 6,
            "WEAPON-REQ-009: an immediate second handleFire call must be blocked by the fire-rate cooldown, ammo must not move again");
        helper.assertValueEqual(stack.getDamageValue(), 1, "durability must not move on a rejected second call");
        helper.assertEntitiesPresent(CombatRegistration.BULLET, 1);

        helper.succeed();
    }

    /** `FA-24`: a fire payload while the main hand holds no firearm is a no-op — no bullet, no crash. */
    @GameTest(structure = "firearms_gametest:open_range")
    public void handleFireWhileHoldingANonWeaponItemDoesNothing(GameTestHelper helper) {
        ServerPlayer player = mockShooter(helper);
        ItemStack stick = new ItemStack(Items.STICK);
        player.setItemInHand(InteractionHand.MAIN_HAND, stick);

        FireNetworking.handleFire(player);

        helper.assertEntitiesPresent(CombatRegistration.BULLET, 0);
        helper.succeed();
    }

    /**
     * `WEAPON-REQ-018`, `019`: proves the aim/fire decoupling structurally — starting the use
     * session (what {@code WeaponItem#use} does on a right click) and letting ticks pass with no
     * fire payload ever sent must never, by itself, decrement ammo or spawn a bullet. There is no
     * {@code onUseTick} any more for a held use session to dispatch through; this test is the
     * negative proof that removing it really did decouple the two.
     */
    @GameTest(structure = "firearms_gametest:open_range")
    public void startingTheUseSessionAloneNeverFiresOrConsumesAmmo(GameTestHelper helper) {
        ServerPlayer player = mockShooter(helper);
        ItemStack stack = weaponStack(WeaponBase.M1911, 7);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        ItemRegistration.WEAPON.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        for (int tick = 0; tick < 20; tick++) {
            player.getCooldowns().tick();
        }

        Ammo ammo = stack.get(ComponentRegistration.AMMO);
        helper.assertTrue(ammo != null && ammo.loaded() == 7, "WEAPON-REQ-018: starting the use session alone must never fire — ammo must be unchanged");
        helper.assertEntitiesPresent(CombatRegistration.BULLET, 0);

        helper.succeed();
    }

    /** `FA-24`: `FireNetworking#handleReload` — the real {@code ServerboundReloadPayload} receiver — reloads exactly as {@code FiringLogic#reload} does directly. */
    @GameTest(structure = "firearms_gametest:open_range")
    public void handleReloadOnAnEmptyWeaponWithMatchingCartridgesReloadsToMagazineSizeAndConsumesThem(GameTestHelper helper) {
        ServerPlayer player = mockShooter(helper);
        ItemStack stack = weaponStack(WeaponBase.M1911, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.getInventory().add(new ItemStack(ItemRegistration.cartridge(WeaponBase.M1911.caliber()), 10));

        FiringLogic.Outcome outcome = FireNetworking.handleReload(player);
        helper.assertTrue(outcome == FiringLogic.Outcome.RELOADED, "WEAPON-REQ-010: an empty weapon with matching cartridges must reload, got " + outcome);

        Ammo ammo = stack.get(ComponentRegistration.AMMO);
        int magazineSize = WeaponBase.M1911.baseStats().magazineSize();
        helper.assertTrue(ammo != null && ammo.loaded() == magazineSize,
            "WEAPON-REQ-010: loaded must reach the derived magazine size (" + magazineSize + "), got " + ammo);

        int remaining = countCartridges(player, WeaponBase.M1911);
        helper.assertValueEqual(remaining, 10 - magazineSize, "exactly magazineSize cartridges must be consumed from inventory");

        helper.succeed();
    }

    /**
     * `FA-24`, `WEAPON-DEC-008`: a dedicated reload press on a magazine that already has ammo is a
     * pure no-op — {@link FiringLogic.Outcome#NOT_NEEDED}, ammo and durability unchanged, and,
     * unlike a real reload, no cooldown started at all: a fire attempt made right afterward must
     * still succeed rather than reporting {@code ON_COOLDOWN}, proving nothing was started.
     */
    @GameTest(structure = "firearms_gametest:open_range")
    public void handleReloadOnAWeaponThatAlreadyHasAmmoReturnsNotNeededAndChangesNothing(GameTestHelper helper) {
        ServerPlayer player = mockShooter(helper);
        ItemStack stack = weaponStack(WeaponBase.M1911, 7);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        FiringLogic.Outcome outcome = FireNetworking.handleReload(player);
        helper.assertTrue(outcome == FiringLogic.Outcome.NOT_NEEDED,
            "WEAPON-DEC-008: a reload press on a magazine that already has ammo must be a no-op, got " + outcome);

        Ammo ammo = stack.get(ComponentRegistration.AMMO);
        helper.assertTrue(ammo != null && ammo.loaded() == 7, "ammo must be unchanged");
        helper.assertValueEqual(stack.getDamageValue(), 0, "durability must be unchanged");

        FiringLogic.Outcome fireOutcome = FiringLogic.attempt(helper.getLevel(), player, stack, InteractionHand.MAIN_HAND);
        helper.assertTrue(fireOutcome == FiringLogic.Outcome.FIRED,
            "WEAPON-DEC-008: NOT_NEEDED must never start a cooldown — a fire attempt right after must still succeed, got " + fireOutcome);

        helper.succeed();
    }

    /** `WEAPON-REQ-004`: pump behaves as semi for cooldown purposes — a second attempt inside that same delay is refused. */
    @GameTest(structure = "firearms_gametest:open_range")
    public void aPumpWeaponRefusesASecondShotInsideThePumpDelay(GameTestHelper helper) {
        ServerPlayer player = mockShooter(helper);
        ItemStack stack = weaponStack(WeaponBase.WINCHESTER_MODEL_1897, WeaponBase.WINCHESTER_MODEL_1897.baseStats().magazineSize());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        FiringLogic.Outcome first = FiringLogic.attempt(helper.getLevel(), player, stack, InteractionHand.MAIN_HAND);
        FiringLogic.Outcome second = FiringLogic.attempt(helper.getLevel(), player, stack, InteractionHand.MAIN_HAND);

        helper.assertTrue(first == FiringLogic.Outcome.FIRED, "the first pull must fire, got " + first);
        helper.assertTrue(second == FiringLogic.Outcome.ON_COOLDOWN, "a second pull inside the pump delay must be refused, got " + second);

        Ammo ammo = stack.get(ComponentRegistration.AMMO);
        helper.assertTrue(ammo != null && ammo.loaded() == WeaponBase.WINCHESTER_MODEL_1897.baseStats().magazineSize() - 1,
            "exactly one round must be consumed, not two");

        helper.succeed();
    }

    /** `COMBAT-REQ-005`: one trigger pull, all 8 pellets, one round consumed. */
    @GameTest(structure = "firearms_gametest:open_range")
    public void aShotgunTriggerPullSpawnsItsFullPelletCount(GameTestHelper helper) {
        ServerPlayer player = mockShooter(helper);
        ItemStack stack = weaponStack(WeaponBase.WINCHESTER_MODEL_1897, WeaponBase.WINCHESTER_MODEL_1897.baseStats().magazineSize());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        FiringLogic.Outcome outcome = FiringLogic.attempt(helper.getLevel(), player, stack, InteractionHand.MAIN_HAND);

        helper.assertTrue(outcome == FiringLogic.Outcome.FIRED, "got " + outcome);
        helper.assertEntitiesPresent(CombatRegistration.BULLET, WeaponBase.WINCHESTER_MODEL_1897.baseStats().pellets());
        Ammo ammo = stack.get(ComponentRegistration.AMMO);
        helper.assertTrue(ammo != null && ammo.loaded() == WeaponBase.WINCHESTER_MODEL_1897.baseStats().magazineSize() - 1,
            "COMBAT-REQ-005: exactly one round is consumed for the whole pull, not one per pellet");

        helper.succeed();
    }

    /** `WEAPON-REQ-013`, `COMBAT-REQ-009`: a suppressor changes which sound event {@code FireSounds} selects. */
    @GameTest
    public void aSuppressorChangesTheFireSoundEventChosen(GameTestHelper helper) {
        for (WeaponClass weaponClass : WeaponClass.values()) {
            SoundEvent unsuppressed = FireSounds.fireSound(weaponClass, false);
            SoundEvent suppressed = FireSounds.fireSound(weaponClass, true);
            helper.assertTrue(suppressed == FireSounds.FIRE_SUPPRESSED,
                "a suppressed shot must always resolve to the universal suppressed event for " + weaponClass);
            helper.assertTrue(unsuppressed != suppressed,
                "an unsuppressed shot must resolve to a different sound event than a suppressed one for " + weaponClass);
        }
        helper.succeed();
    }

    /** `WEAPON-FAIL-001`: not reachable in practice (nothing exposes firing on a non-weapon item), but `FiringLogic#attempt` still checks defensively. */
    @GameTest(structure = "firearms_gametest:open_range")
    public void attemptingToFireANonWeaponItemReportsNotAWeapon(GameTestHelper helper) {
        ServerPlayer player = mockShooter(helper);
        ItemStack stick = new ItemStack(Items.STICK);
        player.setItemInHand(InteractionHand.MAIN_HAND, stick);

        FiringLogic.Outcome outcome = FiringLogic.attempt(helper.getLevel(), player, stick, InteractionHand.MAIN_HAND);
        helper.assertTrue(outcome == FiringLogic.Outcome.NOT_A_WEAPON, "a stack with no firearms:base must report NOT_A_WEAPON, got " + outcome);

        helper.succeed();
    }

    /**
     * `WEAPON-FAIL-004`: a slot component naming an attachment id a datapack has since removed is
     * treated as absent by {@code WeaponLoadouts#of}, the resolver `FiringLogic#attempt` itself
     * calls — no crash, that slot simply contributes nothing to the resolved {@link Loadout}.
     */
    @GameTest(structure = "firearms_gametest:open_range")
    public void aRemovedAttachmentIdIsTreatedAsAnAbsentSlotNotACrash(GameTestHelper helper) {
        ItemStack stack = weaponStack(WeaponBase.M1911, 0);
        stack.set(ComponentRegistration.attachmentComponent(Slot.MUZZLE), Firearms.id("no_longer_exists"));

        Optional<Loadout> resolved = WeaponLoadouts.of(stack);
        helper.assertTrue(resolved.isPresent(), "a missing attachment id must not fail the whole weapon's resolution");
        helper.assertTrue(resolved.get().attachment(Slot.MUZZLE).isEmpty(),
            "a removed attachment id must be treated as an empty muzzle slot, not crash or carry a phantom value");

        helper.succeed();
    }

    /**
     * {@link GameTestHelper#makeMockServerPlayerInLevel()} is the only helper that gives the mock
     * player a live (embedded-channel) connection — required since {@link FiringLogic#fire} sends a
     * {@code RecoilPacket} to the shooter every shot, which needs one (`FA-20`; the older, unconnected
     * {@code makeMockServerPlayer} NPEs the instant a shot starts a cooldown, since even that alone
     * syncs the cooldown to the client). Two things it does *not* give a mock player, that this method
     * corrects: {@code PlayerList.placeNewPlayer} spawns it at the level's real spawn point, nowhere
     * near this test's own structure, so {@link FiringLogic#fire}'s {@code player.getEyePosition()}
     * origin has to be moved into the structure by hand; and {@code GameTestHelper}'s own mock-player
     * class hard-codes {@code gameMode() == CREATIVE}, which leaves {@code abilities.instabuild} set
     * and makes every {@code hurtAndBreak} call a no-op (`ItemStack#processDurabilityChange`) —
     * durability would never move without resetting it.
     */
    private static ServerPlayer mockShooter(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setPos(helper.absoluteVec(new Vec3(0.5, 2.5, 0.5)));
        player.getAbilities().instabuild = false;
        return player;
    }

    private static ItemStack weaponStack(WeaponBase base, int ammoLoaded) {
        ItemStack stack = new ItemStack(ItemRegistration.WEAPON);
        stack.set(ComponentRegistration.BASE, Base.of(Firearms.id(base.id())));
        stack.set(DataComponents.MAX_DAMAGE, base.baseStats().durability());
        stack.set(DataComponents.DAMAGE, 0);
        if (ammoLoaded > 0) {
            stack.set(ComponentRegistration.AMMO, new Ammo(Firearms.id(Ids.slug(base.caliber())), ammoLoaded));
        }
        return stack;
    }

    private static int countCartridges(ServerPlayer player, WeaponBase base) {
        int count = 0;
        for (ItemStack candidate : player.getInventory().getNonEquipmentItems()) {
            if (candidate.getItem() == ItemRegistration.cartridge(base.caliber())) {
                count += candidate.getCount();
            }
        }
        return count;
    }
}
