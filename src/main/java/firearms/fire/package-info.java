/**
 * The fire-control loop (`FA-6`, `docs/spec/domains/weapon.md` `WEAPON-REQ-004`, `007`-`013`):
 * resolving a stack's live {@code Loadout}, the single fire-or-reload attempt {@code
 * firearms.item.WeaponItem} dispatches every eligible tick, the sounds it plays, and the cosmetic
 * recoil packet it sends the shooter. Entirely server-authoritative; the client-only receiver for
 * that packet lives in {@code firearms.client.fire} instead.
 */
package firearms.fire;
