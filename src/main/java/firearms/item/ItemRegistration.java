package firearms.item;

import firearms.Firearms;
import firearms.model.Caliber;
import firearms.model.Slot;
import firearms.support.Ids;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/**
 * Every item this ticket registers: the one weapon item, the five attachment items (one per
 * slot), and the six cartridge items (one per calibre, `docs/spec/domains/ammo.md`
 * {@code AMMO-REQ-002}, {@code 003}). None carries {@code .durability(int)},
 * {@code .repairable(...)} or {@code .enchantable(...)} at the {@link Item.Properties} level —
 * durability differs per base weapon, so each base's own crafting recipe sets {@code
 * minecraft:max_damage}/{@code minecraft:damage}/{@code minecraft:max_stack_size} directly via its
 * output {@code components} patch instead (`docs/spec/domains/weapon.md` {@code WEAPON-REQ-014},
 * {@code 015}; `docs/spec/decisions/DEC-017-no-detach-durability.md`); omission alone, not a
 * mixin, keeps every weapon unrepairable at an anvil and unenchantable.
 */
public final class ItemRegistration {

    /** {@code firearms:weapon} — one item; which base it is lives entirely in its own {@code firearms:base} component. */
    public static final Item WEAPON = Registry.register(BuiltInRegistries.ITEM, key("weapon"),
        new WeaponItem(new Item.Properties().setId(key("weapon")).stacksTo(1)));

    private static final Map<Slot, AttachmentItem> ATTACHMENTS = registerAttachments();
    private static final Map<Caliber, Item> CARTRIDGES = registerCartridges();

    /** The one attachment item for {@code slot}; which specific attachment a stack is lives in its own slot component. */
    public static Item attachment(Slot slot) {
        return ATTACHMENTS.get(slot);
    }

    /** The one cartridge item for {@code caliber} (`AMMO-REQ-003`: identified purely by item id, no component). */
    public static Item cartridge(Caliber caliber) {
        return CARTRIDGES.get(caliber);
    }

    private static Map<Slot, AttachmentItem> registerAttachments() {
        EnumMap<Slot, AttachmentItem> map = new EnumMap<>(Slot.class);
        for (Slot slot : Slot.values()) {
            String path = "attachment_" + Ids.slug(slot);
            ResourceKey<Item> key = key(path);
            AttachmentItem item = Registry.register(BuiltInRegistries.ITEM, key,
                new AttachmentItem(slot, new Item.Properties().setId(key).stacksTo(64)));
            map.put(slot, item);
        }
        return Map.copyOf(map);
    }

    private static Map<Caliber, Item> registerCartridges() {
        EnumMap<Caliber, Item> map = new EnumMap<>(Caliber.class);
        for (Caliber caliber : Caliber.values()) {
            String path = "cartridge_" + Ids.slug(caliber);
            ResourceKey<Item> key = key(path);
            Item item = Registry.register(BuiltInRegistries.ITEM, key,
                new Item(new Item.Properties().setId(key).stacksTo(64)));
            map.put(caliber, item);
        }
        return Map.copyOf(map);
    }

    private static ResourceKey<Item> key(String path) {
        return ResourceKey.create(Registries.ITEM, Firearms.id(path));
    }

    /** Forces this class's static registrations to run; call once from {@link Firearms#onInitialize()}. */
    public static void register() {
    }

    private ItemRegistration() {
    }
}
