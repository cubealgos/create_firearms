package firearms.fire;

import firearms.Firearms;
import firearms.combat.BulletSpawner;
import firearms.component.Ammo;
import firearms.component.ComponentRegistration;
import firearms.item.ItemRegistration;
import firearms.model.Attachment;
import firearms.model.Caliber;
import firearms.model.Loadout;
import firearms.model.Slot;
import firearms.model.StatDerivation;
import firearms.model.Stats;
import firearms.model.WeaponBase;
import firearms.support.Ids;
import java.util.Optional;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.phys.Vec3;

/**
 * The fire-control loop (`docs/spec/domains/weapon.md` {@code WEAPON-REQ-007}-{@code 013}): one
 * attempt per eligible network payload, entirely server-authoritative
 * (`docs/spec/04-architecture.md` {@code ARCH-DEC-005}). Since `docs/spec/decisions
 * /DEC-019-controls.md`, {@code firearms.fire.FireNetworking#handleFire}/{@code #handleReload} are
 * the only callers — one per {@code firearms.fire.ServerboundFirePayload}/{@code
 * ServerboundReloadPayload} the client sends — and the fire-mode pacing itself (semi once per
 * press, auto once per fire-rate interval while held, pump like semi) lives entirely client-side in
 * {@code firearms.client.fire.FireInputHandler}, since it depends on client input state
 * ({@code Options.keyAttack}) this class never sees; this class only ever answers "what happens on
 * one eligible attempt", never "how many attempts am I eligible for" — the vanilla {@code
 * ItemCooldowns} check inside {@link #attempt}/{@link #reload} is what rejects every attempt in
 * excess of the derived fire/reload rate regardless of how fast the client sends them.
 */
public final class FiringLogic {
    private FiringLogic() {
    }

    /** What one fire-control attempt did, for callers — including game tests — to assert on. */
    public enum Outcome {
        /** A shot (or a shotgun's pellets) was fired; ammo, durability and the fire-rate cooldown all moved. */
        FIRED,
        /** The magazine was empty and matching cartridges were found and loaded; the reload cooldown started. */
        RELOADED,
        /** The magazine was empty and no matching cartridge was found (`WEAPON-FAIL-003`): the empty-click sound played, nothing else changed. */
        EMPTY_CLICK,
        /** Still cooling down (firing or reloading) from a previous attempt: nothing happened. */
        ON_COOLDOWN,
        /** {@code stack} does not resolve to a loaded weapon (`WEAPON-FAIL-001`, not reachable in practice, but checked defensively). */
        NOT_A_WEAPON,
        /** The magazine already has ammo; a dedicated reload press when nothing needs reloading is a no-op. */
        NOT_NEEDED
    }

    /**
     * `WEAPON-REQ-019`, `docs/spec/decisions/DEC-019-controls.md`: the aim control is now the use
     * control itself, held — {@code player.isUsingItem() && player.getUseItem() == stack}, an exact
     * identity check, mirroring vanilla {@code Player.isScoping()}'s own {@code isUsingItem() &&
     * getUseItem().is(Items.SPYGLASS)} convention (`firearms.mixin.client.PlayerScopingMixin`'s own
     * doc). Sneaking is no longer read at all — `DEC-019` replaces it as the aiming signal now that
     * firing is the attack control and aiming is whatever the use control is doing.
     */
    public static boolean isAiming(Player player, ItemStack stack) {
        return player.isUsingItem() && player.getUseItem() == stack;
    }

    /**
     * One fire-control attempt for {@code stack}, held by {@code player} in {@code hand}: fires if
     * loaded and off cooldown, attempts a reload if the magazine is empty, or reports why neither
     * happened.
     */
    public static Outcome attempt(ServerLevel level, ServerPlayer player, ItemStack stack, InteractionHand hand) {
        Optional<Loadout> loadoutOpt = WeaponLoadouts.of(stack);
        if (loadoutOpt.isEmpty()) {
            return Outcome.NOT_A_WEAPON;
        }
        Loadout loadout = loadoutOpt.get();
        WeaponBase base = loadout.base();
        Stats stats = StatDerivation.stats(loadout,
            message -> Firearms.LOGGER.warn("firearms:{} stat clamp: {}", base.id(), message));

        ItemStack cooldownKey = cooldownKey(stack, base);
        ItemCooldowns cooldowns = player.getCooldowns();
        if (cooldowns.isOnCooldown(cooldownKey)) {
            return Outcome.ON_COOLDOWN;
        }

        Ammo ammo = stack.get(ComponentRegistration.AMMO);
        if (ammo == null || ammo.loaded() <= 0) {
            return reload(level, player, stack, cooldowns, cooldownKey, base, stats);
        }
        return fire(level, player, stack, hand, cooldowns, cooldownKey, loadout, stats, ammo);
    }

    /**
     * The dedicated reload control's own entry point (`WEAPON-REQ-010`, `011`, `WEAPON-DEC-008`):
     * unlike {@link #attempt}, which auto-reloads an empty magazine as a side effect of an attack
     * payload arriving to an empty gun, this method is reachable only from
     * {@code firearms.fire.FireNetworking#handleReload}, the reload {@code KeyMapping}'s own
     * receiver — firing must never be a side effect of pressing reload, and this method never calls
     * {@link #fire}. Shares {@link #attempt}'s own opening (resolve the loadout, derive stats, check
     * the weapon's cooldown) since a reload attempt is paced by the exact same {@code ItemCooldowns}
     * group a fire attempt is (`WEAPON-REQ-009`'s mechanism, `cooldownKey`'s own doc); the one
     * difference is what happens once the magazine is inspected: an already-loaded magazine does
     * nothing at all — no cooldown started, no sound, no state change — and reports
     * {@link Outcome#NOT_NEEDED} rather than falling through to {@link #fire} the way {@link
     * #attempt} would if it read a nonzero {@code loaded} count (`attempt` never reaches this branch
     * in that case; this method's own guard is what keeps a reload press from ever firing).
     */
    public static Outcome reload(ServerLevel level, ServerPlayer player, ItemStack stack) {
        Optional<Loadout> loadoutOpt = WeaponLoadouts.of(stack);
        if (loadoutOpt.isEmpty()) {
            return Outcome.NOT_A_WEAPON;
        }
        Loadout loadout = loadoutOpt.get();
        WeaponBase base = loadout.base();
        Stats stats = StatDerivation.stats(loadout,
            message -> Firearms.LOGGER.warn("firearms:{} stat clamp: {}", base.id(), message));

        ItemStack cooldownKey = cooldownKey(stack, base);
        ItemCooldowns cooldowns = player.getCooldowns();
        if (cooldowns.isOnCooldown(cooldownKey)) {
            return Outcome.ON_COOLDOWN;
        }

        Ammo ammo = stack.get(ComponentRegistration.AMMO);
        if (ammo != null && ammo.loaded() > 0) {
            // WEAPON-DEC-008: a dedicated reload press when the magazine already has ammo is a
            // pure no-op — no cooldown, no sound, no state change — unlike attempt()'s own
            // empty-magazine-only dispatch into this same private reload() helper below.
            return Outcome.NOT_NEEDED;
        }
        return reload(level, player, stack, cooldowns, cooldownKey, base, stats);
    }

    private static Outcome reload(
            ServerLevel level, ServerPlayer player, ItemStack stack, ItemCooldowns cooldowns,
            ItemStack cooldownKey, WeaponBase base, Stats stats) {
        Caliber caliber = base.caliber();
        Item cartridgeItem = ItemRegistration.cartridge(caliber);
        Inventory inventory = player.getInventory();
        int available = countCartridges(inventory, cartridgeItem);
        if (available <= 0) {
            // WEAPON-REQ-011, WEAPON-FAIL-003: ammo left unchanged, no error beyond the cue.
            playSound(level, player, FireSounds.EMPTY, 1.0f);
            return Outcome.EMPTY_CLICK;
        }

        int toLoad = Math.min(available, stats.magazineSize());
        consumeCartridges(inventory, cartridgeItem, toLoad);
        stack.set(ComponentRegistration.AMMO, new Ammo(caliberId(caliber), toLoad));
        cooldowns.addCooldown(cooldownKey, stats.reloadTicks());
        playSound(level, player, FireSounds.RELOAD, 1.0f);
        return Outcome.RELOADED;
    }

    private static Outcome fire(
            ServerLevel level, ServerPlayer player, ItemStack stack, InteractionHand hand, ItemCooldowns cooldowns,
            ItemStack cooldownKey, Loadout loadout, Stats stats, Ammo ammo) {
        Vec3 origin = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double spreadDegrees = isAiming(player, stack) ? stats.spreadWhileAiming() : stats.spread();
        float damage = (float) stats.damage();

        if (stats.pellets() > 1) {
            BulletSpawner.spawnPellets(level, player, origin, look, stats.muzzleVelocity(), spreadDegrees, damage, stats.pellets());
        } else {
            BulletSpawner.spawnBullet(level, player, origin, look, stats.muzzleVelocity(), spreadDegrees, damage);
        }

        stack.set(ComponentRegistration.AMMO, new Ammo(ammo.caliber(), ammo.loaded() - 1));
        stack.hurtAndBreak(1, player, hand); // WEAPON-REQ-008: one durability point per shot.
        cooldowns.addCooldown(cooldownKey, stats.fireRateTicks());

        boolean suppressed = loadout.attachment(Slot.MUZZLE)
            .map(muzzle -> muzzle.id().equals(Attachment.SUPPRESSOR.id()))
            .orElse(false);
        SoundEvent sound = FireSounds.fireSound(loadout.base().weaponClass(), suppressed);
        // COMBAT-REQ-009: quieter, on top of the shorter fixed range FireSounds.FIRE_SUPPRESSED already carries.
        playSound(level, player, sound, suppressed ? 0.5f : 1.0f);

        ServerPlayNetworking.send(player, new RecoilPacket((float) stats.recoilVertical(), (float) stats.recoilHorizontal()));

        return Outcome.FIRED;
    }

    /**
     * {@link ItemCooldowns} keys every stack by {@link ItemCooldowns#getCooldownGroup}, which
     * defaults to the item's own registry id when no {@code minecraft:use_cooldown} component
     * overrides it. Every base weapon shares the one {@code firearms:weapon} item ({@code
     * firearms.item.WeaponItem}'s own Javadoc), so that default would put every weapon on the same
     * cooldown regardless of which base it is — an M1911 fired empty-handed would block an unrelated
     * AKM in the other hand. This mod never persists that override on the real stack
     * (`docs/spec/contracts/data-contract.md` {@code DATA-REQ-005}: no extra state of its own);
     * instead every cooldown check and every cooldown start goes through a throwaway copy carrying
     * the override just for this one lookup, keyed to the base weapon's own id ({@code
     * firearms:m1911}, and so on) — genuinely "the weapon's own cooldown group" (`WEAPON-REQ-008`,
     * `009`), never the one shared item's.
     */
    private static ItemStack cooldownKey(ItemStack stack, WeaponBase base) {
        ItemStack key = stack.copy();
        key.set(DataComponents.USE_COOLDOWN, new UseCooldown(0.0f, Optional.of(Firearms.id(base.id()))));
        return key;
    }

    private static Identifier caliberId(Caliber caliber) {
        return Firearms.id(Ids.slug(caliber));
    }

    private static void playSound(ServerLevel level, ServerPlayer player, SoundEvent sound, float volume) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, 1.0f);
    }

    /** `AMMO-REQ-004`: scans the player's main inventory, the same way the potato cannon's own ammo lookup does (research §A.1). */
    private static int countCartridges(Inventory inventory, Item cartridgeItem) {
        int count = 0;
        for (ItemStack candidate : inventory.getNonEquipmentItems()) {
            if (candidate.getItem() == cartridgeItem) {
                count += candidate.getCount();
            }
        }
        return count;
    }

    private static void consumeCartridges(Inventory inventory, Item cartridgeItem, int amount) {
        int remaining = amount;
        for (ItemStack candidate : inventory.getNonEquipmentItems()) {
            if (remaining <= 0) {
                break;
            }
            if (candidate.getItem() == cartridgeItem) {
                int take = Math.min(remaining, candidate.getCount());
                candidate.shrink(take);
                remaining -= take;
            }
        }
    }
}
