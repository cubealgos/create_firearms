package firearms.component;

import java.util.Objects;
import net.minecraft.resources.Identifier;

/**
 * The {@code firearms:base} component: {@code { version, weapon_id }}, present on every item of
 * this mod's own weapon type (`docs/spec/contracts/data-contract.md`). {@code version} is the
 * single schema version governing all seven of this mod's own component types together
 * ({@code DATA-REQ-001}) — the weapon's own identity plus every present {@code
 * firearms:attachment_<slot>} and {@code firearms:ammo} value are read and migrated as one unit,
 * keyed off this one field, never off a per-component version of their own.
 *
 * @param version the schema version this value was written at
 * @param weaponId which of {@code firearms.model.WeaponBase}'s ids this weapon is, resolved at
 *     read time against the loaded weapon registry (`firearms.data.WeaponRegistry`)
 */
public record Base(int version, Identifier weaponId) {

    public Base {
        Objects.requireNonNull(weaponId, "weaponId");
        if (version < 1) {
            throw new IllegalArgumentException("version must be at least 1, was " + version);
        }
    }

    /** {@code weaponId} at the current schema version (`BaseCodec#VERSION`), for a freshly-crafted weapon. */
    public static Base of(Identifier weaponId) {
        return new Base(BaseCodec.VERSION, weaponId);
    }

    /**
     * Written by a newer build than this one (`docs/spec/contracts/data-contract.md`
     * {@code DATA-REQ-003}): the weapon is treated as bare and unmodifiable rather than
     * reinterpreted, mirroring {@code create_metered_motor}'s own {@code Stats#readOnly()}.
     */
    public boolean readOnly() {
        return version > BaseCodec.VERSION;
    }
}
