package firearms.item;

import firearms.component.ComponentRegistration;
import firearms.model.Slot;
import java.util.Objects;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * One of the five attachment items, {@code firearms:attachment_<slot>}: every attachment in that
 * slot — three muzzle attachments, eight optics, three magazines, five grips, three stocks
 * (`docs/spec/domains/attach.md` §3) — is a stack of the one item for its own slot, distinguished
 * by that same slot's own {@code firearms:attachment_<slot>} component carrying its specific
 * attachment id. The shared attach function (`FA-7`, `FA-8`) copies that exact component value onto
 * the target weapon's own copy of the same component type, so an attachment item and an attached
 * slot read identically without a translation step. Crafting a specific attachment (a suppressor
 * vs. a compensator, both {@link Slot#MUZZLE}) is purely a matter of which {@code components}
 * output patch that attachment's own recipe carries (`firearms.item.ItemRegistration`); adding a
 * new attachment to an existing slot needs no new {@code Item} registration, mirroring {@link
 * WeaponItem}'s own reasoning (`docs/spec/domains/attach.md` "22 attachment items exist at 1.0
 * across 5 slots; a datapack may add more per slot with zero new Java").
 */
public final class AttachmentItem extends Item {
    private static final String NAME_PREFIX = "item.firearms.attachment.";

    private final Slot slot;

    public AttachmentItem(Slot slot, Properties properties) {
        super(properties);
        this.slot = Objects.requireNonNull(slot, "slot");
    }

    /** The one slot every stack of this item fits, fixed at registration. */
    public Slot slot() {
        return slot;
    }

    /**
     * "Suppressor", "Compensator", and so on, read off this stack's own slot component; falls back
     * to the item's own default name (e.g. {@code item.firearms.attachment_muzzle}) for a bare
     * stack carrying no component.
     */
    @Override
    public Component getName(ItemStack stack) {
        Identifier attachmentId = stack.get(ComponentRegistration.attachmentComponent(slot));
        if (attachmentId == null) {
            return super.getName(stack);
        }
        return Component.translatable(NAME_PREFIX + attachmentId.getPath());
    }
}
