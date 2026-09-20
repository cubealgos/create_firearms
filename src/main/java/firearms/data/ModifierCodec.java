package firearms.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import firearms.model.Modifier;
import firearms.model.Op;
import firearms.model.Stat;

/** One attachment modifier: {@code { stat, op, value } } (`docs/spec/domains/attach.md` §3). */
final class ModifierCodec {
    static final Codec<Stat> STAT = EnumCodec.of(Stat.class);
    static final Codec<Op> OP = EnumCodec.of(Op.class);

    static final Codec<Modifier> CODEC = RecordCodecBuilder.create(b -> b.group(
        STAT.fieldOf("stat").forGetter(Modifier::stat),
        OP.fieldOf("op").forGetter(Modifier::op),
        Codec.DOUBLE.fieldOf("value").forGetter(Modifier::value)
    ).apply(b, Modifier::new));

    private ModifierCodec() {
    }
}
