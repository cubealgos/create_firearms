package firearms.item;

import firearms.client.scope.ScopedWeapon;
import firearms.component.Base;
import firearms.component.ComponentRegistration;
import firearms.fire.FiringLogic;
import firearms.fire.WeaponLoadouts;
import firearms.model.FireMode;
import firearms.model.Loadout;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * The one weapon item, {@code firearms:weapon}: every 1.0 base weapon is a stack of this same
 * item, distinguished only by its own {@code firearms:base.weapon_id} component
 * (`docs/spec/04-architecture.md` `ARCH-DEC-005`, `docs/spec/contracts/public-surface.md`) — the
 * mechanism `docs/spec/domains/weapon.md` {@code WEAPON-DEC-003} relies on: a new base weapon in
 * an existing class is one data file plus one crafting recipe, never a new {@code Item}
 * registration. Carries no {@code repairable}/{@code enchantable} component
 * (`docs/spec/decisions/DEC-017-no-detach-durability.md`), and no fixed {@code durability(int)}
 * either — {@code max_damage} differs per base, so each base's own crafting recipe sets it via a
 * {@code components} output patch instead of one shared {@link Item.Properties} value
 * (`firearms.item.ItemRegistration`).
 *
 * <p>Firing (`FA-6`, `docs/spec/domains/weapon.md` `WEAPON-REQ-004`, `007`-`013`): {@link #use}
 * starts the vanilla "using item" session uniformly, for every fire mode, on both logical sides —
 * the fire mode itself lives in server-only weapon data ({@code firearms.data.WeaponRegistry}, a
 * {@code PackType.SERVER_DATA} reload listener a remote client never has loaded), so deciding
 * whether to start using here, before that data is even reachable, would be wrong on a real
 * dedicated server. All the real, mode-aware pacing happens server-side in {@link #onUseTick},
 * dispatched through {@link FiringLogic#attempt} and gated on vanilla {@code ItemCooldowns}: {@code
 * semi}/{@code pump} take exactly one attempt per press and then stop the session themselves
 * (`FireMode`'s own "blocked ... regardless of how long the control is held"); {@code auto} keeps
 * attempting every tick the cooldown allows, for as long as the control stays held, and stops
 * itself the instant the magazine is empty (`UC-008` step 3) rather than auto-reloading mid-hold —
 * a fresh press is what starts the next reload attempt. {@link #useOn} funnels a block-targeted
 * right click through the exact same path, so aiming
 * point-blank at a block still fires.
 */
public final class WeaponItem extends Item {
    private static final String NAME_PREFIX = "item.firearms.weapon.";
    /**
     * Large enough that no fire mode's own pacing ever runs into it; the "using" session always
     * ends itself first, via {@link #onUseTick} or {@link #releaseUsing}, never by this duration
     * naturally expiring (mirrors the bow's own 72000-tick convention for "effectively unbounded").
     */
    private static final int HOLD_DURATION_TICKS = 72000;

    public WeaponItem(Properties properties) {
        super(properties);
    }

    /**
     * "M1911", "AKM", and so on, read off this stack's own {@code firearms:base} component; falls
     * back to the item's own default name (`item.firearms.weapon` in {@code en_us.json}) for a
     * bare stack carrying no component, e.g. a creative-mode {@code /give} with none given.
     */
    @Override
    public Component getName(ItemStack stack) {
        Base base = stack.get(ComponentRegistration.BASE);
        if (base == null) {
            return super.getName(stack);
        }
        return Component.translatable(NAME_PREFIX + base.weaponId().getPath());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    /** Funnels a block-targeted right click through {@link #use}, so firing point-blank at a block behaves identically to firing at open air or an entity. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        return use(context.getLevel(), player, context.getHand());
    }

    /**
     * The one server-authoritative fire-or-reload evaluation for this held tick
     * (`FiringLogic#attempt`); client-side (and any non-{@link ServerPlayer} shooter) is a no-op,
     * since every real decision needs the server-only weapon/attachment registries.
     */
    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (!(level instanceof ServerLevel serverLevel) || !(livingEntity instanceof ServerPlayer player)) {
            return;
        }
        Optional<Loadout> loadout = WeaponLoadouts.of(stack);
        if (loadout.isEmpty()) {
            player.stopUsingItem();
            return;
        }

        FiringLogic.Outcome outcome = FiringLogic.attempt(serverLevel, player, stack, player.getUsedItemHand());
        boolean auto = loadout.get().base().baseStats().fireMode() == FireMode.AUTO;
        // UC-008 step 3: auto stops "the instant firearms:ammo.loaded reaches zero" — RELOADED and
        // EMPTY_CLICK both only ever happen when the magazine was already empty at this attempt, so
        // both end the session here rather than auto-reloading and continuing mid-hold; a fresh
        // press (a new use()) is what triggers the next reload attempt. semi/pump always take
        // exactly one evaluation per press, so they stop regardless of what that one attempt did.
        boolean magazineWasEmpty = outcome == FiringLogic.Outcome.RELOADED || outcome == FiringLogic.Outcome.EMPTY_CLICK;
        if (!auto || magazineWasEmpty) {
            player.stopUsingItem();
        }
    }

    /**
     * {@code ItemUseAnimation.SPYGLASS} while {@code stack} carries a magnifying optic, so the arm
     * pose matches the vanilla spyglass's own while scoped; {@code NONE} otherwise. This alone gets
     * none of the zoom, the overlay, or the held-item suppression — those three all gate on {@code
     * Player.isScoping()}, an exact item-identity check that never inspects this animation
     * (`docs/spec/domains/combat.md` {@code COMBAT-REQ-006}; `firearms.mixin.client
     * .PlayerScopingMixin`) — this only reproduces the pose vanilla's own spyglass use already has.
     * {@link ScopedWeapon#hasZoomingOptic} is common-safe: it reads only this stack's own
     * network-synchronized components, no client-only Minecraft API (`FA-10`).
     */
    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ScopedWeapon.hasZoomingOptic(stack) ? ItemUseAnimation.SPYGLASS : ItemUseAnimation.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity livingEntity) {
        return HOLD_DURATION_TICKS;
    }

    /**
     * Firing/reload pacing all happens per-tick in {@link #onUseTick}, itself gated on {@code
     * ItemCooldowns}; releasing the control early just ends the session ({@code LivingEntity}'s own
     * state machine already does that), so there is nothing further for this mod to do here.
     */
    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeCharged) {
        return false;
    }
}
