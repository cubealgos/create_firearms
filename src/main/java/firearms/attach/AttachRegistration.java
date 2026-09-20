package firearms.attach;

import firearms.Firearms;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Registers {@link AttachSmithingRecipe}'s serializer as {@code firearms:attach_smithing}
 * (`docs/spec/contracts/public-surface.md`), through the same public
 * {@code BuiltInRegistries.RECIPE_SERIALIZER} door every mod recipe uses — no mixin
 * (`docs/spec/04-architecture.md` `ARCH-DEC-002`). Registration alone is not what makes the
 * smithing table find this recipe; that is {@code SmithingRecipe#getType()}'s inherited
 * {@code RecipeType.SMITHING} default plus the live-{@code getType()} bucketing
 * {@code RecipeMap} already does for every recipe of that type.
 */
public final class AttachRegistration {

    public static void register() {
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Firearms.id("attach_smithing"), AttachSmithingRecipe.SERIALIZER);
    }

    private AttachRegistration() {
    }
}
