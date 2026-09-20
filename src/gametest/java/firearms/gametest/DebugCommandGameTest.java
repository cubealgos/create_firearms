package firearms.gametest;

import firearms.Firearms;
import firearms.component.Ammo;
import firearms.component.Base;
import firearms.component.ComponentRegistration;
import firearms.data.AttachmentRegistry;
import firearms.data.WeaponRegistry;
import firearms.item.ItemRegistration;
import firearms.model.Attachment;
import firearms.model.Loadout;
import firearms.model.Slot;
import firearms.model.Stats;
import firearms.model.StatDerivation;
import firearms.model.WeaponBase;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;

/**
 * {@code FA-12}'s three acceptance-criteria game tests for {@code firearms.debug.DebugCommand},
 * run in the game test environment, which is itself a development environment
 * ({@code FabricLoader.isDevelopmentEnvironment()} is true under {@code runGameTest}, exactly as
 * it is under {@code runClient}), so the command is registered and reachable here. Registration
 * being gated on that same check -- the one line in {@link Firearms#onInitialize()} -- is
 * otherwise proven by code review, not a game test: there is no development/non-development pair
 * of environments a single test run can compare (the same finding {@code create_metered_motor}'s
 * {@code MM-8}, {@code create_villager_customers}'s {@code VC-5} and {@code villager_voices}'s
 * {@code VV-13} already recorded for their own identically-gated debug commands).
 *
 * <p>Every test runs the real command through {@code MinecraftServer.getCommands()
 * .performPrefixedCommand}, against a {@link CommandSourceStack} built with a capturing
 * {@link CommandSource} so the feedback {@code DebugCommand} sends can be asserted on directly. A
 * {@code give} failure reaches {@link CapturingSource} through {@code sendFailure}, which wraps
 * the original message as a sibling of an empty root component rather than carrying it as its own
 * contents, so {@link CapturingSource#hasKey} walks siblings too (mirroring {@code
 * create_villager_customers}'s and {@code villager_voices}'s own debug game tests). {@code stats}
 * prints plain {@code Component.literal} lines, not translated keys, since its content is
 * inherently dynamic per-stack data (the same reasoning {@code create_villager_customers}'s
 * {@code box}/{@code shop} subcommands use for their own literal output), so those assertions
 * check the rendered text directly through {@link CapturingSource#hasText}.
 */
public final class DebugCommandGameTest {

    @GameTest
    public void giveAkmSuppressorScope4xYieldsBothSlotsFilledWithMatchingDerivedStats(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        CapturingSource capturing = new CapturingSource();
        CommandSourceStack source = sourceFor(helper, capturing, player);

        helper.getLevel().getServer().getCommands().performPrefixedCommand(source, "firearms debug give akm suppressor scope_4x");

        ItemStack given = findGivenWeapon(player, Firearms.id("akm"));
        helper.assertTrue(given != null, "the caller's inventory holds the given AKM: " + capturing.describe());

        Identifier muzzle = given.get(ComponentRegistration.attachmentComponent(Slot.MUZZLE));
        Identifier optic = given.get(ComponentRegistration.attachmentComponent(Slot.OPTIC));
        helper.assertTrue(Firearms.id("suppressor").equals(muzzle), "the muzzle slot carries the suppressor, was " + muzzle);
        helper.assertTrue(Firearms.id("scope_4x").equals(optic), "the optic slot carries the 4x scope, was " + optic);
        helper.assertTrue(
            given.get(ComponentRegistration.attachmentComponent(Slot.MAGAZINE)) == null
                && given.get(ComponentRegistration.attachmentComponent(Slot.GRIP)) == null
                && given.get(ComponentRegistration.attachmentComponent(Slot.STOCK)) == null,
            "every other slot is empty");

        Loadout loadout = Loadout.bare(WeaponBase.AKM).with(Attachment.SUPPRESSOR).with(Attachment.SCOPE_4X);
        Stats expected = StatDerivation.stats(loadout, message -> {
            throw new AssertionError("unexpected clamp: " + message);
        });
        Stats actual = StatDerivation.stats(reconstructLoadout(given, WeaponBase.AKM), message -> {
            throw new AssertionError("unexpected clamp: " + message);
        });
        helper.assertTrue(expected.equals(actual), "the given weapon's derived stats match StatDerivation: " + expected + " vs " + actual);

        helper.succeed();
    }

    @GameTest
    public void giveWinchester1897VerticalGripFailsWithTheErrorKey(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        CapturingSource capturing = new CapturingSource();
        CommandSourceStack source = sourceFor(helper, capturing, player);

        helper.getLevel().getServer().getCommands().performPrefixedCommand(source, "firearms debug give winchester_model_1897 vertical_grip");

        helper.assertTrue(
            capturing.hasKey("command.firearms.debug.give.cannot_attach"),
            "a grip attachment on a shotgun (no grip slot) fails with the cannot-attach key: " + capturing.describe());
        helper.assertTrue(
            findGivenWeapon(player, Firearms.id("winchester_model_1897")) == null,
            "no weapon was given once the attachment refused");

        helper.succeed();
    }

    @GameTest
    public void statsOnAHeldWeaponPrintsTheExpectedLines(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack held = new ItemStack(ItemRegistration.WEAPON);
        held.set(ComponentRegistration.BASE, Base.of(Firearms.id("m1911")));
        held.set(ComponentRegistration.AMMO, new Ammo(Firearms.id("acp_45"), 7));
        player.setItemInHand(InteractionHand.MAIN_HAND, held);

        CapturingSource capturing = new CapturingSource();
        CommandSourceStack source = sourceFor(helper, capturing, player);

        helper.getLevel().getServer().getCommands().performPrefixedCommand(source, "firearms debug stats");

        helper.assertTrue(capturing.hasText("base: firearms:m1911"), "prints the base weapon: " + capturing.describe());
        helper.assertTrue(capturing.hasText("muzzle: empty"), "prints an empty muzzle slot: " + capturing.describe());
        helper.assertTrue(capturing.hasText("optic: empty"), "prints an empty optic slot: " + capturing.describe());
        helper.assertTrue(capturing.hasText("magazine: empty"), "prints an empty magazine slot: " + capturing.describe());
        helper.assertTrue(capturing.hasText("grip: empty"), "prints an empty grip slot: " + capturing.describe());
        helper.assertTrue(capturing.hasText("stock: empty"), "prints an empty stock slot: " + capturing.describe());
        helper.assertTrue(capturing.hasText("ammo: 7 x firearms:acp_45"), "prints the ammo state: " + capturing.describe());
        helper.assertTrue(capturing.hasText("damage: " + WeaponBase.M1911.baseStats().damage()), "prints the derived damage: " + capturing.describe());
        helper.assertTrue(
            capturing.hasText("magazine_size: " + WeaponBase.M1911.baseStats().magazineSize()),
            "prints the derived magazine size: " + capturing.describe());
        helper.assertTrue(capturing.hasText("fire_mode: " + WeaponBase.M1911.baseStats().fireMode()), "prints the derived fire mode: " + capturing.describe());

        helper.succeed();
    }

    /** The first stack in {@code player}'s inventory carrying {@code firearms:weapon} whose base id is {@code weaponId}, or {@code null}. */
    private static ItemStack findGivenWeapon(ServerPlayer player, Identifier weaponId) {
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.getItem() == ItemRegistration.WEAPON) {
                Base base = stack.get(ComponentRegistration.BASE);
                if (base != null && weaponId.equals(base.weaponId())) {
                    return stack;
                }
            }
        }
        return null;
    }

    /** Rebuilds {@code stack}'s own {@link Loadout} off its real, currently-loaded {@link AttachmentRegistry} entries. */
    private static Loadout reconstructLoadout(ItemStack stack, WeaponBase base) {
        Loadout loadout = Loadout.bare(base);
        for (Slot slot : Slot.values()) {
            Identifier attachmentId = stack.get(ComponentRegistration.attachmentComponent(slot));
            if (attachmentId != null) {
                loadout = loadout.with(AttachmentRegistry.get(attachmentId).orElseThrow());
            }
        }
        return loadout;
    }

    private static CommandSourceStack sourceFor(GameTestHelper helper, CapturingSource capturing, ServerPlayer player) {
        return new CommandSourceStack(
            capturing, player.position(), Vec2.ZERO, helper.getLevel(), PermissionSet.ALL_PERMISSIONS, "DebugCommandGameTest",
            Component.literal("DebugCommandGameTest"), helper.getLevel().getServer(), player
        );
    }

    /**
     * A {@link CommandSource} that records every message sent to it instead of delivering it
     * anywhere. {@link #hasKey} walks each message's sibling components too, not just its own
     * contents -- {@code sendFailure} wraps the original message as a sibling of an empty root
     * rather than carrying it directly.
     */
    private static final class CapturingSource implements CommandSource {
        private final List<Component> messages = new ArrayList<>();

        @Override
        public void sendSystemMessage(Component component) {
            messages.add(component);
        }

        @Override
        public boolean acceptsSuccess() {
            return true;
        }

        @Override
        public boolean acceptsFailure() {
            return true;
        }

        @Override
        public boolean shouldInformAdmins() {
            return false;
        }

        boolean hasKey(String key) {
            return messages.stream().anyMatch(m -> containsKey(m, key));
        }

        boolean hasText(String substring) {
            return messages.stream().anyMatch(m -> m.getString().contains(substring));
        }

        private static boolean containsKey(Component component, String key) {
            if (component.getContents() instanceof TranslatableContents t && t.getKey().equals(key)) {
                return true;
            }
            for (Component sibling : component.getSiblings()) {
                if (containsKey(sibling, key)) {
                    return true;
                }
            }
            return false;
        }

        String describe() {
            return messages.stream().map(Component::getString).toList().toString();
        }
    }
}
