/**
 * The bullet entity and the server-side combat resolution it drives: flight, hit testing,
 * damage, knockback, pellets, and the spread maths the firing code (FA-6) rolls through
 * ({@code docs/spec/domains/combat.md}). Not firing itself (cooldown, ammo, fire mode) and not
 * the client-only tracer renderer or scope mixins ({@code firearms.client.combat}).
 */
package firearms.combat;
