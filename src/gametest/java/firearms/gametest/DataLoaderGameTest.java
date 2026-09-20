package firearms.gametest;

import firearms.Firearms;
import firearms.data.AttachmentRegistry;
import firearms.data.WeaponRegistry;
import firearms.model.Attachment;
import firearms.model.Loadout;
import firearms.model.Slot;
import firearms.model.StatDerivation;
import firearms.model.WeaponBase;
import firearms.model.WeaponClass;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

/**
 * The two data loaders (`WeaponDataLoader`, `AttachmentDataLoader`) produce exactly this mod's own
 * 6 + 22 entries, with the model's own values (`docs/spec/domains/weapon.md` `WEAPON-DEC-003`) —
 * this mod's own shipped {@code data/firearms/weapon/*.json} and
 * {@code data/firearms/attachment/*.json} files ship the same numbers those constants do — and a
 * weapon or attachment shipped by an entirely different namespace (the shape any real datapack
 * addition takes) loads through the identical, unmodified loader with zero new Java
 * (`SURFACE-REQ-003`; the gametest source set's own {@code data/datapack_test/} files stand in for
 * a third party's datapack).
 */
public final class DataLoaderGameTest {

    private static final List<WeaponBase> WEAPONS = List.of(
        WeaponBase.M1911, WeaponBase.MICRO_UZI, WeaponBase.AKM,
        WeaponBase.RUGER_MINI_14, WeaponBase.AWM, WeaponBase.WINCHESTER_MODEL_1897);

    @GameTest
    public void theWeaponLoaderProducesExactlyTheSixBasesWithTheModelsValues(GameTestHelper helper) {
        Map<Identifier, WeaponBase> loaded = onlyNamespace(WeaponRegistry.all(), Firearms.MOD_ID);
        helper.assertValueEqual(loaded.size(), WEAPONS.size(), "exactly 6 weapon data files must load under firearms:");
        for (WeaponBase expected : WEAPONS) {
            WeaponBase actual = loaded.get(Firearms.id(expected.id()));
            helper.assertTrue(actual != null, expected.id() + " must be loaded");
            helper.assertTrue(expected.equals(actual), expected.id() + " must match firearms.model.WeaponBase's own constant: "
                + expected + " vs loaded " + actual);
        }
        helper.succeed();
    }

    @GameTest
    public void theAttachmentLoaderProducesExactlyThe22AttachmentsWithTheModelsValues(GameTestHelper helper) {
        Map<Identifier, Attachment> loaded = onlyNamespace(AttachmentRegistry.all(), Firearms.MOD_ID);
        helper.assertValueEqual(loaded.size(), Attachment.ALL.size(), "exactly 22 attachment data files must load under firearms:");
        for (Attachment expected : Attachment.ALL) {
            Attachment actual = loaded.get(Firearms.id(expected.id()));
            helper.assertTrue(actual != null, expected.id() + " must be loaded");
            helper.assertTrue(expected.equals(actual), expected.id() + " must match firearms.model.Attachment's own constant: "
                + expected + " vs loaded " + actual);
        }
        helper.succeed();
    }

    @GameTest
    public void aDatapackAddedWeaponAndAttachmentInAnExistingClassAndSlotLoadWithZeroNewJava(GameTestHelper helper) {
        Identifier weaponId = Identifier.fromNamespaceAndPath("datapack_test", "example_smg");
        Optional<WeaponBase> weapon = WeaponRegistry.get(weaponId);
        helper.assertTrue(weapon.isPresent(), "a weapon shipped by an unrelated namespace must load through the same loader");
        helper.assertTrue(weapon.get().weaponClass() == WeaponClass.SMG,
            "the datapack example must use the existing SMG class, not a new one");
        // Zero new Java: the same StatDerivation call every real weapon goes through, unmodified.
        StatDerivation.stats(Loadout.bare(weapon.get()), message -> { throw new AssertionError(message); });

        Identifier attachmentId = Identifier.fromNamespaceAndPath("datapack_test", "example_gadget");
        Optional<Attachment> attachment = AttachmentRegistry.get(attachmentId);
        helper.assertTrue(attachment.isPresent(), "an attachment shipped by an unrelated namespace must load through the same loader");
        helper.assertTrue(attachment.get().slot() == Slot.GRIP, "the datapack example must use the existing grip slot");
        Loadout.bare(weapon.get()).with(attachment.get());

        helper.succeed();
    }

    private static <T> Map<Identifier, T> onlyNamespace(Map<Identifier, T> all, String namespace) {
        return all.entrySet().stream()
            .filter(e -> e.getKey().getNamespace().equals(namespace))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
