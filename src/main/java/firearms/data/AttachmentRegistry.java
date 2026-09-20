package firearms.data;

import firearms.model.Attachment;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

/**
 * The attachments currently loaded from {@code data/firearms/attachment/*.json}
 * (`docs/spec/domains/attach.md` §2 "Multiplicity"), refreshed on every data reload by {@link
 * AttachmentDataLoader}. Mirrors {@link WeaponRegistry} exactly, for the identical reason: nothing
 * about an attachment's stats is ever baked onto an item.
 */
public final class AttachmentRegistry {
    private static volatile Map<Identifier, Attachment> attachments = Map.of();

    /** The attachment named {@code id}, if a currently-loaded data file defines one (`WEAPON-FAIL-004`). */
    public static Optional<Attachment> get(Identifier id) {
        return Optional.ofNullable(attachments.get(id));
    }

    /** Every currently-loaded attachment, keyed by id. */
    public static Map<Identifier, Attachment> all() {
        return attachments;
    }

    static void set(Map<Identifier, Attachment> loaded) {
        attachments = Map.copyOf(loaded);
    }

    private AttachmentRegistry() {
    }
}
