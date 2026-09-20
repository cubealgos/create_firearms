package firearms.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * A base weapon plus zero or more present attachments, one per slot
 * (`docs/spec/domains/weapon.md` §2 "Multiplicity"). Immutable: {@link #with(Attachment)} returns
 * a new {@code Loadout} rather than mutating this one. Validates only that an attachment's slot
 * exists on the base weapon's class (`WeaponClass#hasSlot`); it does not decide whether a slot is
 * already occupied — that match/merge logic belongs to the shared attach function landing at
 * `FA-7`, not this ticket's derivation-only scope.
 */
public final class Loadout {
    private final WeaponBase base;
    private final EnumMap<Slot, Attachment> attachments;

    private Loadout(WeaponBase base, EnumMap<Slot, Attachment> attachments) {
        this.base = base;
        this.attachments = attachments;
    }

    /** A weapon with no attachment present in any slot. */
    public static Loadout bare(WeaponBase base) {
        return new Loadout(base, new EnumMap<>(Slot.class));
    }

    /**
     * Returns a copy of this loadout with {@code attachment} present in its own slot, replacing
     * whatever this loadout already carried there.
     *
     * @throws IllegalArgumentException if {@code attachment}'s slot does not exist on this
     *     loadout's own weapon class (`docs/spec/domains/attach.md` `ATTACH-FAIL-001`)
     */
    public Loadout with(Attachment attachment) {
        if (!base.weaponClass().hasSlot(attachment.slot())) {
            throw new IllegalArgumentException(
                base.id() + "'s class " + base.weaponClass() + " has no " + attachment.slot()
                    + " slot, so " + attachment.id() + " cannot attach (ATTACH-FAIL-001)");
        }
        EnumMap<Slot, Attachment> next = new EnumMap<>(attachments);
        next.put(attachment.slot(), attachment);
        return new Loadout(base, next);
    }

    /** This loadout's base weapon. */
    public WeaponBase base() {
        return base;
    }

    /** The attachment present in {@code slot}, if any. */
    public Optional<Attachment> attachment(Slot slot) {
        return Optional.ofNullable(attachments.get(slot));
    }

    /** Every present attachment, keyed by slot, in {@link Slot}'s own fixed order. */
    public Map<Slot, Attachment> attachments() {
        return new EnumMap<>(attachments);
    }
}
