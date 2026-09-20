package firearms.item;

import firearms.Firearms;
import firearms.component.ComponentRegistration;
import firearms.data.AttachmentRegistry;
import firearms.data.WeaponRegistry;
import firearms.model.Attachment;
import firearms.model.Caliber;
import firearms.model.Loadout;
import firearms.model.Slot;
import firearms.model.WeaponBase;
import firearms.model.WeaponClass;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * The one creative-mode tab {@code firearms:firearms} (`docs/spec/domains/ui.md` `UI-REQ-007`,
 * `docs/spec/decisions/DEC-019-controls.md` §Creative tab), iconed with a bare M1911 and listing,
 * in order: every currently-loaded base weapon bare, one fully loaded example per class, every
 * currently-loaded attachment, and the six cartridges. {@link #displayItems} reads {@link
 * WeaponRegistry} and {@link AttachmentRegistry} lazily, each time the tab is populated (after data
 * load, never captured at registration), so a datapack-added weapon or attachment appears without a
 * mod restart, and degrades gracefully to whichever segments the currently-loaded catalogues
 * support if either one is still empty. Every bare and loaded stack goes through {@link
 * WeaponStacks}, the same construction path {@code firearms.debug.DebugCommand}'s own {@code give}
 * uses.
 */
public final class CreativeTabRegistration {

    /**
     * The class-to-optic mapping `UI-REQ-007`'s own scope names: red dot for the pistol and the
     * SMG, a 4x scope for the two rifle classes (assault rifle, DMR), an 8x scope for the sniper
     * rifle. The shotgun carries no optic slot at all ({@code WeaponClass#SHOTGUN}), so this is
     * never consulted for it.
     */
    private static String opticFor(WeaponClass weaponClass) {
        return switch (weaponClass) {
            case PISTOL, SMG -> "red_dot";
            case ASSAULT_RIFLE, DMR -> "scope_4x";
            case SNIPER_RIFLE -> "scope_8x";
            case SHOTGUN -> throw new IllegalStateException(weaponClass + " has no optic slot");
        };
    }

    /** The one fixed attachment id every loaded example fills {@code slot} with. */
    private static String attachmentFor(Slot slot, WeaponClass weaponClass) {
        return switch (slot) {
            case MUZZLE -> "suppressor";
            case OPTIC -> opticFor(weaponClass);
            case MAGAZINE -> "extended_magazine";
            case GRIP -> "vertical_grip";
            case STOCK -> "tactical_stock";
        };
    }

    public static void register() {
        CreativeModeTab tab = FabricCreativeModeTab.builder()
            .title(Component.translatable("itemGroup.firearms.firearms"))
            .icon(CreativeTabRegistration::icon)
            .displayItems(CreativeTabRegistration::displayItems)
            .build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Firearms.id("firearms"), tab);
    }

    /** A bare M1911, built from the compiled constant so the icon never depends on data having loaded yet. */
    private static ItemStack icon() {
        return WeaponStacks.bare(Firearms.id(WeaponBase.M1911.id()), WeaponBase.M1911);
    }

    private static void displayItems(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        Map<Identifier, WeaponBase> weapons = sorted(WeaponRegistry.all());
        Map<Identifier, Attachment> attachments = sorted(AttachmentRegistry.all());

        for (Map.Entry<Identifier, WeaponBase> entry : weapons.entrySet()) {
            output.accept(WeaponStacks.bare(entry.getKey(), entry.getValue()));
        }

        for (WeaponClass weaponClass : WeaponClass.values()) {
            loadedExample(weaponClass, weapons, attachments).ifPresent(output::accept);
        }

        for (Map.Entry<Identifier, Attachment> entry : attachments.entrySet()) {
            output.accept(attachmentStack(entry.getKey(), entry.getValue()));
        }

        for (Caliber caliber : Caliber.values()) {
            output.accept(new ItemStack(ItemRegistration.cartridge(caliber)));
        }
    }

    /**
     * The representative currently-loaded weapon of {@code weaponClass} — this mod's own (sorted-id
     * order among ties, though only one of its own ever occupies a class at 1.0), falling back to
     * the first datapack-added one of that class if this mod ships none — fully attached: every
     * slot the class has filled with {@link #attachmentFor}'s own choice, loaded to capacity through
     * the real {@link WeaponStacks#attach}/{@link WeaponStacks#loaded}. Empty if no currently-loaded
     * weapon belongs to this class at all, or if one of its fixed attachment picks is not itself
     * currently loaded (both only reachable while the catalogues are still empty, e.g. before the
     * first data reload).
     */
    private static Optional<ItemStack> loadedExample(
            WeaponClass weaponClass, Map<Identifier, WeaponBase> weapons, Map<Identifier, Attachment> attachments) {
        Optional<Map.Entry<Identifier, WeaponBase>> representative = representativeOfClass(weaponClass, weapons);
        if (representative.isEmpty()) {
            return Optional.empty();
        }
        Map.Entry<Identifier, WeaponBase> entry = representative.get();
        ItemStack stack = WeaponStacks.bare(entry.getKey(), entry.getValue());
        Loadout loadout = Loadout.bare(entry.getValue());
        for (Slot slot : weaponClass.slots()) {
            Identifier attachmentId = Firearms.id(attachmentFor(slot, weaponClass));
            Attachment attachment = attachments.get(attachmentId);
            if (attachment == null) {
                continue;
            }
            Optional<ItemStack> attached = WeaponStacks.attach(stack, attachmentId, attachment);
            if (attached.isEmpty()) {
                continue;
            }
            stack = attached.get();
            loadout = loadout.with(attachment);
        }
        return Optional.of(WeaponStacks.loaded(stack, entry.getValue(), loadout));
    }

    /**
     * Among {@code weapons} of {@code weaponClass}, this mod's own entry (first by sorted id, on
     * the off chance a datapack ever ships two), or, failing that, the first datapack-added one —
     * so a real third-party addition still gets an example once this mod's own weapon for a class
     * is removed, while at 1.0, with exactly one weapon per class, this always resolves to that
     * weapon regardless of what else a test or datapack layers in alongside it.
     */
    private static Optional<Map.Entry<Identifier, WeaponBase>> representativeOfClass(
            WeaponClass weaponClass, Map<Identifier, WeaponBase> weapons) {
        Map.Entry<Identifier, WeaponBase> fallback = null;
        for (Map.Entry<Identifier, WeaponBase> entry : weapons.entrySet()) {
            if (entry.getValue().weaponClass() != weaponClass) {
                continue;
            }
            if (entry.getKey().getNamespace().equals(Firearms.MOD_ID)) {
                return Optional.of(entry);
            }
            if (fallback == null) {
                fallback = entry;
            }
        }
        return Optional.ofNullable(fallback);
    }

    private static ItemStack attachmentStack(Identifier attachmentId, Attachment attachment) {
        ItemStack stack = new ItemStack(ItemRegistration.attachment(attachment.slot()));
        stack.set(ComponentRegistration.attachmentComponent(attachment.slot()), attachmentId);
        return stack;
    }

    /** {@code source}, in natural {@link Identifier} order (path, then namespace) — deterministic display order. */
    private static <T> Map<Identifier, T> sorted(Map<Identifier, T> source) {
        return new TreeMap<>(source);
    }

    private CreativeTabRegistration() {
    }
}
