package firearms.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import firearms.support.Ids;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A {@code Codec<E>} for any enum, reading and writing {@link Ids#slug(Enum)}: a weapon or
 * attachment data file spells {@code WeaponClass.ASSAULT_RIFLE} as {@code "assault_rifle"}, {@code
 * Caliber.ACP_45} as {@code "acp_45"}, and so on, for every enum `firearms.model` exposes
 * (`docs/spec/domains/weapon.md` §3, `docs/spec/domains/attach.md` §3). An unrecognised value fails
 * the decode with the full set of valid ids named, rather than throwing — {@code
 * SimpleJsonResourceReloadListener}'s own {@code scanDirectory} already logs that failure and skips
 * just the one bad file, the graceful-degradation path this mod relies on instead of a bespoke one
 * (`docs/spec/contracts/public-surface.md` {@code SURFACE-REQ-002}).
 */
final class EnumCodec {
    private EnumCodec() {
    }

    static <E extends Enum<E>> Codec<E> of(Class<E> type) {
        Map<String, E> bySlug = Arrays.stream(type.getEnumConstants())
            .collect(Collectors.toMap(Ids::slug, e -> e));
        return Codec.STRING.flatXmap(
            s -> bySlug.containsKey(s)
                ? DataResult.success(bySlug.get(s))
                : DataResult.error(() -> "Unknown " + type.getSimpleName() + " '" + s + "', expected one of " + bySlug.keySet()),
            e -> DataResult.success(Ids.slug(e)));
    }
}
