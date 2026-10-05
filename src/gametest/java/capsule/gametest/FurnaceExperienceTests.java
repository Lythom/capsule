package capsule.gametest;

import capsule.CapsuleMod;
import capsule.helpers.Capsule;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static capsule.gametest.CapsuleTestUtils.assertTrue;
import static capsule.gametest.CapsuleTestUtils.capture;
import static capsule.gametest.CapsuleTestUtils.deploy;

@GameTestHolder(CapsuleMod.MODID)
@PrefixGameTestTemplate(false)
public class FurnaceExperienceTests {

    @GameTest(template = "empty")
    public static void capturingAFurnaceDropsNoExperience(GameTestHelper helper) {
        BlockPos furnacePos = new BlockPos(2, 1, 2);
        helper.setBlock(furnacePos, Blocks.FURNACE);
        FurnaceBlockEntity furnace = helper.getBlockEntity(furnacePos);
        var recipe = helper.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.SMELTING).getFirst();
        for (int i = 0; i < 10; i++) furnace.setRecipeUsed(recipe);

        ItemStack capsule = capture(helper, new BlockPos(1, 1, 1), 3);
        helper.assertEntityNotPresent(EntityType.EXPERIENCE_ORB);
        deploy(helper, capsule, new BlockPos(5, 0, 5), null);
        Capsule.resentToCapsule(capsule, helper.getLevel(), null);
        helper.assertEntityNotPresent(EntityType.EXPERIENCE_ORB);
        assertTrue(helper, !CapsuleTestUtils.template(helper, capsule).getPalette().getFirst().nbt().getCompound("RecipesUsed").isEmpty(), "the stored experience stays in the furnace");
        helper.succeed();
    }
}
