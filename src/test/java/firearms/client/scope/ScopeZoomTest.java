package firearms.client.scope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import firearms.model.Attachment;
import java.util.OptionalDouble;
import org.junit.jupiter.api.Test;

/**
 * {@link ScopeZoom#of}, the pure mapping the three client mixins share
 * (`docs/spec/domains/combat.md` {@code COMBAT-REQ-006}, {@code COMBAT-DEC-004};
 * `docs/spec/domains/attach.md` §3): red dot and holo never zoom, every magnifying optic (2x
 * through 15x) reports its own factor unchanged, and every non-optic attachment and an empty slot
 * ({@code null}) report none. No Minecraft import needed — `firearms.client.scope` still compiles
 * this one class as a plain unit, independent of the mixins that call it.
 */
final class ScopeZoomTest {

    @Test
    void redDotAndHoloNeverZoom() {
        assertEquals(OptionalDouble.empty(), ScopeZoom.of(Attachment.RED_DOT));
        assertEquals(OptionalDouble.empty(), ScopeZoom.of(Attachment.HOLO));
    }

    @Test
    void everyMagnifyingOpticReportsItsOwnFactor() {
        assertEquals(OptionalDouble.of(2.0), ScopeZoom.of(Attachment.SCOPE_2X));
        assertEquals(OptionalDouble.of(3.0), ScopeZoom.of(Attachment.SCOPE_3X));
        assertEquals(OptionalDouble.of(4.0), ScopeZoom.of(Attachment.SCOPE_4X));
        assertEquals(OptionalDouble.of(6.0), ScopeZoom.of(Attachment.SCOPE_6X));
        assertEquals(OptionalDouble.of(8.0), ScopeZoom.of(Attachment.SCOPE_8X));
        assertEquals(OptionalDouble.of(15.0), ScopeZoom.of(Attachment.SCOPE_15X));
    }

    @Test
    void aNonOpticAttachmentNeverZoomsEvenThoughItsOwnZoomFieldDefaultsToNone() {
        for (Attachment attachment : Attachment.ALL) {
            if (attachment.slot() == firearms.model.Slot.OPTIC) {
                continue;
            }
            assertEquals(OptionalDouble.empty(), ScopeZoom.of(attachment), attachment.id() + " is not an optic and must never zoom");
        }
    }

    @Test
    void anEmptySlotNeverZooms() {
        assertEquals(OptionalDouble.empty(), ScopeZoom.of(null));
    }

    @Test
    void everyOpticInTheRosterAgreesWithItsOwnZoomField() {
        for (Attachment attachment : Attachment.ALL) {
            if (attachment.slot() != firearms.model.Slot.OPTIC) {
                continue;
            }
            OptionalDouble zoom = ScopeZoom.of(attachment);
            if (attachment.zoom() > 1.0) {
                assertTrue(zoom.isPresent(), attachment.id() + " has zoom " + attachment.zoom() + " but ScopeZoom.of reported none");
                assertEquals(attachment.zoom(), zoom.getAsDouble(), 1e-9);
            } else {
                assertEquals(OptionalDouble.empty(), zoom, attachment.id() + " has zoom() <= 1.0 and must report none");
            }
        }
    }
}
