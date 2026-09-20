/**
 * The scope mechanic's client-only support: resolving a held weapon's optic and its zoom factor
 * and overlay texture (`docs/spec/domains/combat.md` {@code COMBAT-REQ-006}–{@code 008}), read by
 * the three mixins in {@code firearms.mixin.client} that actually apply it. Deliberately independent
 * of {@code firearms.data.AttachmentRegistry} — that registry is a {@code PackType.SERVER_DATA}
 * reload listener a remote client never has loaded ({@code firearms.item.WeaponItem}'s own doc) —
 * so every lookup here goes through {@code firearms.model.Attachment#ALL}'s hardcoded constants
 * instead, present identically on every client regardless of which side loaded which datapack.
 */
package firearms.client.scope;
