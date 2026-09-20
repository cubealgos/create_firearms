package firearms.attach;

import com.mojang.serialization.MapCodec;
import firearms.item.ItemRegistration;
import firearms.model.Slot;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;

/**
 * The custom {@code SmithingRecipe} implementor {@code docs/spec/04-architecture.md}
 * {@code ARCH-DEC-002} calls for: base slot = {@code firearms:weapon} (any base), addition slot =
 * one of the five attachment items, template slot = empty. Implementing {@link SmithingRecipe}
 * directly inherits {@link SmithingRecipe#getType()}'s default {@code RecipeType.SMITHING}, so the
 * vanilla smithing table finds and slot-highlights this recipe with zero mixin — the same live-
 * {@code getType()} bucketing and {@code instanceof SmithingRecipe} highlighting the research note
 * confirmed (`vault/technical/minecraft/smithing-and-item-model-layers-26-2.md` §A.3). The class
 * carries no per-instance data (one recipe matches every base weapon against every attachment
 * generically, `docs/spec/domains/attach.md` §"Recipe shapes"), so its JSON body is empty and its
 * codec is a unit codec. All matching and assembling is delegated to {@link Attach}, the one place
 * this logic lives (`ATTACH-REQ-008`).
 *
 * <p><b>Template-slot verdict</b> (`docs/spec/domains/attach.md` `ATTACH-REQ-001`): this recipe
 * declares no template ingredient at all ({@link #templateIngredient()} returns
 * {@link Optional#empty()}). {@code SmithingRecipe}'s own default {@code matches()} — which this
 * class does not use, since it overrides {@link #matches(SmithingRecipeInput, Level)} directly —
 * disassembles to {@code Ingredient.testOptionalIngredient}, which for an empty {@code Optional}
 * requires the real template slot's stack to be empty for a match; this class's own override
 * enforces the identical rule explicitly. No template item of any kind is required at 1.0,
 * confirmed live by {@code firearms.gametest.AttachSmithingGameTest}.
 */
public final class AttachSmithingRecipe implements SmithingRecipe {

    /** Every attachment item, in {@link Slot}'s own order — what may sit in the addition slot, regardless of which weapon class it fits. */
    private static final Ingredient ADDITION_INGREDIENT = Ingredient.of(attachmentItems());

    public static final AttachSmithingRecipe INSTANCE = new AttachSmithingRecipe();

    public static final MapCodec<AttachSmithingRecipe> CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, AttachSmithingRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<AttachSmithingRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

    private final PlacementInfo placementInfo = PlacementInfo.createFromOptionals(
        List.of(templateIngredientStatic(), Optional.of(baseIngredientStatic()), Optional.of(ADDITION_INGREDIENT)));

    private AttachSmithingRecipe() {
    }

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        return input.template().isEmpty() && Attach.matches(input.base(), input.addition());
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input) {
        return Attach.assemble(input.base(), input.addition());
    }

    @Override
    public RecipeSerializer<? extends SmithingRecipe> getSerializer() {
        return SERIALIZER;
    }

    /** Empty — no template item is required or accepted at 1.0 (see class Javadoc, "Template-slot verdict"). */
    @Override
    public Optional<Ingredient> templateIngredient() {
        return templateIngredientStatic();
    }

    /** Any {@code firearms:weapon} stack, regardless of which base it is — the specific base is read from its own {@code firearms:base} component, not the ingredient. */
    @Override
    public Ingredient baseIngredient() {
        return baseIngredientStatic();
    }

    /** Any of the five attachment items — which one, and which slot it fits, is read off the stack itself in {@link Attach#matches}. */
    @Override
    public Optional<Ingredient> additionIngredient() {
        return Optional.of(ADDITION_INGREDIENT);
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public boolean showNotification() {
        return true;
    }

    @Override
    public PlacementInfo placementInfo() {
        return placementInfo;
    }

    private static Optional<Ingredient> templateIngredientStatic() {
        return Optional.empty();
    }

    private static Ingredient baseIngredientStatic() {
        return Ingredient.of(ItemRegistration.WEAPON);
    }

    private static Item[] attachmentItems() {
        Item[] items = new Item[Slot.values().length];
        for (int i = 0; i < items.length; i++) {
            items[i] = ItemRegistration.attachment(Slot.values()[i]);
        }
        return items;
    }
}
