package firearms.gametest;

import firearms.Firearms;
import firearms.item.ItemRegistration;
import firearms.trade.NoFirearmOffered;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * FA-13, `docs/spec/domains/trade.md` §7: a weaponsmith that already offers a {@code
 * firearms:weapon} stack refuses a second weapon-selling offer, through {@code
 * firearms:no_firearm_offered} reading the villager's live offers off {@code
 * LootContextParams.THIS_ENTITY} — modelled directly on {@code create_metered_motor}'s own {@code
 * NoDuplicateOfferGameTest}.
 */
public final class NoDuplicateOfferGameTest {

    /** The three weaponsmith levels this ticket's catalogue puts a bare-weapon sale trade at. */
    private static final List<String> WEAPON_SALE_TRADES = List.of(
        "weaponsmith/1/emerald_m1911",
        "weaponsmith/1/emerald_winchester_model_1897",
        "weaponsmith/3/emerald_micro_uzi",
        "weaponsmith/3/emerald_akm",
        "weaponsmith/4/emerald_ruger_mini_14",
        "weaponsmith/4/emerald_awm"
    );

    @GameTest
    public void aVillagerAlreadyOfferingAWeaponRefusesASecond(GameTestHelper helper) {
        Villager villager = helper.spawn(EntityTypes.VILLAGER, new BlockPos(1, 2, 1));
        ItemStack weapon = new ItemStack(ItemRegistration.WEAPON);
        villager.getOffers().add(new MerchantOffer(new ItemCost(Items.EMERALD, 10), weapon, 2, 5, 0.2f));

        LootContext context = tradeContext(helper, villager);
        NoFirearmOffered condition = new NoFirearmOffered();
        helper.assertTrue(!condition.test(context), "a villager already offering a weapon refuses a second weapon offer");
        helper.succeed();
    }

    @GameTest
    public void aVillagerWithNoWeaponOfferYetAllowsOne(GameTestHelper helper) {
        Villager villager = helper.spawn(EntityTypes.VILLAGER, new BlockPos(1, 2, 1));
        LootContext context = tradeContext(helper, villager);
        NoFirearmOffered condition = new NoFirearmOffered();
        helper.assertTrue(condition.test(context), "a villager offering nothing yet allows a first weapon offer");
        helper.succeed();
    }

    /**
     * The real production trade files, drawn in the same trade-by-trade order
     * {@code AbstractVillager.addOffersFromTradeSet} appends into a villager's live
     * {@code getOffers()} list (`firearms.trade.NoFirearmOffered`'s own javadoc): every
     * weapon-selling trade across all three levels that carry one is offered in turn, and at most
     * one ever succeeds, since each carries the {@code firearms:no_firearm_offered} guard.
     */
    @GameTest
    public void aWeaponsmithLevelledThroughAllLevelsDrawsAtMostOneWeaponOffer(GameTestHelper helper) {
        Registry<VillagerTrade> registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE);
        Villager villager = helper.spawn(EntityTypes.VILLAGER, new BlockPos(1, 2, 1));
        villager.setVillagerData(villager.getVillagerData()
            .withProfession(helper.getLevel().registryAccess(), VillagerProfession.WEAPONSMITH)
            .withLevel(5));

        int weaponOffersDrawn = 0;
        for (String path : WEAPON_SALE_TRADES) {
            VillagerTrade trade = registry.get(Firearms.id(path)).map(Holder.Reference::value).orElse(null);
            helper.assertTrue(trade != null, "set up: " + path + " resolves");
            if (trade == null) {
                continue;
            }
            MerchantOffer offer = trade.getOffer(tradeContext(helper, villager));
            if (offer != null) {
                villager.getOffers().add(offer);
                if (offer.getResult().getItem() == ItemRegistration.WEAPON) {
                    weaponOffersDrawn++;
                }
            }
        }

        helper.assertTrue(weaponOffersDrawn == 1,
            "a weaponsmith levelled through every weapon-selling trade draws exactly one weapon offer, drew " + weaponOffersDrawn);
        helper.succeed();
    }

    private static LootContext tradeContext(GameTestHelper helper, Villager villager) {
        LootParams params = new LootParams.Builder(helper.getLevel())
            .withParameter(LootContextParams.ORIGIN, helper.absoluteVec(villager.position()))
            .withParameter(LootContextParams.THIS_ENTITY, villager)
            .withParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED, Unit.INSTANCE)
            .create(LootContextParamSets.VILLAGER_TRADE);
        return new LootContext.Builder(params).create(Optional.empty());
    }
}
