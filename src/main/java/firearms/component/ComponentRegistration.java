package firearms.component;

import firearms.Firearms;
import firearms.model.Slot;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/**
 * The seven {@code DataComponentType} registrations `docs/spec/04-architecture.md`
 * {@code ARCH-DEC-005} defines, the same {@code BuiltInRegistries.DATA_COMPONENT_TYPE} pattern
 * {@code create_metered_motor}'s own {@code MeteredMotor.STATS} uses. Split per slot rather than
 * one combined attachment map, for the two engine reasons {@code ARCH-DEC-005} names: the
 * zero-mixin item-model path needs one directly-valued component per layer, and a villager
 * {@code wants} predicate matches a named component's value as a whole. {@link
 * #attachmentComponent(Slot)} is this mod's one place mapping a slot to its own component type, so
 * callers never repeat the five-way switch themselves.
 */
public final class ComponentRegistration {

    /** {@code firearms:base} — always present on a weapon item; which base it is and its schema version. */
    public static final DataComponentType<Base> BASE = register("base",
        DataComponentType.<Base>builder().persistent(BaseCodec.CODEC).networkSynchronized(BaseCodec.STREAM_CODEC).build());

    /** {@code firearms:ammo} — present once a weapon has been loaded at least once. */
    public static final DataComponentType<Ammo> AMMO = register("ammo",
        DataComponentType.<Ammo>builder().persistent(AmmoCodec.CODEC).networkSynchronized(AmmoCodec.STREAM_CODEC).build());

    /** {@code firearms:attachment_muzzle} — the muzzle attachment's own id, absent when the slot is empty. */
    public static final DataComponentType<Identifier> ATTACHMENT_MUZZLE = attachment("attachment_muzzle");
    /** {@code firearms:attachment_optic} — the optic attachment's own id, absent when the slot is empty. */
    public static final DataComponentType<Identifier> ATTACHMENT_OPTIC = attachment("attachment_optic");
    /** {@code firearms:attachment_magazine} — the magazine attachment's own id, absent when the slot is empty. */
    public static final DataComponentType<Identifier> ATTACHMENT_MAGAZINE = attachment("attachment_magazine");
    /** {@code firearms:attachment_grip} — the grip attachment's own id, absent when the slot is empty. */
    public static final DataComponentType<Identifier> ATTACHMENT_GRIP = attachment("attachment_grip");
    /** {@code firearms:attachment_stock} — the stock attachment's own id, absent when the slot is empty. */
    public static final DataComponentType<Identifier> ATTACHMENT_STOCK = attachment("attachment_stock");

    private static final Map<Slot, DataComponentType<Identifier>> BY_SLOT = bySlot();

    private static Map<Slot, DataComponentType<Identifier>> bySlot() {
        EnumMap<Slot, DataComponentType<Identifier>> map = new EnumMap<>(Slot.class);
        map.put(Slot.MUZZLE, ATTACHMENT_MUZZLE);
        map.put(Slot.OPTIC, ATTACHMENT_OPTIC);
        map.put(Slot.MAGAZINE, ATTACHMENT_MAGAZINE);
        map.put(Slot.GRIP, ATTACHMENT_GRIP);
        map.put(Slot.STOCK, ATTACHMENT_STOCK);
        return Map.copyOf(map);
    }

    /**
     * The one {@code firearms:attachment_<slot>} component type carrying {@code slot}'s own
     * attachment id — on a weapon item, that slot's present-or-absent state; on this mod's own
     * attachment item for that slot, which of the slot's attachments this stack is
     * (`firearms.item.AttachmentItem`).
     */
    public static DataComponentType<Identifier> attachmentComponent(Slot slot) {
        return BY_SLOT.get(slot);
    }

    private static DataComponentType<Identifier> attachment(String path) {
        return register(path, DataComponentType.<Identifier>builder()
            .persistent(Identifier.CODEC)
            .networkSynchronized(Identifier.STREAM_CODEC)
            .build());
    }

    private static <T> DataComponentType<T> register(String path, DataComponentType<T> type) {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Firearms.id(path), type);
    }

    /** Forces this class's static registrations to run; call once from {@link Firearms#onInitialize()}. */
    public static void register() {
    }

    private ComponentRegistration() {
    }
}
