package firearms.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import firearms.component.Ammo;
import firearms.component.Base;
import firearms.component.BaseCodec;
import firearms.component.ComponentRegistration;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

/**
 * All seven of this mod's own {@code DataComponentType}s round-trip through their real registered
 * codec, and a malformed value decodes to a graceful {@code DataResult} error rather than throwing
 * — the precondition {@code docs/spec/contracts/data-contract.md} {@code DATA-REQ-004} relies on:
 * an item's own component-map deserialization drops exactly the one component whose codec errors,
 * degrading that slot to absent (or ammo to "no ammo loaded") instead of failing the whole item.
 */
public final class ComponentCodecGameTest {

    @GameTest
    public void baseRoundTripsAndMigratesAndDegrades(GameTestHelper helper) {
        Codec<Base> codec = ComponentRegistration.BASE.codec();
        Identifier m1911 = Identifier.fromNamespaceAndPath("firearms", "m1911");

        Base written = new Base(1, m1911);
        JsonElement encoded = codec.encodeStart(JsonOps.INSTANCE, written).getOrThrow();
        Base decoded = decode(codec, encoded);
        helper.assertTrue(decoded.equals(written), "firearms:base must round-trip: " + written + " -> " + decoded);

        JsonObject unversioned = new JsonObject();
        unversioned.add("weapon_id", new JsonPrimitive("firearms:m1911"));
        Base defaulted = decode(codec, unversioned);
        helper.assertValueEqual(defaulted.version(), 1, "an unversioned firearms:base defaults to version 1 (DATA-REQ-001)");

        JsonObject older = new JsonObject();
        older.add("version", new JsonPrimitive(0));
        older.add("weapon_id", new JsonPrimitive("firearms:m1911"));
        Base migrated = decode(codec, older);
        helper.assertValueEqual(migrated.version(), BaseCodec.VERSION,
            "a version older than current must migrate forward to the current schema on read (DATA-REQ-002)");
        helper.assertFalse(migrated.readOnly(), "a migrated-forward base must not report read-only");

        JsonObject newer = new JsonObject();
        newer.add("version", new JsonPrimitive(999));
        newer.add("weapon_id", new JsonPrimitive("firearms:m1911"));
        Base fromTheFuture = decode(codec, newer);
        helper.assertValueEqual(fromTheFuture.version(), 999, "a newer version is kept exactly as saved (DATA-REQ-003)");
        helper.assertTrue(fromTheFuture.readOnly(), "a newer version reports read-only (DATA-REQ-003)");

        JsonObject malformed = new JsonObject();
        malformed.add("weapon_id", new JsonPrimitive("not a valid identifier!!"));
        helper.assertTrue(codec.decode(JsonOps.INSTANCE, malformed).isError(), "a malformed weapon_id must fail to decode, not throw");

        helper.succeed();
    }

    @GameTest
    public void ammoRoundTripsAndDegrades(GameTestHelper helper) {
        Codec<Ammo> codec = ComponentRegistration.AMMO.codec();
        Ammo written = new Ammo(Identifier.fromNamespaceAndPath("firearms", "acp_45"), 7);
        JsonElement encoded = codec.encodeStart(JsonOps.INSTANCE, written).getOrThrow();
        Ammo decoded = decode(codec, encoded);
        helper.assertTrue(decoded.equals(written), "firearms:ammo must round-trip: " + written + " -> " + decoded);

        JsonObject negativeLoaded = new JsonObject();
        negativeLoaded.add("caliber", new JsonPrimitive("firearms:acp_45"));
        negativeLoaded.add("loaded", new JsonPrimitive(-1));
        helper.assertTrue(codec.decode(JsonOps.INSTANCE, negativeLoaded).isError(),
            "a negative loaded count must fail to decode, degrading to no ammo loaded (DATA-REQ-004)");

        JsonObject badCaliber = new JsonObject();
        badCaliber.add("caliber", new JsonPrimitive("not a valid identifier!!"));
        badCaliber.add("loaded", new JsonPrimitive(1));
        helper.assertTrue(codec.decode(JsonOps.INSTANCE, badCaliber).isError(), "a malformed caliber must fail to decode, not throw");

        helper.succeed();
    }

    @GameTest
    public void everyAttachmentSlotComponentRoundTripsAndDegrades(GameTestHelper helper) {
        checkSlotComponent(helper, ComponentRegistration.ATTACHMENT_MUZZLE.codec(), "suppressor");
        checkSlotComponent(helper, ComponentRegistration.ATTACHMENT_OPTIC.codec(), "scope_4x");
        checkSlotComponent(helper, ComponentRegistration.ATTACHMENT_MAGAZINE.codec(), "extended_magazine");
        checkSlotComponent(helper, ComponentRegistration.ATTACHMENT_GRIP.codec(), "vertical_grip");
        checkSlotComponent(helper, ComponentRegistration.ATTACHMENT_STOCK.codec(), "tactical_stock");
        helper.succeed();
    }

    private static void checkSlotComponent(GameTestHelper helper, Codec<Identifier> codec, String attachmentPath) {
        Identifier written = Identifier.fromNamespaceAndPath("firearms", attachmentPath);
        JsonElement encoded = codec.encodeStart(JsonOps.INSTANCE, written).getOrThrow();
        Identifier decoded = decode(codec, encoded);
        helper.assertTrue(decoded.equals(written), "an attachment slot component must round-trip: " + written + " -> " + decoded);

        DataResult<Pair<Identifier, JsonElement>> malformed = codec.decode(JsonOps.INSTANCE, new JsonPrimitive("not a valid identifier!!"));
        helper.assertTrue(malformed.isError(), "a malformed attachment id must fail to decode, degrading the slot to empty (DATA-REQ-004)");
    }

    private static <A> A decode(Codec<A> codec, JsonElement json) {
        return codec.decode(JsonOps.INSTANCE, json).getOrThrow().getFirst();
    }
}
