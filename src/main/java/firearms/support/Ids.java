package firearms.support;

import java.util.Locale;

/**
 * The one naming rule every data-file-facing enum in this mod follows: a Java constant's own
 * {@code name()}, lowercased, is its wire form — in JSON data files (`docs/spec/domains/weapon.md`
 * §3, `docs/spec/domains/attach.md` §3), in item and component registry paths ({@code
 * firearms.item.ItemRegistration}), and in {@code firearms:ammo}'s own {@code caliber} field
 * (`docs/spec/contracts/data-contract.md`). One function, reused everywhere an enum from {@code
 * firearms.model} needs a stable textual id, so the same constant never gets two different
 * spellings in two different files.
 */
public final class Ids {
    private Ids() {
    }

    /** {@code value.name()}, lowercased — {@code Caliber.ACP_45} becomes {@code "acp_45"}. */
    public static String slug(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }
}
