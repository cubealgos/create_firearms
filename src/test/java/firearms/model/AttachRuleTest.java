package firearms.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * {@link AttachRule#canAttach} against every {@link WeaponClass} × {@link Slot} combination
 * (`docs/spec/domains/attach.md` `ATTACH-REQ-001`, `002`; `ATTACH-FAIL-001`, `003`): {@code true}
 * only for a slot the class has and that is not already occupied.
 */
final class AttachRuleTest {

    @ParameterizedTest
    @EnumSource(WeaponClass.class)
    void everySlotTheClassHasIsAttachableWhenEmpty(WeaponClass weaponClass) {
        for (Slot slot : weaponClass.slots()) {
            assertTrue(AttachRule.canAttach(weaponClass, slot, false),
                weaponClass + "'s own " + slot + " slot must be attachable when empty");
        }
    }

    @ParameterizedTest
    @EnumSource(WeaponClass.class)
    void everySlotTheClassHasIsNeverAttachableWhenOccupied(WeaponClass weaponClass) {
        for (Slot slot : weaponClass.slots()) {
            assertFalse(AttachRule.canAttach(weaponClass, slot, true),
                weaponClass + "'s own " + slot + " slot must never match once occupied (ATTACH-REQ-002)");
        }
    }

    @ParameterizedTest
    @EnumSource(WeaponClass.class)
    void everySlotTheClassLacksIsNeverAttachableEitherWay(WeaponClass weaponClass) {
        for (Slot slot : EnumSet.complementOf(EnumSet.copyOf(weaponClass.slots()))) {
            assertFalse(AttachRule.canAttach(weaponClass, slot, false),
                weaponClass + " has no " + slot + " slot at all (ATTACH-FAIL-001)");
            assertFalse(AttachRule.canAttach(weaponClass, slot, true),
                weaponClass + " has no " + slot + " slot at all (ATTACH-FAIL-001)");
        }
    }
}
