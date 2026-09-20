package firearms.gametest;

import firearms.Firearms;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.VillagerTradeTags;
import net.minecraft.world.item.trading.VillagerTrade;

/**
 * FA-13: every weaponsmith and fletcher trade file this ticket ships resolves through
 * {@code Registries.VILLAGER_TRADE} and is tagged into its own trade level
 * (`TRADE-REQ-001`, `TRADE-REQ-002`; `docs/spec/domains/trade.md` §3's catalogue table). Mirrors
 * {@code create_metered_motor}'s own {@code TradeFileGameTest} pattern exactly.
 */
public final class TradeFileGameTest {

    private static final Map<String, TagKey<VillagerTrade>> TRADES = buildTrades();

    private static Map<String, TagKey<VillagerTrade>> buildTrades() {
        Map<String, TagKey<VillagerTrade>> trades = new HashMap<>();
        // Weaponsmith level 1 -- the two cheapest-to-craft 1.0 weapons, bare.
        put(trades, VillagerTradeTags.WEAPONSMITH_LEVEL_1,
            "weaponsmith/1/emerald_m1911",
            "weaponsmith/1/emerald_winchester_model_1897");
        // Weaponsmith level 2 -- muzzle attachments.
        put(trades, VillagerTradeTags.WEAPONSMITH_LEVEL_2,
            "weaponsmith/2/emerald_suppressor",
            "weaponsmith/2/emerald_compensator",
            "weaponsmith/2/emerald_flash_hider");
        // Weaponsmith level 3 -- bare weapons, magazine attachments, grip attachments.
        put(trades, VillagerTradeTags.WEAPONSMITH_LEVEL_3,
            "weaponsmith/3/emerald_micro_uzi",
            "weaponsmith/3/emerald_akm",
            "weaponsmith/3/emerald_extended_magazine",
            "weaponsmith/3/emerald_quickdraw_magazine",
            "weaponsmith/3/emerald_extended_quickdraw_magazine",
            "weaponsmith/3/emerald_vertical_grip",
            "weaponsmith/3/emerald_angled_grip",
            "weaponsmith/3/emerald_half_grip",
            "weaponsmith/3/emerald_light_grip",
            "weaponsmith/3/emerald_thumb_grip");
        // Weaponsmith level 4 -- bare weapons, stock attachments.
        put(trades, VillagerTradeTags.WEAPONSMITH_LEVEL_4,
            "weaponsmith/4/emerald_ruger_mini_14",
            "weaponsmith/4/emerald_awm",
            "weaponsmith/4/emerald_tactical_stock",
            "weaponsmith/4/emerald_cheek_pad",
            "weaponsmith/4/emerald_bullet_loops");
        // Weaponsmith level 5 -- optics, plus the six master buy-back trades.
        put(trades, VillagerTradeTags.WEAPONSMITH_LEVEL_5,
            "weaponsmith/5/emerald_red_dot",
            "weaponsmith/5/emerald_holo",
            "weaponsmith/5/emerald_scope_2x",
            "weaponsmith/5/emerald_scope_3x",
            "weaponsmith/5/emerald_scope_4x",
            "weaponsmith/5/emerald_scope_6x",
            "weaponsmith/5/emerald_scope_8x",
            "weaponsmith/5/emerald_scope_15x",
            "weaponsmith/5/master_m1911",
            "weaponsmith/5/master_micro_uzi",
            "weaponsmith/5/master_akm",
            "weaponsmith/5/master_ruger_mini_14",
            "weaponsmith/5/master_awm",
            "weaponsmith/5/master_winchester_model_1897");
        // Fletcher levels 1-4 -- cartridges, cheapest calibres first.
        put(trades, VillagerTradeTags.FLETCHER_LEVEL_1,
            "fletcher/1/emerald_cartridge_acp_45",
            "fletcher/1/emerald_cartridge_mm_9");
        put(trades, VillagerTradeTags.FLETCHER_LEVEL_2,
            "fletcher/2/emerald_cartridge_gauge_12");
        put(trades, VillagerTradeTags.FLETCHER_LEVEL_3,
            "fletcher/3/emerald_cartridge_mm_7_62",
            "fletcher/3/emerald_cartridge_mm_5_56");
        put(trades, VillagerTradeTags.FLETCHER_LEVEL_4,
            "fletcher/4/emerald_cartridge_magnum_300");
        return Map.copyOf(trades);
    }

    private static void put(Map<String, TagKey<VillagerTrade>> trades, TagKey<VillagerTrade> tag, String... paths) {
        for (String path : paths) {
            trades.put(path, tag);
        }
    }

    @GameTest
    public void everyTradeFileResolvesAndIsTaggedIntoItsLevel(GameTestHelper helper) {
        Registry<VillagerTrade> registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE);
        List<String> missing = new ArrayList<>();
        List<String> untagged = new ArrayList<>();
        TRADES.forEach((path, tag) -> {
            Holder.Reference<VillagerTrade> holder = registry.get(Firearms.id(path)).orElse(null);
            if (holder == null) {
                missing.add(path);
                return;
            }
            if (!holder.is(tag)) {
                untagged.add(path + " not tagged into " + tag.location());
            }
        });
        helper.assertTrue(missing.isEmpty(), "every trade file resolves in the villager_trade registry, missing: " + missing);
        helper.assertTrue(untagged.isEmpty(), "every trade is tagged into its own level: " + untagged);
        helper.assertTrue(TRADES.size() == 40, "the catalogue table's own trade count (34 weaponsmith + 6 fletcher), was " + TRADES.size());
        helper.succeed();
    }
}
