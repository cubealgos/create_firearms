package firearms.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

/**
 * Every one of the 22 attachments, applied singly to a bare AKM (`WeaponClass.ASSAULT_RIFLE` has
 * all five slots, so it fits every attachment in turn), changes exactly the stats its own modifiers
 * name and leaves every other field untouched (`docs/spec/domains/attach.md` §3,
 * `docs/spec/operations/testing.md`).
 */
final class AttachmentModifierTest {
    private static final Consumer<String> NO_CLAMP = message -> {
        throw new AssertionError("a single 1.0 attachment on the AKM should never clamp, but: " + message);
    };
    private static final Stats AKM_BASE = WeaponBase.AKM.baseStats();

    @Test
    void everyAttachmentChangesExactlyTheStatsItsModifiersName() {
        assertEquals(22, Attachment.ALL.size(), "the 1.0 roster ships exactly 22 attachments (ATTACH-DEC-001)");
        for (Attachment attachment : Attachment.ALL) {
            Loadout loadout = Loadout.bare(WeaponBase.AKM).with(attachment);
            Stats derived = StatDerivation.stats(loadout, NO_CLAMP);

            Set<Stat> touched = EnumSet.noneOf(Stat.class);
            attachment.modifiers().forEach(m -> touched.add(m.stat()));

            for (Stat stat : Stat.values()) {
                double baseValue = valueOf(AKM_BASE, stat);
                double derivedValue = valueOf(derived, stat);
                if (touched.contains(stat)) {
                    assertTrue(derivedValue != baseValue,
                        attachment.id() + " names " + stat + " but left it at " + baseValue);
                } else {
                    assertEquals(baseValue, derivedValue, 1e-9,
                        attachment.id() + " changed " + stat + " (" + baseValue + " -> " + derivedValue + ") without naming it");
                }
            }

            if (attachment.slot() == Slot.OPTIC) {
                assertEquals(attachment.zoom(), derived.zoom(), 1e-9, attachment.id() + "'s own zoom must be read onto the final stats");
            } else {
                assertEquals(1.0, derived.zoom(), 1e-9, "a non-optic attachment must never change zoom");
            }
        }
    }

    private static double valueOf(Stats stats, Stat stat) {
        return switch (stat) {
            case DAMAGE -> stats.damage();
            case MUZZLE_VELOCITY -> stats.muzzleVelocity();
            case SPREAD -> stats.spread();
            case SPREAD_WHILE_AIMING -> stats.spreadWhileAiming();
            case FIRE_RATE_TICKS -> stats.fireRateTicks();
            case MAGAZINE_SIZE -> stats.magazineSize();
            case RELOAD_TICKS -> stats.reloadTicks();
            case RECOIL_VERTICAL -> stats.recoilVertical();
            case RECOIL_HORIZONTAL -> stats.recoilHorizontal();
            case DURABILITY -> stats.durability();
            case PELLETS -> stats.pellets();
        };
    }
}
