package firearms.trade;

import com.mojang.serialization.MapCodec;
import firearms.item.ItemRegistration;
import java.util.Set;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * The {@code firearms:no_firearm_offered} merchant predicate: true unless the merchant that
 * {@code LootContextParams.THIS_ENTITY} names already offers a {@code firearms:weapon} stack
 * (`docs/spec/domains/trade.md` §7, settling that section's own open question by adding the
 * guard). Every 1.0 base weapon is a stack of the single {@link ItemRegistration#WEAPON} item,
 * distinguished only by its own {@code firearms:base} component
 * (`docs/spec/04-architecture.md` `ARCH-DEC-005`), so checking item identity alone — not a
 * per-weapon id — is enough to stop a weaponsmith from ever holding two weapon-selling offers at
 * once, regardless of which base weapons they are. Modelled directly on {@code
 * create_metered_motor}'s own {@code metered_motor:no_motor_offered} condition (research
 * `smithing-and-item-model-layers-26-2.md` §D.2): {@code VillagerTrade.getOffer} tests this
 * predicate against a {@link LootContext} built with {@code THIS_ENTITY} set to the villager and
 * the {@code MerchantOffers} instance drawn into is the villager's own live {@code getOffers()}
 * list, appended to trade-by-trade within the same level's batch, so no villager-hook fallback is
 * needed here either.
 */
public final class NoFirearmOffered implements LootItemCondition {
    public static final MapCodec<NoFirearmOffered> MAP_CODEC = MapCodec.unit(NoFirearmOffered::new);

    @Override
    public MapCodec<NoFirearmOffered> codec() {
        return MAP_CODEC;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Set.of(LootContextParams.THIS_ENTITY);
    }

    @Override
    public boolean test(LootContext context) {
        Entity entity = context.getOptionalParameter(LootContextParams.THIS_ENTITY);
        if (!(entity instanceof Merchant merchant)) {
            return true;
        }
        for (MerchantOffer offer : merchant.getOffers()) {
            if (offer.getResult().getItem() == ItemRegistration.WEAPON) {
                return false;
            }
        }
        return true;
    }
}
