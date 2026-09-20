package firearms.combat;

import firearms.Firearms;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Registers the bullet entity type as {@code firearms:bullet} (`contracts/public-surface.md`),
 * and names the resource key the data-driven {@code firearms:bullet} damage type
 * ({@code data/firearms/damage_type/bullet.json}) registers under — this class defines neither
 * the damage type's values nor its tag membership, both of which live entirely in data files.
 */
public final class CombatRegistration {
    private CombatRegistration() {
    }

    public static final ResourceKey<EntityType<?>> BULLET_KEY =
        ResourceKey.create(Registries.ENTITY_TYPE, Firearms.id("bullet"));

    /** `04-architecture.md` `ARCH-DEC-004`: a real, gravity-affected projectile entity, not a hitscan raycast. */
    public static final EntityType<BulletEntity> BULLET = Registry.register(
        BuiltInRegistries.ENTITY_TYPE,
        BULLET_KEY,
        EntityType.Builder.<BulletEntity>of(BulletEntity::new, MobCategory.MISC)
            .sized(0.1f, 0.1f)
            .clientTrackingRange(4)
            .updateInterval(1)
            .noSave()
            .noSummon()
            .build(BULLET_KEY));

    /** `COMBAT-REQ-004`: the key `data/firearms/damage_type/bullet.json` registers under. */
    public static final ResourceKey<DamageType> BULLET_DAMAGE_TYPE =
        ResourceKey.create(Registries.DAMAGE_TYPE, Firearms.id("bullet"));

    /**
     * The death message translation keys {@code bullet.json}'s {@code message_id} field
     * (`firearms.bullet`) produces: {@code death.attack.<message_id>} with no causing player,
     * {@code death.attack.<message_id>.player} when a player caused the kill. Named here, not
     * used at runtime (vanilla builds them from the JSON alone) — kept as literals only so
     * {@code SourceSurfaceTest} can check both have an `en_us` entry.
     */
    public static final String DEATH_MESSAGE_KEY = "death.attack.firearms.bullet";

    public static final String DEATH_MESSAGE_KEY_PLAYER = "death.attack.firearms.bullet.player";

    public static void register() {
        Firearms.LOGGER.info("Registered {}", BULLET_KEY.identifier());
    }
}
