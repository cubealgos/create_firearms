package firearms.gametest;

import firearms.Firearms;
import firearms.component.Base;
import firearms.component.ComponentRegistration;
import firearms.item.ItemRegistration;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
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
 * FA-13, `docs/spec/domains/trade.md` §3 "The master buy-back trades", `TRADE-REQ-004`,
 * `TRADE-REQ-005`: the {@code weaponsmith/5/master_akm} trade's {@code ItemCost} accepts an AKM
 * carrying its named muzzle and optic attachments and rejects one missing either, while an
 * unconstrained slot (magazine) is never inspected at all — the structural guarantee research
 * `smithing-and-item-model-layers-26-2.md` §D.3 confirms. Also checks the offer's own {@code
 * gives} is the spec's own emerald payout (`TRADE-REQ-004`'s "gives an emerald payout").
 */
public final class MasterBuyBackGameTest {

    @GameTest
    public void masterAkmAcceptsTheNamedConfigurationWithAnyUnconstrainedSlot(GameTestHelper helper) {
        ItemCost cost = masterAkmCost(helper);

        ItemStack matching = akm();
        matching.set(ComponentRegistration.ATTACHMENT_MUZZLE, Firearms.id("suppressor"));
        matching.set(ComponentRegistration.ATTACHMENT_OPTIC, Firearms.id("scope_4x"));
        helper.assertTrue(cost.test(matching), "an AKM with the named muzzle and optic, no other slot filled, satisfies the buy-back predicate");

        ItemStack matchingWithFreeSlot = matching.copy();
        matchingWithFreeSlot.set(ComponentRegistration.ATTACHMENT_MAGAZINE, Firearms.id("extended_magazine"));
        helper.assertTrue(cost.test(matchingWithFreeSlot),
            "an unconstrained slot (magazine) filled with anything must still satisfy the predicate (TRADE-REQ-005)");

        helper.succeed();
    }

    @GameTest
    public void masterAkmRejectsAMissingOrWrongNamedAttachment(GameTestHelper helper) {
        ItemCost cost = masterAkmCost(helper);

        ItemStack noMuzzle = akm();
        noMuzzle.set(ComponentRegistration.ATTACHMENT_OPTIC, Firearms.id("scope_4x"));
        helper.assertTrue(!cost.test(noMuzzle), "an AKM missing the named muzzle attachment must not satisfy the predicate");

        ItemStack wrongMuzzle = akm();
        wrongMuzzle.set(ComponentRegistration.ATTACHMENT_MUZZLE, Firearms.id("compensator"));
        wrongMuzzle.set(ComponentRegistration.ATTACHMENT_OPTIC, Firearms.id("scope_4x"));
        helper.assertTrue(!cost.test(wrongMuzzle), "an AKM with the wrong muzzle attachment must not satisfy the predicate");

        ItemStack noOptic = akm();
        noOptic.set(ComponentRegistration.ATTACHMENT_MUZZLE, Firearms.id("suppressor"));
        helper.assertTrue(!cost.test(noOptic), "an AKM missing the named optic attachment must not satisfy the predicate");

        ItemStack wrongBase = new ItemStack(ItemRegistration.WEAPON);
        wrongBase.set(ComponentRegistration.BASE, Base.of(Firearms.id("m1911")));
        wrongBase.set(ComponentRegistration.ATTACHMENT_MUZZLE, Firearms.id("suppressor"));
        helper.assertTrue(!cost.test(wrongBase), "a different base weapon entirely must not satisfy the predicate");

        helper.succeed();
    }

    @GameTest
    public void masterAkmGivesFortyEightEmeralds(GameTestHelper helper) {
        MerchantOffer offer = masterAkmOffer(helper);
        ItemStack result = offer.getResult();
        helper.assertTrue(result.getItem() == Items.EMERALD, "the master buy-back trade gives emeralds, gave " + result.getItem());
        helper.assertValueEqual(result.getCount(), 48, "the AKM master buy-back gives 48 emeralds per docs/spec/domains/trade.md §3");
        helper.assertValueEqual(offer.getMaxUses(), 1, "a master buy-back trade uses at most once (TRADE-REQ-004)");
        helper.succeed();
    }

    private static ItemCost masterAkmCost(GameTestHelper helper) {
        return masterAkmOffer(helper).getItemCostA();
    }

    private static MerchantOffer masterAkmOffer(GameTestHelper helper) {
        Registry<VillagerTrade> registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE);
        VillagerTrade trade = registry.get(Firearms.id("weaponsmith/5/master_akm")).map(Holder.Reference::value).orElse(null);
        helper.assertTrue(trade != null, "set up: weaponsmith/5/master_akm resolves");
        Villager villager = helper.spawn(EntityTypes.VILLAGER, new BlockPos(1, 2, 1));
        LootParams params = new LootParams.Builder(helper.getLevel())
            .withParameter(LootContextParams.ORIGIN, helper.absoluteVec(villager.position()))
            .withParameter(LootContextParams.THIS_ENTITY, villager)
            .withParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED, Unit.INSTANCE)
            .create(LootContextParamSets.VILLAGER_TRADE);
        LootContext context = new LootContext.Builder(params).create(Optional.empty());
        MerchantOffer offer = trade.getOffer(context);
        helper.assertTrue(offer != null, "set up: the trade creates an offer for a fresh villager");
        return offer;
    }

    private static ItemStack akm() {
        ItemStack stack = new ItemStack(ItemRegistration.WEAPON);
        stack.set(ComponentRegistration.BASE, Base.of(Firearms.id("akm")));
        stack.set(DataComponents.MAX_DAMAGE, 400);
        stack.set(DataComponents.DAMAGE, 0);
        return stack;
    }
}
