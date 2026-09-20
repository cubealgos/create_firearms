package firearms.combat;

import it.unimi.dsi.fastutil.doubles.DoubleDoubleImmutablePair;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A fired shot, or one shotgun pellet, in flight (`docs/spec/domains/combat.md` §3,
 * `04-architecture.md` `ARCH-DEC-004`). A plain {@link Projectile} — not {@code AbstractArrow}
 * or {@code AbstractHurtingProjectile} — so this class owns its own tick: gravity, the per-tick
 * swept-segment hit test (`COMBAT-REQ-002`), the damage/knockback on an entity hit
 * (`COMBAT-REQ-004`, `010`), and the fixed-life despawn (`COMBAT-REQ-003`). The firing code
 * (FA-6) never constructs one directly; it calls {@link BulletSpawner}.
 *
 * <p>Water is deliberately not special-cased: `domains/combat.md` §3's own flight row proposes
 * "unaffected flight" in water at 1.0, retuned at the balance sweep if that turns out wrong.
 *
 * <p>The gravity multiplier and the despawn life are this ticket's own open-question answers
 * (`domains/combat.md` §7: "first ticket, balance sweep") — {@link #GRAVITY_PER_TICK} is below
 * vanilla's arrow (0.05 blocks/tick², `AbstractArrow.getDefaultGravity()`), and {@link
 * #LIFE_TICKS} is the sheet's own proposed 40 (2 real seconds). Both are proposed starting
 * numbers, not balanced, exactly as `domains/weapon.md`'s roster table marks its own.
 */
public final class BulletEntity extends Projectile {
    /** Below vanilla arrow's own 0.05 blocks/tick² — cartridge rounds drop less over typical range. */
    private static final double GRAVITY_PER_TICK = 0.02;
    /** `domains/combat.md` §3, §7: proposed 40 ticks (2 real seconds). */
    private static final int LIFE_TICKS = 40;
    /**
     * A defensive cap only; at 1.0's velocities and {@link #LIFE_TICKS} this is never reached
     * (the AWM's fastest bullet at 10.0 blocks/tick over 40 ticks travels at most 400 blocks).
     * Guards against a future weapon or attachment pushing muzzle velocity high enough that life
     * alone would let a bullet simulate implausibly far.
     */
    private static final double MAX_TRAVEL_DISTANCE_SQ = 512.0 * 512.0;
    /** A small base value, comparable to an arrow's, not amplified per calibre at 1.0 (`domains/combat.md` §3). */
    private static final float KNOCKBACK_STRENGTH = 0.35f;

    private static final EntityDataAccessor<Float> DATA_DAMAGE =
        SynchedEntityData.defineId(BulletEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_PELLET =
        SynchedEntityData.defineId(BulletEntity.class, EntityDataSerializers.BOOLEAN);

    private Vec3 spawnPos = Vec3.ZERO;

    public BulletEntity(EntityType<? extends BulletEntity> type, Level level) {
        super(type, level);
    }

    /** Positions and launches the bullet; called once by {@link BulletSpawner} right after construction. */
    void shootFrom(Vec3 origin, Vec3 direction, double velocity) {
        setPos(origin.x, origin.y, origin.z);
        this.spawnPos = origin;
        setDeltaMovement(direction.normalize().scale(velocity));
        updateRotation();
    }

    public float getDamage() {
        return entityData.get(DATA_DAMAGE);
    }

    void setDamage(float damage) {
        entityData.set(DATA_DAMAGE, damage);
    }

    public boolean isPellet() {
        return entityData.get(DATA_PELLET);
    }

    void setPellet(boolean pellet) {
        entityData.set(DATA_PELLET, pellet);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_DAMAGE, 0.0f);
        builder.define(DATA_PELLET, false);
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 start = position();
        Vec3 movement = getDeltaMovement();
        HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity, ClipContext.Block.COLLIDER);
        boolean hit = hitResult.getType() != HitResult.Type.MISS;
        Vec3 end = hit ? hitResult.getLocation() : start.add(movement);
        setPos(end.x, end.y, end.z);
        updateRotation();

        if (hit) {
            hitTargetOrDeflectSelf(hitResult);
        }
        if (isRemoved()) {
            return;
        }

        setDeltaMovement(movement.x, movement.y - GRAVITY_PER_TICK, movement.z);

        if (tickCount >= LIFE_TICKS || position().distanceToSqr(spawnPos) >= MAX_TRAVEL_DISTANCE_SQ) {
            discard();
        }
    }

    /** `COMBAT-REQ-004`, `010`: damage via a `firearms:bullet` `DamageSource`, direct = this bullet, causing = the shooter, for free `pvp`/team gating. */
    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Entity target = hitResult.getEntity();
        Entity owner = getOwner();
        DamageSource damageSource = serverLevel.damageSources().source(CombatRegistration.BULLET_DAMAGE_TYPE, this, owner);
        boolean hurt = target.hurtServer(serverLevel, damageSource, getDamage());
        if (hurt && target instanceof LivingEntity livingTarget) {
            DoubleDoubleImmutablePair direction = calculateHorizontalHurtKnockbackDirection(livingTarget, damageSource);
            livingTarget.knockback(KNOCKBACK_STRENGTH, direction.leftDouble(), direction.rightDouble(), damageSource, 1.0f);
        }
        discard();
    }

    /** `COMBAT-FAIL-002`: removed immediately, no penetration, no ricochet; a small impact particle only. */
    @Override
    protected void onHitBlock(BlockHitResult hitResult) {
        super.onHitBlock(hitResult);
        if (level() instanceof ServerLevel serverLevel) {
            Vec3 pos = hitResult.getLocation();
            serverLevel.sendParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 6, 0.05, 0.05, 0.05, 0.01);
        }
        discard();
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
