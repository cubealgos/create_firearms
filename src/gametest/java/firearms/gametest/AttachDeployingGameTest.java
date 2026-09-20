package firearms.gametest;

import com.zurrtum.create.AllBlocks;
import com.zurrtum.create.content.kinetics.base.DirectionalKineticBlock;
import com.zurrtum.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.zurrtum.create.content.kinetics.belt.transport.TransportedItemStack;
import com.zurrtum.create.content.kinetics.deployer.BeltDeployerCallbacks;
import com.zurrtum.create.content.kinetics.deployer.DeployerBlockEntity;
import com.zurrtum.create.content.logistics.depot.DepotBlockEntity;
import firearms.Firearms;
import firearms.attach.AttachDeployingRecipe;
import firearms.component.Base;
import firearms.component.ComponentRegistration;
import firearms.item.ItemRegistration;
import firearms.model.Slot;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.GameType;

/**
 * A real {@code DeployerBlockEntity} finds and runs {@link AttachDeployingRecipe} exactly as
 * Create's own deploying recipes are found — zero mixin (`docs/spec/domains/attach.md`
 * `ATTACH-REQ-005`, `006`; `docs/spec/04-architecture.md` `ARCH-DEC-003`).
 *
 * <p><b>How the deployer is driven</b>: this ticket's own {@code javap -p -c} of
 * {@code com.zurrtum.create.content.kinetics.deployer.BeltDeployerCallbacks} (recorded in this
 * ticket's {@code ## Findings}) shows its public static {@code activate(TransportedItemStack,
 * TransportedItemStackHandlerBehaviour, DeployerBlockEntity, Recipe<?>)} is the one method both
 * the belt-segment path ({@code whenItemHeld}) and — since a {@code DepotBlockEntity}'s own {@code
 * DepotBehaviour} registers the identical {@code TransportedItemStackHandlerBehaviour} sub-
 * behaviour a moving belt segment does — the world/depot path eventually call to actually apply a
 * deploying recipe: it reads the target off the given {@code TransportedItemStack}, the held
 * attachment off the deployer's own fake player, calls this recipe's {@code assemble()}, shrinks
 * the target by one, and (since {@link AttachDeployingRecipe#keepHeldItem()} is {@code false})
 * shrinks the held attachment by one. Calling it directly — the same spirit as
 * {@code create_synthetic_diamonds}'s {@code WeightedPressingGameTest} driving
 * {@code MechanicalPressBlockEntity#tryProcessInWorld} directly rather than simulating RPM —
 * exercises the real application path without needing to simulate the deployer's own multi-tick
 * fist-bump animation or a moving belt's segment tracking. The target {@code TransportedItemStack}
 * is placed into the depot via {@code DepotBehaviour#setCenteredHeldItem} first: decompiling
 * {@code TransportedItemStackHandlerBehaviour#handleProcessingOnItem} shows its callback matches
 * the given {@code TransportedItemStack} against the depot's own held item by reference equality,
 * so the same instance must be used on both sides.
 *
 * <p><b>The belt path</b> is not separately driven headless here: {@code BeltDeployerCallbacks} is
 * the one class both the depot and a moving belt segment call into (confirmed above by
 * decompilation — a depot is, for this purpose, a stationary belt segment), so this test's depot
 * scenario already exercises the identical application code a real contraption belt runs. Driving
 * an actually-moving belt segment's own position tracking would additionally exercise Create's
 * belt engine itself, not this mod's recipe, and is out of this ticket's scope.
 */
public final class AttachDeployingGameTest {

    @GameTest
    public void aRealDeployerFindsAndAppliesTheDeployingAttachRecipe(GameTestHelper helper) {
        BlockPos depotPos = new BlockPos(1, 1, 1);
        BlockPos deployerPos = new BlockPos(1, 2, 1);
        helper.setBlock(depotPos, AllBlocks.DEPOT.defaultBlockState());
        helper.setBlock(deployerPos, AllBlocks.DEPLOYER.defaultBlockState().setValue(DirectionalKineticBlock.FACING, Direction.DOWN));

        DepotBlockEntity depot = helper.getBlockEntity(depotPos, DepotBlockEntity.class);
        DeployerBlockEntity deployer = helper.getBlockEntity(deployerPos, DeployerBlockEntity.class);
        deployer.initHandler();

        ItemStack uzi = microUzi();
        TransportedItemStack targetTransported = new TransportedItemStack(uzi.copy());
        depot.depotBehaviour.setCenteredHeldItem(targetTransported);
        deployer.invHandler.setItem(0, attachment(Slot.MUZZLE, "suppressor").copy());

        TransportedItemStackHandlerBehaviour handler = depot.getBehaviour(TransportedItemStackHandlerBehaviour.TYPE);
        Recipe<? extends RecipeInput> recipe = deployer.getRecipe(targetTransported.stack);
        helper.assertTrue(recipe instanceof AttachDeployingRecipe,
            "a real DeployerBlockEntity.getRecipe() must find firearms:attach_deploying with zero mixin (ATTACH-REQ-005), found " + recipe);

        BeltDeployerCallbacks.activate(targetTransported, handler, deployer, recipe);

        ItemStack depositedResult = depot.depotBehaviour.getHeldItemStack();
        helper.assertFalse(depositedResult.isEmpty(), "the depot must hold the merged result after the deployer applies the recipe");
        helper.assertTrue(depositedResult.getItem() == ItemRegistration.WEAPON, "the result must still be firearms:weapon");
        helper.assertValueEqual(depositedResult.getCount(), 1, "the depot must hold exactly one merged weapon, not a leftover or duplicate stack");

        Identifier muzzle = depositedResult.get(ComponentRegistration.attachmentComponent(Slot.MUZZLE));
        helper.assertTrue(muzzle != null && muzzle.equals(Firearms.id("suppressor")),
            "the result must carry firearms:attachment_muzzle = firearms:suppressor, was " + muzzle);

        Base resultBase = depositedResult.get(ComponentRegistration.BASE);
        Base uziBase = uzi.get(ComponentRegistration.BASE);
        helper.assertTrue(resultBase != null && resultBase.equals(uziBase), "every other component, including firearms:base, must be unchanged (ATTACH-REQ-003)");

        helper.assertTrue(deployer.invHandler.getItem(0).isEmpty(),
            "the deployer's held attachment must be consumed on a successful attach (ATTACH-REQ-006, keep_held_item: false)");

        // TEST-REQ-003: the deploying path and the smithing path must reach byte-identical
        // component output for the same base weapon and attachment, proving both call the one
        // shared attach function (`firearms.attach.Attach`).
        ItemStack smithingResult = smithingAttach(helper, microUzi(), attachment(Slot.MUZZLE, "suppressor"));
        helper.assertTrue(ItemStack.isSameItemSameComponents(depositedResult, smithingResult),
            "the deploying and smithing recipes must reach byte-identical component output (TEST-REQ-003): "
                + depositedResult.getComponents() + " vs " + smithingResult.getComponents());

        helper.succeed();
    }

    @GameTest
    public void aSecondSuppressorOnAnAlreadySuppressedUziMatchesNothing(GameTestHelper helper) {
        BlockPos deployerPos = new BlockPos(1, 1, 1);
        helper.setBlock(deployerPos, AllBlocks.DEPLOYER.defaultBlockState().setValue(DirectionalKineticBlock.FACING, Direction.DOWN));
        DeployerBlockEntity deployer = helper.getBlockEntity(deployerPos, DeployerBlockEntity.class);
        deployer.initHandler();

        ItemStack alreadySuppressed = microUzi();
        alreadySuppressed.set(ComponentRegistration.attachmentComponent(Slot.MUZZLE), Firearms.id("suppressor"));
        deployer.invHandler.setItem(0, attachment(Slot.MUZZLE, "suppressor").copy());

        Recipe<? extends RecipeInput> recipe = deployer.getRecipe(alreadySuppressed);
        helper.assertTrue(recipe == null,
            "the deployer must find no recipe for an already-occupied slot — it does nothing that cycle (ATTACH-FAIL-004, ATTACH-REQ-002), found " + recipe);
        helper.succeed();
    }

    private static ItemStack smithingAttach(GameTestHelper helper, ItemStack base, ItemStack addition) {
        Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        SmithingMenu menu = new SmithingMenu(0, player.getInventory());
        menu.getSlot(SmithingMenu.BASE_SLOT).set(base.copy());
        menu.getSlot(SmithingMenu.ADDITIONAL_SLOT).set(addition.copy());
        menu.createResult();
        return menu.getSlot(SmithingMenu.RESULT_SLOT).getItem();
    }

    private static ItemStack microUzi() {
        ItemStack stack = new ItemStack(ItemRegistration.WEAPON);
        stack.set(ComponentRegistration.BASE, Base.of(Firearms.id("micro_uzi")));
        stack.set(DataComponents.MAX_DAMAGE, 300);
        stack.set(DataComponents.DAMAGE, 0);
        return stack;
    }

    private static ItemStack attachment(Slot slot, String attachmentId) {
        ItemStack stack = new ItemStack(ItemRegistration.attachment(slot));
        stack.set(ComponentRegistration.attachmentComponent(slot), Firearms.id(attachmentId));
        return stack;
    }
}
