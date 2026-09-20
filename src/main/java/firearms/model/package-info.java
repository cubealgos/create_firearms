/**
 * The pure part: the stat derivation function (`FA-2`) and the attach rule's own empty-slot-and-
 * class-has-it decision (`AttachRule`, `FA-7`), with no Minecraft imports
 * (docs/spec/operations/testing.md). The Minecraft-typed half of the shared attach function — the
 * one that actually reads and writes a real {@code ItemStack}'s components — lives in
 * {@code firearms.attach} instead.
 */
package firearms.model;
