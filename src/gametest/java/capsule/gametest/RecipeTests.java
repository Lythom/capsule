package capsule.gametest;

import capsule.CapsuleMod;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItem.CapsuleState;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static capsule.gametest.CapsuleTestUtils.assertTrue;

@GameTestHolder(CapsuleMod.MODID)
@PrefixGameTestTemplate(false)
public class RecipeTests {

    private static ItemStack craft(GameTestHelper helper, String expectedRecipe, int width, int height, ItemStack... items) {
        CraftingInput input = CraftingInput.of(width, height, List.of(items));
        RecipeHolder<CraftingRecipe> recipe = helper.getLevel().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .orElse(null);
        if (recipe == null) helper.fail("no recipe matches for " + expectedRecipe);
        assertTrue(helper, recipe.id().toString().equals(expectedRecipe), "expected " + expectedRecipe + " but matched " + recipe.id());
        return recipe.value().assemble(input, helper.getLevel().registryAccess());
    }

    private static ItemStack linkedCapsule(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.STONE);
        return CapsuleTestUtils.capture(helper, new BlockPos(1, 1, 1), 1);
    }

    @GameTest(template = "empty")
    public static void dyeRecipeColorsCapsule(GameTestHelper helper) {
        ItemStack capsule = CapsuleTestUtils.emptyCapsule(3);

        ItemStack result = craft(helper, "capsule:dye", 2, 1, capsule, new ItemStack(Items.RED_DYE));

        assertTrue(helper, CapsuleItem.getBaseColor(result) != CapsuleItem.getBaseColor(capsule), "base color should change");
        assertTrue(helper, CapsuleItem.getSize(result) == 3, "size is kept");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void upgradeRecipeGrowsCapsule(GameTestHelper helper) {
        ItemStack result = craft(helper, "capsule:upgrade", 2, 1, CapsuleTestUtils.emptyCapsule(3), new ItemStack(Items.POPPED_CHORUS_FRUIT));

        assertTrue(helper, CapsuleItem.getSize(result) == 5, "size should be 5, got " + CapsuleItem.getSize(result));
        assertTrue(helper, CapsuleItem.getUpgradeLevel(result) == 1, "upgrade level should be 1");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void clearRecipeEmptiesCapsule(GameTestHelper helper) {
        ItemStack result = craft(helper, "capsule:clear", 1, 1, linkedCapsule(helper));

        assertTrue(helper, CapsuleItem.hasState(result, CapsuleState.EMPTY), "capsule should be empty");
        assertTrue(helper, CapsuleItem.getStructureName(result) == null, "structure link should be removed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void recoveryRecipeMakesOneUseCopy(GameTestHelper helper) {
        ItemStack linked = linkedCapsule(helper);

        ItemStack result = craft(helper, "capsule:recovery", 2, 1, linked, new ItemStack(Items.GLASS_BOTTLE));

        assertTrue(helper, CapsuleItem.hasState(result, CapsuleState.ONE_USE) && CapsuleItem.isOneUse(result), "result should be a one use capsule");
        assertTrue(helper, CapsuleItem.getStructureName(result).equals(CapsuleItem.getStructureName(linked)), "recovery points to the same content");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void blueprintRecipeCopiesCapsule(GameTestHelper helper) {
        ItemStack linked = linkedCapsule(helper);
        ItemStack e = ItemStack.EMPTY;

        ItemStack result = craft(helper, "capsule:blueprint", 3, 3,
                e, new ItemStack(Items.STONE_BUTTON), e,
                new ItemStack(Items.BLUE_DYE), linked, new ItemStack(Items.BLUE_DYE),
                e, new ItemStack(Items.PAPER), e);

        assertTrue(helper, CapsuleItem.isBlueprint(result), "result should be a blueprint");
        assertTrue(helper, CapsuleItem.hasState(result, CapsuleState.DEPLOYED), "a crafted blueprint is uncharged");
        assertTrue(helper, CapsuleItem.getStructureName(result).equals(CapsuleItem.getStructureName(linked)), "blueprint references the source until crafted");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ironCapsuleRecipe(GameTestHelper helper) {
        ItemStack e = ItemStack.EMPTY;
        ItemStack iron = new ItemStack(Items.IRON_INGOT);

        ItemStack result = craft(helper, "capsule:capsule_iron", 3, 3,
                e, new ItemStack(Items.STONE_BUTTON), e,
                iron, new ItemStack(Items.ENDER_PEARL), iron,
                e, iron, e);

        assertTrue(helper, CapsuleItem.hasState(result, CapsuleState.EMPTY) && CapsuleItem.getSize(result) == 3, "iron capsule should be an empty 3x3x3 capsule");
        helper.succeed();
    }
}
