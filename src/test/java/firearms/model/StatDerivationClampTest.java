package firearms.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The out-of-range clamp-and-log path a datapack's own weapon/attachment data can trigger
 * (`docs/spec/contracts/public-surface.md` `SURFACE-REQ-002`): a negative magazine size or a
 * zero-or-below fire-rate divide clamps to its nearest valid value and logs exactly once per
 * clamped field, rather than crashing the derivation.
 */
final class StatDerivationClampTest {

    @Test
    void aNegativeMagazineSizeClampsToOneAndLogsOnce() {
        // A datapack-shaped attachment with an out-of-range multiplier, not one of the 22.
        Attachment breaksMagazine = new Attachment("test_negative_magazine", Slot.MAGAZINE, List.of(Modifier.multiply(Stat.MAGAZINE_SIZE, -1.0)), 1.0);
        Loadout loadout = Loadout.bare(WeaponBase.M1911).with(breaksMagazine);

        List<String> messages = new ArrayList<>();
        Stats stats = StatDerivation.stats(loadout, messages::add);

        assertEquals(1, stats.magazineSize(), "magazine size must clamp to its minimum of 1");
        assertEquals(1, messages.size(), "exactly one message for the one clamped field");
        assertTrue(messages.get(0).contains("magazineSize"), messages.get(0));
    }

    @Test
    void aZeroFireRateClampsToOneAndLogsOnce() {
        Attachment breaksFireRate = new Attachment("test_zero_fire_rate", Slot.GRIP, List.of(Modifier.multiply(Stat.FIRE_RATE_TICKS, 0.0)), 1.0);
        Loadout loadout = Loadout.bare(WeaponBase.AKM).with(breaksFireRate);

        List<String> messages = new ArrayList<>();
        Stats stats = StatDerivation.stats(loadout, messages::add);

        assertEquals(1, stats.fireRateTicks(), "fire rate ticks must clamp to its minimum of 1, never a zero divide");
        assertEquals(1, messages.size(), "exactly one message for the one clamped field");
        assertTrue(messages.get(0).contains("fireRateTicks"), messages.get(0));
    }

    @Test
    void twoSimultaneousOutOfRangeFieldsEachLogExactlyOnce() {
        Attachment breaksMagazine = new Attachment("test_negative_magazine_2", Slot.MAGAZINE, List.of(Modifier.multiply(Stat.MAGAZINE_SIZE, -5.0)), 1.0);
        Attachment breaksReload = new Attachment("test_negative_reload", Slot.GRIP, List.of(Modifier.multiply(Stat.RELOAD_TICKS, -1.0)), 1.0);
        Loadout loadout = Loadout.bare(WeaponBase.AKM).with(breaksMagazine).with(breaksReload);

        List<String> messages = new ArrayList<>();
        Stats stats = StatDerivation.stats(loadout, messages::add);

        assertEquals(1, stats.magazineSize());
        assertEquals(0, stats.reloadTicks());
        assertEquals(2, messages.size(), "one message per clamped field, exactly once per derivation");
        assertTrue(messages.stream().anyMatch(m -> m.contains("magazineSize")), messages.toString());
        assertTrue(messages.stream().anyMatch(m -> m.contains("reloadTicks")), messages.toString());
    }

    @Test
    void noClampMessageWhenEveryFieldIsInRange() {
        List<String> messages = new ArrayList<>();
        StatDerivation.stats(Loadout.bare(WeaponBase.AKM).with(Attachment.SUPPRESSOR), messages::add);
        assertTrue(messages.isEmpty(), "a legitimate 1.0 attachment must never clamp");
    }
}
