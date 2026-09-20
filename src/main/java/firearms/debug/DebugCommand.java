package firearms.debug;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import firearms.Firearms;
import firearms.attach.Attach;
import firearms.component.Ammo;
import firearms.component.Base;
import firearms.component.ComponentRegistration;
import firearms.data.AttachmentRegistry;
import firearms.data.WeaponRegistry;
import firearms.item.WeaponStacks;
import firearms.model.Attachment;
import firearms.model.Loadout;
import firearms.model.Slot;
import firearms.model.Stats;
import firearms.model.StatDerivation;
import firearms.model.WeaponBase;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Development-only: {@code /firearms debug <give|stats> <...>} (`docs/spec/domains/ui.md`
 * `UI-REQ-006`, `docs/spec/operations/testing.md`'s "Development tool" row, `FA-12`), mirroring
 * every sibling mod's own debug command shape (`create_metered_motor`'s `MM-8`,
 * `create_villager_customers`'s `VC-5`, `villager_voices`'s `VV-13`).
 *
 * <p>{@code give <base> [attachment...]} bypasses the smithing table and the deployer entirely for
 * fast iteration: it builds a bare {@code firearms:weapon} stack of {@code base}, then attaches
 * each named attachment in the order given, one call to the real {@link Attach} per attachment —
 * the same shared function both front ends call (`ATTACH-REQ-008`) — refusing with a translated
 * error naming the first attachment {@link Attach#matches} rejects (its slot missing from the
 * base's class, or already filled by an earlier attachment). The resulting stack is loaded to
 * capacity with its own calibre, {@code loaded} read off the derived magazine size so an attached
 * extended magazine already counts.
 *
 * <p>{@code stats} prints the caller's held main-hand weapon's base, every slot's attachment or
 * "empty", its ammo state, and its derived final stats one line per field, all through {@link
 * StatDerivation} — the same pure function the tooltip and firing both call — for verification
 * without hovering (`UI-REQ-006`). Refuses when the main hand carries no {@code firearms:weapon}
 * (`command.firearms.debug.stats.no_item`); there is no argument to name a different stack at
 * 1.0.
 *
 * <p>Registered only when Fabric reports a development environment (the one guarded line in
 * {@link Firearms#onInitialize()}); never present in a released jar.
 */
public final class DebugCommand {
    private static final SimpleCommandExceptionType NO_ITEM_HELD =
        new SimpleCommandExceptionType(Component.translatable("command.firearms.debug.stats.no_item"));
    private static final DynamicCommandExceptionType UNKNOWN_BASE = new DynamicCommandExceptionType(
        id -> Component.translatable("command.firearms.debug.give.unknown_base", id));
    private static final DynamicCommandExceptionType UNKNOWN_ATTACHMENT = new DynamicCommandExceptionType(
        id -> Component.translatable("command.firearms.debug.give.unknown_attachment", id));
    private static final DynamicCommandExceptionType CANNOT_ATTACH = new DynamicCommandExceptionType(
        id -> Component.translatable("command.firearms.debug.give.cannot_attach", id));

    private DebugCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> dispatcher.register(
            Commands.literal(Firearms.MOD_ID).then(Commands.literal("debug")
                .then(Commands.literal("give")
                    .then(Commands.argument("base", StringArgumentType.word()).suggests(DebugCommand::suggestBases)
                        .executes(c -> give(c.getSource(), StringArgumentType.getString(c, "base"), List.of()))
                        .then(Commands.argument("attachments", StringArgumentType.greedyString())
                            .suggests(DebugCommand::suggestAttachments)
                            .executes(c -> give(
                                c.getSource(), StringArgumentType.getString(c, "base"),
                                splitWords(StringArgumentType.getString(c, "attachments")))))))
                .then(Commands.literal("stats")
                    .executes(c -> stats(c.getSource()))))));
    }

    /**
     * Builds a bare {@code base} weapon, attaches every one of {@code attachmentWords} in order
     * through the real {@link Attach}, loads it to its own derived magazine capacity with its own
     * calibre, and gives it to the caller.
     */
    static int give(CommandSourceStack source, String baseWord, List<String> attachmentWords) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();

        Identifier baseId = Firearms.id(baseWord);
        WeaponBase weaponBase = WeaponRegistry.get(baseId).orElseThrow(() -> UNKNOWN_BASE.create(baseWord));

        ItemStack stack = WeaponStacks.bare(baseId, weaponBase);

        Loadout loadout = Loadout.bare(weaponBase);
        for (String word : attachmentWords) {
            Identifier attachmentId = Firearms.id(word);
            Attachment attachment = AttachmentRegistry.get(attachmentId).orElseThrow(() -> UNKNOWN_ATTACHMENT.create(word));

            Optional<ItemStack> attached = WeaponStacks.attach(stack, attachmentId, attachment);
            if (attached.isEmpty()) {
                throw CANNOT_ATTACH.create(word);
            }
            stack = attached.get();
            loadout = loadout.with(attachment);
        }

        stack = WeaponStacks.loaded(stack, weaponBase, loadout);

        ItemStack given = stack.copy(); // Inventory.add drains the stack it is handed; keep the name (FA-23)
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }

        source.sendSuccess(() -> Component.translatable(
            "command.firearms.debug.give.done", given.getHoverName(), attachmentWords.size()), false);
        return 1;
    }

    /**
     * Prints the held main-hand item's base, every slot's attachment or "empty", its ammo state,
     * and its derived stats, one line per field (`UI-REQ-006`) — plain literal text, not translated
     * keys, since every line's content is inherently dynamic (`create_villager_customers`'s own
     * {@code box}/{@code shop} precedent).
     */
    static int stats(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack held = player.getMainHandItem();
        Base base = held.get(ComponentRegistration.BASE);
        if (base == null) {
            throw NO_ITEM_HELD.create();
        }
        WeaponBase weaponBase = WeaponRegistry.get(base.weaponId()).orElseThrow(() -> UNKNOWN_BASE.create(base.weaponId().toString()));

        print(source, "base: " + base.weaponId());

        Loadout loadout = Loadout.bare(weaponBase);
        for (Slot slot : Slot.values()) {
            Identifier attachmentId = held.get(ComponentRegistration.attachmentComponent(slot));
            print(source, slot.name().toLowerCase(Locale.ROOT) + ": " + (attachmentId == null ? "empty" : attachmentId.toString()));
            if (attachmentId != null) {
                Attachment attachment = AttachmentRegistry.get(attachmentId).orElse(null);
                if (attachment != null) {
                    loadout = loadout.with(attachment);
                }
            }
        }

        Ammo ammo = held.get(ComponentRegistration.AMMO);
        print(source, ammo == null ? "ammo: none loaded" : "ammo: " + ammo.loaded() + " x " + ammo.caliber());

        Stats stats = StatDerivation.stats(loadout, message -> print(source, "clamped: " + message));
        print(source, "damage: " + stats.damage());
        print(source, "muzzle_velocity: " + stats.muzzleVelocity());
        print(source, "spread: " + stats.spread());
        print(source, "spread_while_aiming: " + stats.spreadWhileAiming());
        print(source, "fire_rate_ticks: " + stats.fireRateTicks());
        print(source, "fire_mode: " + stats.fireMode());
        print(source, "magazine_size: " + stats.magazineSize());
        print(source, "reload_ticks: " + stats.reloadTicks());
        print(source, "recoil_vertical: " + stats.recoilVertical());
        print(source, "recoil_horizontal: " + stats.recoilHorizontal());
        print(source, "durability: " + stats.durability());
        print(source, "pellets: " + stats.pellets());
        print(source, "zoom: " + stats.zoom());
        return 1;
    }

    private static void print(CommandSourceStack source, String line) {
        source.sendSuccess(() -> Component.literal(line), false);
    }

    /** {@code attachments}, split on whitespace, blank tokens dropped — empty for a blank argument. */
    private static List<String> splitWords(String attachments) {
        List<String> words = new ArrayList<>();
        for (String word : attachments.trim().split("\\s+")) {
            if (!word.isEmpty()) {
                words.add(word);
            }
        }
        return words;
    }

    private static CompletableFuture<Suggestions> suggestBases(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(WeaponRegistry.all().keySet().stream().map(Identifier::getPath), builder);
    }

    /** Suggests only the last, still-being-typed attachment id — the earlier ones in {@code builder}'s remaining text are left alone. */
    private static CompletableFuture<Suggestions> suggestAttachments(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining();
        int lastSpace = remaining.lastIndexOf(' ');
        SuggestionsBuilder tail = builder.createOffset(builder.getStart() + lastSpace + 1);
        return SharedSuggestionProvider.suggest(AttachmentRegistry.all().keySet().stream().map(Identifier::getPath), tail);
    }
}
