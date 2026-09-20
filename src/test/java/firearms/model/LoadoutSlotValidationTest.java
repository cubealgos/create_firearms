package firearms.model;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * An attachment on a class whose slot set lacks its own slot is rejected
 * (`docs/spec/domains/attach.md` `ATTACH-FAIL-001`): the pistol has no grip or stock, and the
 * shotgun has neither optic, grip nor stock.
 */
final class LoadoutSlotValidationTest {
    @Test
    void anOpticOnAShotgunIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Loadout.bare(WeaponBase.WINCHESTER_MODEL_1897).with(Attachment.RED_DOT));
    }

    @Test
    void aGripOnAShotgunIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Loadout.bare(WeaponBase.WINCHESTER_MODEL_1897).with(Attachment.VERTICAL_GRIP));
    }

    @Test
    void aStockOnAShotgunIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Loadout.bare(WeaponBase.WINCHESTER_MODEL_1897).with(Attachment.TACTICAL_STOCK));
    }

    @Test
    void aGripOnAPistolIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Loadout.bare(WeaponBase.M1911).with(Attachment.VERTICAL_GRIP));
    }

    @Test
    void aStockOnAPistolIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Loadout.bare(WeaponBase.M1911).with(Attachment.TACTICAL_STOCK));
    }

    @Test
    void aGripOnADmrIsRejected() {
        // DMR and sniper rifle both have muzzle/optic/magazine/stock, no grip (docs/spec/domains/weapon.md §3).
        assertThrows(IllegalArgumentException.class, () -> Loadout.bare(WeaponBase.RUGER_MINI_14).with(Attachment.VERTICAL_GRIP));
    }

    @Test
    void aMuzzleAttachmentFitsEveryClassSinceEveryClassHasAMuzzleSlot() {
        // Universal by slot, not by class (docs/spec/domains/attach.md §3, ATTACH-DEC-001): this must never throw.
        Loadout.bare(WeaponBase.M1911).with(Attachment.SUPPRESSOR);
        Loadout.bare(WeaponBase.MICRO_UZI).with(Attachment.SUPPRESSOR);
        Loadout.bare(WeaponBase.AKM).with(Attachment.SUPPRESSOR);
        Loadout.bare(WeaponBase.RUGER_MINI_14).with(Attachment.SUPPRESSOR);
        Loadout.bare(WeaponBase.AWM).with(Attachment.SUPPRESSOR);
        Loadout.bare(WeaponBase.WINCHESTER_MODEL_1897).with(Attachment.SUPPRESSOR);
    }
}
