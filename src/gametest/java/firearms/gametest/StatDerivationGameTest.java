package firearms.gametest;

import firearms.Firearms;
import firearms.component.Base;
import firearms.component.ComponentRegistration;
import firearms.data.WeaponRegistry;
import firearms.item.ItemRegistration;
import firearms.model.Loadout;
import firearms.model.StatDerivation;
import firearms.model.Stats;
import firearms.model.WeaponBase;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

/**
 * A weapon stack carrying a {@code firearms:base} component reads its own base weapon back out of
 * {@link WeaponRegistry} and derives its final stats through {@code StatDerivation}, matching the
 * bare roster row exactly (`docs/spec/domains/weapon.md` `WEAPON-REQ-003`).
 */
public final class StatDerivationGameTest {

    @GameTest
    public void aWeaponStackReadsItsBaseAndDerivesStats(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ItemRegistration.WEAPON);
        stack.set(ComponentRegistration.BASE, Base.of(Firearms.id("m1911")));

        Base base = stack.get(ComponentRegistration.BASE);
        helper.assertTrue(base != null, "the crafted stack must carry firearms:base");

        Optional<WeaponBase> resolved = WeaponRegistry.get(base.weaponId());
        helper.assertTrue(resolved.isPresent(), base.weaponId() + " must resolve in the loaded weapon registry");

        Stats derived = StatDerivation.stats(Loadout.bare(resolved.get()),
            message -> { throw new AssertionError("unexpected clamp: " + message); });
        helper.assertTrue(derived.equals(WeaponBase.M1911.baseStats()),
            "a bare M1911 stack must derive to the roster row: " + WeaponBase.M1911.baseStats() + " vs " + derived);

        helper.succeed();
    }
}
