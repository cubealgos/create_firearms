package firearms.item;

import firearms.client.scope.ScopedWeapon;
import firearms.component.Base;
import firearms.component.ComponentRegistration;
import net.minecraft.network.chat.Component;
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
 * <p>Controls (`FA-24`, `docs/spec/decisions/DEC-019-controls.md`, `docs/spec/domains/weapon.md`
 * {@code WEAPON-REQ-018}, {@code 019}): this class is now aim-only. {@link #use} (and, through it,
 * {@link #useOn}) does nothing but start the vanilla "using item" session — it never dispatches a
 * fire-or-reload attempt itself, and there is no {@code onUseTick} override any more, so this class
 * is entirely decoupled from firing. Firing travels instead as this mod's own client-to-server
 * payload: the attack control sends {@code firearms.fire.ServerboundFirePayload}
 * ({@code firearms.client.fire.FireInputHandler}, with a client mixin cancelling vanilla's own
 * swing while a firearm is held), received server-side by {@code firearms.fire.FireNetworking
 * #handleFire}, which calls the exact same server-authoritative {@code
 * firearms.fire.FiringLogic#attempt} this class used to drive from {@code onUseTick} — cooldown,
 * ammo, durability and the bullet spawn all stay exactly as server-authoritative as before this
 * ticket (`WEAPON-REQ-018`). Aiming itself lasts exactly as long as the use control stays held: a
 * bow-length, 72000-tick session that never finishes on its own, ended only by {@link
 * #releaseUsing} on release (`WEAPON-REQ-019`) — never by a shot or a cooldown, which is what makes
 * this the fix for the pre-`FA-24` AWM bug (the old "use fires" scheme restarted the use session on
 * every shot/cooldown, so a held right click scoped, reset, and rescoped with no shot ever landing;
 * separating aim from fire by construction removes that loop entirely — `DEC-019`'s own "Why the
 * AWM looped"). {@link #useOn} funnels a block-targeted right click through the exact same
 * {@link #use} path, so aiming point-blank at a block still aims correctly.
 */
public final class WeaponItem extends Item {
    private static final String NAME_PREFIX = "item.firearms.weapon.";
    /**
     * Large enough that no player ever holds the use control this long; the aiming session always
     * ends itself first, via {@link #releaseUsing} on release, never by this duration naturally
     * expiring (mirrors the bow's own 72000-tick convention for "effectively unbounded").
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

    /** `WEAPON-REQ-019`: starts the aiming session, unconditionally, on both logical sides. Firing is never a side effect of this call (`DEC-019`). */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    /** Funnels a block-targeted right click through {@link #use}, so aiming point-blank at a block behaves identically to aiming at open air or an entity. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        return use(context.getLevel(), player, context.getHand());
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
     * Releasing the use control early just ends the aiming session (`WEAPON-REQ-019`) — {@code
     * LivingEntity}'s own state machine already does that once this returns — so there is nothing
     * further for this mod to do here; firing has no relationship to this call at all any more.
     */
    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeCharged) {
        return false;
    }
}
