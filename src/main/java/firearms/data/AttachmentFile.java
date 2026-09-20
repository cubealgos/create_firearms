package firearms.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import firearms.model.Attachment;
import firearms.model.Modifier;
import firearms.model.Slot;
import java.util.List;

/**
 * A {@code data/firearms/attachment/*.json} file's own fields — {@code slot}, {@code modifiers}
 * and optional {@code zoom} — without the attachment's own id, which is its file's name
 * (`docs/spec/domains/attach.md` §3). {@link AttachmentDataLoader#apply} combines one of these with
 * its file id into the real {@link Attachment} the rest of this mod reads.
 *
 * @param slot the one slot this attachment fits
 * @param modifiers this attachment's effect on the base weapon's stats
 * @param zoom the scope magnification an optic contributes; {@code 1.0} ("none") by default, the
 *     value every non-optic and every non-magnifying optic (red dot, holo) uses
 */
record AttachmentFile(Slot slot, List<Modifier> modifiers, double zoom) {
    static final Codec<Slot> SLOT = EnumCodec.of(Slot.class);

    static final Codec<AttachmentFile> CODEC = RecordCodecBuilder.create(b -> b.group(
        SLOT.fieldOf("slot").forGetter(AttachmentFile::slot),
        ModifierCodec.CODEC.listOf().fieldOf("modifiers").forGetter(AttachmentFile::modifiers),
        Codec.DOUBLE.optionalFieldOf("zoom", 1.0).forGetter(AttachmentFile::zoom)
    ).apply(b, AttachmentFile::new));

    /** This file's data plus its own id (the file's name), as the rest of this mod reads it. */
    Attachment toAttachment(String id) {
        return new Attachment(id, slot, modifiers, zoom);
    }
}
