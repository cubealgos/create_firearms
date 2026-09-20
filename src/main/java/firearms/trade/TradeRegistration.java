package firearms.trade;

import firearms.Firearms;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Registers the trade path's one loot condition type: {@code firearms:no_firearm_offered}
 * (`docs/spec/domains/trade.md` §7). Called once from {@link Firearms#onInitialize()}. The trade
 * JSONs and tag-merge files that actually reach the weaponsmith and fletcher need no registration
 * of their own — vanilla's data-driven villager trade and tag machinery finds them by path alone.
 */
public final class TradeRegistration {

    public static void register() {
        Registry.register(BuiltInRegistries.LOOT_CONDITION_TYPE, Firearms.id("no_firearm_offered"), NoFirearmOffered.MAP_CODEC);
    }

    private TradeRegistration() {
    }
}
