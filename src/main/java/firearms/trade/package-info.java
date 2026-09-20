/**
 * The villager trade path (`FA-13`, `docs/spec/domains/trade.md`): {@link
 * firearms.trade.NoFirearmOffered} is the {@code firearms:no_firearm_offered} merchant predicate
 * guarding the weaponsmith's bare-weapon sale trades so a weaponsmith never holds two weapon
 * offers at once (`TRADE-REQ-004`'s sibling concern, `docs/spec/domains/trade.md` §7), mirroring
 * {@code create_metered_motor}'s own {@code no_motor_offered} condition exactly. {@link
 * firearms.trade.TradeRegistration} registers that condition type; the trade JSONs themselves and
 * the tag-merge files that reach the weaponsmith and fletcher live under {@code
 * data/firearms/villager_trade/} and {@code data/minecraft/tags/villager_trade/}, not in this
 * package, and register no new profession, job-site block, or POI (`TRADE-REQ-003`).
 */
package firearms.trade;
