/**
 * The shared attach function and its two front ends (`FA-7`, `FA-8`, `docs/spec/domains/attach.md`):
 * {@link firearms.attach.Attach} reads and writes the Minecraft-typed {@code firearms:base}/{@code
 * firearms:attachment_<slot>} components, deferring the pure empty-slot-and-class-has-it decision
 * to {@link firearms.model.AttachRule}; {@link firearms.attach.AttachSmithingRecipe} is the one
 * {@code SmithingRecipe} implementor the vanilla smithing table finds with zero mixin
 * (`docs/spec/04-architecture.md` `ARCH-DEC-002`), and {@link firearms.attach.AttachDeployingRecipe}
 * is the one {@code Recipe<ItemApplicationInput>} implementor a Create deployer finds the same way,
 * in both belt and world/depot mode (`ARCH-DEC-003`). Neither front end duplicates the other's
 * match/merge logic — both call {@link firearms.attach.Attach} directly (`ATTACH-REQ-008`).
 */
package firearms.attach;
