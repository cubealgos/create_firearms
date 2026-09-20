package firearms.data;

import firearms.model.WeaponBase;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

/**
 * The base weapons currently loaded from {@code data/firearms/weapon/*.json}
 * (`docs/spec/domains/weapon.md` `WEAPON-DEC-003`), refreshed on every data reload by {@link
 * WeaponDataLoader}. Nothing else in this mod keeps its own copy of this map — firing, the
 * tooltip, and the attach function (`FA-6`, `FA-7`) all resolve a weapon stack's own {@code
 * firearms:base.weapon_id} through {@link #get(Identifier)} at the moment they need it, matching
 * `docs/spec/04-architecture.md` `ARCH-DEC-005`'s "nothing is baked."
 */
public final class WeaponRegistry {
    private static volatile Map<Identifier, WeaponBase> weapons = Map.of();

    /** The weapon named {@code id}, if a currently-loaded data file defines one (`WEAPON-FAIL-004` for when it does not). */
    public static Optional<WeaponBase> get(Identifier id) {
        return Optional.ofNullable(weapons.get(id));
    }

    /** Every currently-loaded base weapon, keyed by id. */
    public static Map<Identifier, WeaponBase> all() {
        return weapons;
    }

    static void set(Map<Identifier, WeaponBase> loaded) {
        weapons = Map.copyOf(loaded);
    }

    private WeaponRegistry() {
    }
}
