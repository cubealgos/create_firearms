package firearms.attach;

import com.mojang.serialization.MapCodec;
import com.zurrtum.create.AllRecipeTypes;
import com.zurrtum.create.content.kinetics.deployer.ItemApplicationInput;
import com.zurrtum.create.content.kinetics.deployer.ItemApplicationRecipe;
import com.zurrtum.create.content.processing.recipe.ProcessingOutput;
import firearms.item.ItemRegistration;
import firearms.model.Slot;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * The custom {@code Recipe<ItemApplicationInput>} implementor {@code docs/spec/04-architecture.md}
 * {@code ARCH-DEC-003} calls for: target = {@code firearms:weapon} (any base), ingredient (the
 * deployer's held item) = one of the five attachment items, {@code keepHeldItem() == false}
 * (`ATTACH-REQ-006`). Implementing {@link ItemApplicationRecipe} inherits its {@code matches()}
 * default only in shape — this class overrides {@link #matches(ItemApplicationInput, Level)}
 * directly, the same pattern {@link AttachSmithingRecipe} uses for {@code SmithingRecipe} — and
 * reports the literal {@code AllRecipeTypes.DEPLOYING} field from {@link #getType()}, so a real
 * {@code DeployerBlockEntity} finds this recipe through its ordinary {@code RecipeManager}/{@code
 * RecipeMap} lookup with zero mixin, in both belt and world/depot mode (research
 * `vault/technical/minecraft/create-fly-potato-cannon-and-deploying-26-2.md` §B.3, confirmed by
 * decompiling {@code BeltDeployerCallbacks}: the depot block's own {@code
 * TransportedItemStackHandlerBehaviour} runs the identical application code a moving belt segment
 * does, see this ticket's {@code ## Findings}).
 *
 * <p><b>Why {@code assemble()} is overridden rather than left to {@link ItemApplicationRecipe}'s
 * own default</b>: Create Fly's default {@code assemble()} for a deploying recipe rolls output
 * stacks purely from {@link #results()}' fixed {@code ProcessingOutput}s — it never reads or
 * merges either input stack's live components (research, §B.2). This recipe needs to copy the
 * target weapon's actual, live {@code firearms:base} identity plus the held attachment's actual,
 * live slot id onto one merged result, which only the shared attach function
 * ({@link Attach#assemble(ItemStack, ItemStack)}) can do — the identical function {@link
 * AttachSmithingRecipe} calls, never a second copy of the merge logic (`ATTACH-REQ-008`).
 * {@link #results()} therefore returns an empty list and is never consulted; it exists only
 * because {@link ItemApplicationRecipe} declares it {@code abstract}.
 */
public final class AttachDeployingRecipe implements ItemApplicationRecipe {

    /** Any {@code firearms:weapon} stack, regardless of which base it is — the specific base and its slot occupancy are read off the stack's own components in {@link Attach#matches}, not from this ingredient. */
    private static final Ingredient TARGET_INGREDIENT = Ingredient.of(ItemRegistration.WEAPON);

    /** Any of the five attachment items — which one, and which slot it fits, is read off the stack itself in {@link Attach#matches}. */
    private static final Ingredient INGREDIENT_INGREDIENT = Ingredient.of(attachmentItems());

    public static final AttachDeployingRecipe INSTANCE = new AttachDeployingRecipe();

    public static final MapCodec<AttachDeployingRecipe> CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, AttachDeployingRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<AttachDeployingRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

    private AttachDeployingRecipe() {
    }

    @Override
    public boolean matches(ItemApplicationInput input, Level level) {
        return Attach.matches(input.target(), input.ingredient());
    }

    /**
     * A thin adapter reading {@code target}/{@code ingredient} off {@link ItemApplicationInput}
     * and calling the shared attach function (`ATTACH-REQ-008`); never re-implements slot-match or
     * component-merge itself. Returns an empty list rather than {@link ItemStack#EMPTY} directly,
     * since {@link com.zurrtum.create.foundation.recipe.CreateRollableRecipe} deals in result
     * lists — {@link Attach#assemble(ItemStack, ItemStack)} only returns an empty stack for a
     * malformed addition stack that a correctly-crafted attachment item never produces (see its
     * own Javadoc), a case {@link #matches(ItemApplicationInput, Level)} does not otherwise guard
     * against.
     */
    @Override
    public List<ItemStack> assemble(ItemApplicationInput input, RandomSource random) {
        ItemStack result = Attach.assemble(input.target(), input.ingredient());
        return result.isEmpty() ? List.of() : List.of(result);
    }

    @Override
    public RecipeSerializer<AttachDeployingRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    @SuppressWarnings("unchecked")
    public RecipeType<AttachDeployingRecipe> getType() {
        // The literal static field, not a new registration (`ARCH-DEC-003`): generics erase at
        // runtime, so this cast only satisfies the compiler, matching `WeightedPressingRecipe`'s
        // own getType() target in create_synthetic_diamonds.
        return (RecipeType<AttachDeployingRecipe>) (RecipeType<?>) AllRecipeTypes.DEPLOYING;
    }

    /** Never keeps the held attachment: consumed on a successful attach (`ATTACH-REQ-006`). */
    @Override
    public boolean keepHeldItem() {
        return false;
    }

    @Override
    public Ingredient target() {
        return TARGET_INGREDIENT;
    }

    @Override
    public Ingredient ingredient() {
        return INGREDIENT_INGREDIENT;
    }

    /** Empty — see class Javadoc, "Why assemble() is overridden". */
    @Override
    public List<ProcessingOutput> results() {
        return List.of();
    }

    private static Item[] attachmentItems() {
        Item[] items = new Item[Slot.values().length];
        for (int i = 0; i < items.length; i++) {
            items[i] = ItemRegistration.attachment(Slot.values()[i]);
        }
        return items;
    }
}
