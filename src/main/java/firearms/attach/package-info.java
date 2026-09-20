/**
 * The shared attach function and the smithing front end over it (`FA-7`,
 * `docs/spec/domains/attach.md`): {@link firearms.attach.Attach} reads and writes the
 * Minecraft-typed {@code firearms:base}/{@code firearms:attachment_<slot>} components, deferring
 * the pure empty-slot-and-class-has-it decision to {@link firearms.model.AttachRule}; {@link
 * firearms.attach.AttachSmithingRecipe} is the one {@code SmithingRecipe} implementor the vanilla
 * smithing table finds with zero mixin (`docs/spec/04-architecture.md` `ARCH-DEC-002`). The
 * deploying front end lands at {@code FA-8}, calling this same {@link firearms.attach.Attach}
 * rather than a second copy (`ATTACH-REQ-008`).
 */
package firearms.attach;
