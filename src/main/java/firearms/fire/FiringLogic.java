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
 * attempt per eligible press or held tick, entirely server-authoritative
 * (`docs/spec/04-architecture.md` {@code ARCH-DEC-005}). {@code firearms.item.WeaponItem}'s {@code
 * use}/{@code useOn}/{@code onUseTick} overrides are the only callers; the fire-mode dispatch (semi
 * once per press, auto repeating while held, pump like semi) lives there, since it depends on the
 * {@code LivingEntity} "using item" state machine {@code Item} itself owns — this class only ever
 * answers "what happens on one eligible tick", never "how many ticks am I eligible for".
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
        NOT_A_WEAPON
    }

    /**
     * `WEAPON-REQ-005`: no dedicated aim-down-sights control exists yet — the three scope mixins
     * ({@code COMBAT-REQ-006}-{@code 008}) that would give one land at a later ticket. This ticket
     * reads sneaking as the aiming signal in the meantime, per the ticket's own instruction; replace
     * this method's body with the real control once it exists, and nothing else in this class needs
     * to change.
     */
    public static boolean isAiming(Player player) {
        return player.isShiftKeyDown();
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
        double spreadDegrees = isAiming(player) ? stats.spreadWhileAiming() : stats.spread();
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
