package firearms.fire;

import firearms.Firearms;
import firearms.model.WeaponClass;
import firearms.support.Ids;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * The sound events firing plays (`docs/spec/domains/weapon.md` {@code WEAPON-REQ-013},
 * `docs/spec/domains/combat.md` {@code COMBAT-REQ-009}): one fire sound per weapon <em>class</em>
 * ({@code firearms:fire.<class>}), a single universal suppressed variant substituted whenever a
 * suppressor is attached ({@code firearms:fire.suppressed}, a shorter fixed range and played
 * quieter — see {@code FiringLogic}), and one universal reload and empty-click sound each.
 *
 * <p>This is a class-grained reading of {@code WEAPON-REQ-013}'s "a distinct fire sound ... per
 * weapon", not a literal per-base-weapon one — the ticket's own Build section names exactly these
 * four id shapes, and this sheet reads that as authoritative for what to build now; recorded as a
 * finding under FA-6's Constraints. The six weapons in a class already sound distinct from every
 * other class; a genuinely distinct sound per base weapon is nine more placeholder assets for no
 * gameplay difference at 1.0 and can be added later without touching this class's shape.
 *
 * <p>Placeholder audio only: near-silent generated {@code .ogg} files under {@code
 * assets/firearms/sounds/}, wired through {@code assets/firearms/sounds.json} — a later ticket
 * replaces the actual recordings.
 */
public final class FireSounds {
    /** `COMBAT-REQ-009`: shorter than the vanilla default variable range, carrying half of "quieter/shorter range"; {@code FiringLogic} carries the other half by playing it at a lower volume. */
    private static final float SUPPRESSED_RANGE_BLOCKS = 8.0f;

    public static final SoundEvent RELOAD = register("reload");
    public static final SoundEvent EMPTY = register("empty");
    public static final SoundEvent FIRE_SUPPRESSED = registerFixedRange("fire.suppressed", SUPPRESSED_RANGE_BLOCKS);

    private static final Map<WeaponClass, SoundEvent> FIRE_BY_CLASS = fireByClass();

    private FireSounds() {
    }

    /** The fire sound {@code FiringLogic} plays for a shot from {@code weaponClass}: {@link #FIRE_SUPPRESSED} when {@code suppressed}, else that class's own. */
    public static SoundEvent fireSound(WeaponClass weaponClass, boolean suppressed) {
        return suppressed ? FIRE_SUPPRESSED : FIRE_BY_CLASS.get(weaponClass);
    }

    private static Map<WeaponClass, SoundEvent> fireByClass() {
        EnumMap<WeaponClass, SoundEvent> map = new EnumMap<>(WeaponClass.class);
        for (WeaponClass weaponClass : WeaponClass.values()) {
            map.put(weaponClass, register("fire." + Ids.slug(weaponClass)));
        }
        return Map.copyOf(map);
    }

    private static SoundEvent register(String path) {
        Identifier id = Firearms.id(path);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    private static SoundEvent registerFixedRange(String path, float rangeBlocks) {
        Identifier id = Firearms.id(path);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createFixedRangeEvent(id, rangeBlocks));
    }

    /** Forces this class's static registrations to run; call once from {@link Firearms#onInitialize()}. */
    public static void register() {
    }
}
