/**
 * The client-only side of firing: {@code RecoilHandler} applies {@code firearms.fire.RecoilPacket}'s
 * cosmetic camera kick to the local player on receipt, then eases it back off over a few client
 * ticks via {@code RecoilKick}'s own pure recovery maths. Cosmetic only, carries no authority over
 * any hit, damage or ammo state ({@code COMBAT-REQ-011}, `docs/spec/domains/combat.md`).
 */
package firearms.client.fire;
