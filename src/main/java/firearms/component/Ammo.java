package firearms.component;

import java.util.Objects;
import net.minecraft.resources.Identifier;

/**
 * The {@code firearms:ammo} component: {@code { caliber, loaded }}, present once a weapon has been
 * loaded at least once (`docs/spec/contracts/data-contract.md`,
 * `docs/spec/domains/weapon.md` §3). Not versioned on its own — {@code firearms:base.version}
 * governs every one of this mod's own component types together (`DATA-REQ-001`). A malformed or
 * unreadable value degrades to absent (vanilla's own per-component decode-failure handling, not
 * special-cased here), which reload/firing code already reads as "no ammo loaded"
 * (`docs/spec/domains/weapon.md` `WEAPON-REQ-007`, `DATA-REQ-004`).
 *
 * @param caliber which calibre is loaded, an {@code Identifier} rather than {@code
 *     firearms.model.Caliber} directly so this component stays readable even if a future datapack
 *     calibre has no matching enum constant
 * @param loaded rounds currently chambered/in the magazine; a negative value never reaches this
 *     constructor — {@link AmmoCodec} rejects it at decode time, so the whole component degrades to
 *     absent rather than this record ever holding one (`docs/spec/contracts/data-contract.md`
 *     {@code DATA-REQ-004}), the same reasoning `firearms.model.Loadout` follows for its own
 *     application-level checks, kept out of this codec-facing carrier's constructor instead
 */
public record Ammo(Identifier caliber, int loaded) {

    public Ammo {
        Objects.requireNonNull(caliber, "caliber");
    }
}
