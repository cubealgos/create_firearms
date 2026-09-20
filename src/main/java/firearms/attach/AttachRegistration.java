package firearms.attach;

import firearms.Firearms;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Registers {@link AttachSmithingRecipe}'s serializer as {@code firearms:attach_smithing} and
 * {@link AttachDeployingRecipe}'s as {@code firearms:attach_deploying}
 * (`docs/spec/contracts/public-surface.md`), through the same public
 * {@code BuiltInRegistries.RECIPE_SERIALIZER} door every mod recipe uses — no mixin
 * (`docs/spec/04-architecture.md` `ARCH-DEC-002`, `ARCH-DEC-003`). Registration alone is not what
 * makes the smithing table or the deployer find either recipe; that is each recipe's own
 * {@code getType()} — {@code RecipeType.SMITHING} inherited by default, {@code
 * AllRecipeTypes.DEPLOYING} reported explicitly — plus the live-{@code getType()} bucketing
 * {@code RecipeMap} already does for every recipe of that type.
 */
public final class AttachRegistration {

    public static void register() {
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Firearms.id("attach_smithing"), AttachSmithingRecipe.SERIALIZER);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Firearms.id("attach_deploying"), AttachDeployingRecipe.SERIALIZER);
    }

    private AttachRegistration() {
    }
}
