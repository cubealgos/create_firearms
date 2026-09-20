package firearms.data;

import firearms.Firearms;
import firearms.model.Attachment;
import java.util.Map;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Reads {@code data/firearms/attachment/*.json} into {@link AttachmentRegistry} on every resource
 * reload (`docs/spec/domains/attach.md` §2 "Multiplicity"). This mod's own 22 files under that path
 * ship the same numbers {@code firearms.model.Attachment}'s compiled constants do; a datapack
 * shipping a file at the same path wins, per ordinary resource-pack layering.
 */
public final class AttachmentDataLoader extends SimpleJsonResourceReloadListener<AttachmentFile>
    implements IdentifiableResourceReloadListener {

    public AttachmentDataLoader() {
        // "attachment", not "firearms/attachment": see WeaponDataLoader's own constructor note.
        super(AttachmentFile.CODEC, FileToIdConverter.json("attachment"));
    }

    @Override
    protected void apply(Map<Identifier, AttachmentFile> loaded, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Identifier, Attachment> attachments = loaded.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toAttachment(e.getKey().getPath())));
        AttachmentRegistry.set(attachments);
        Firearms.LOGGER.info("Loaded {} attachment(s) from data/firearms/attachment/", attachments.size());
    }

    @Override
    public Identifier getFabricId() {
        return Firearms.id("attachment_data");
    }
}
