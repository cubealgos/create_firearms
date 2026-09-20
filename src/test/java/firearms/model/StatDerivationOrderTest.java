package firearms.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

/**
 * `docs/spec/domains/weapon.md` `WEAPON-REQ-003`: application order is fixed (muzzle, optic,
 * magazine, grip, stock), not per-call-site. Every 1.0 attachment modifier is {@code MULTIPLY}
 * (`docs/spec/domains/attach.md` §3), and multiplication alone can never demonstrate order
 * sensitivity — so this proves it with a synthetic {@code ADD} attachment ahead of a synthetic
 * {@code MULTIPLY} one, and separately proves a full five-slot loadout folds every present
 * attachment correctly regardless of the order they were {@link Loadout#with} onto the build.
 */
final class StatDerivationOrderTest {
    private static final Consumer<String> NO_CLAMP = message -> {
        throw new AssertionError("unexpected clamp: " + message);
    };

    @Test
    void anAddBeforeAMultiplyOnDifferentSlotsProvesFixedSlotOrderNotInsertionOrder() {
        // Synthetic, not one of the 22 — proving Op.ADD's own order-sensitivity purpose (Op's Javadoc).
        Attachment addTenDamage = new Attachment("test_add_damage", Slot.MUZZLE, List.of(Modifier.add(Stat.DAMAGE, 10.0)), 1.0);
        Attachment timesThreeDamage = new Attachment("test_times_three_damage", Slot.MAGAZINE, List.of(Modifier.multiply(Stat.DAMAGE, 3.0)), 1.0);

        // Fixed slot order is muzzle before magazine (Slot's own declaration order), so the spec-correct
        // result is (base + 10) * 3, regardless of which .with(...) call happened first.
        double base = WeaponBase.M1911.baseStats().damage();
        double correctOrder = (base + 10.0) * 3.0;
        double wrongOrder = base * 3.0 + 10.0;
        assertNotEquals(correctOrder, wrongOrder, "the two orders must disagree, or this test proves nothing");

        Loadout insertedMuzzleFirst = Loadout.bare(WeaponBase.M1911).with(addTenDamage).with(timesThreeDamage);
        Loadout insertedMagazineFirst = Loadout.bare(WeaponBase.M1911).with(timesThreeDamage).with(addTenDamage);

        assertEquals(correctOrder, StatDerivation.stats(insertedMuzzleFirst, NO_CLAMP).damage(), 1e-9);
        assertEquals(correctOrder, StatDerivation.stats(insertedMagazineFirst, NO_CLAMP).damage(), 1e-9,
            "insertion order must not matter: StatDerivation always folds in Slot's fixed order");
    }

    @Test
    void aFullFiveSlotLoadoutFoldsEveryPresentAttachment() {
        Loadout loadout = Loadout.bare(WeaponBase.AKM)
            .with(Attachment.SUPPRESSOR)
            .with(Attachment.SCOPE_4X)
            .with(Attachment.EXTENDED_MAGAZINE)
            .with(Attachment.LIGHT_GRIP)
            .with(Attachment.TACTICAL_STOCK);

        Stats stats = StatDerivation.stats(loadout, NO_CLAMP);

        assertEquals(4, stats.damage(), 1e-9, "no present attachment names damage");
        assertEquals(6.0 * 0.97, stats.muzzleVelocity(), 1e-9, "suppressor");
        assertEquals(3.5 * 0.90, stats.spread(), 1e-9, "suppressor");
        assertEquals(3.5 * 0.55, stats.spreadWhileAiming(), 1e-9, "scope 4x");
        assertEquals(4.0, stats.zoom(), 1e-9, "scope 4x");
        assertEquals((int) Math.floor(4 * 0.95), stats.fireRateTicks(), "light grip");
        assertEquals(FireMode.AUTO, stats.fireMode());
        assertEquals((int) Math.floor(30 * 1.5), stats.magazineSize(), "extended magazine");
        assertEquals((int) Math.floor(50 * 1.1), stats.reloadTicks(), "extended magazine");
        assertEquals(2.5 * 0.85, stats.recoilVertical(), 1e-9, "tactical stock");
        assertEquals(0.0, stats.recoilHorizontal(), 1e-9, "no 1.0 attachment touches horizontal recoil");
        assertEquals(400, stats.durability(), "no present attachment names durability");
        assertEquals(1, stats.pellets(), "no present attachment names pellets");
    }
}
