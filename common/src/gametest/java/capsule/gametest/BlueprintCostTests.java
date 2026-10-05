package capsule.gametest;

import capsule.StructureSaver;
import capsule.helpers.Blueprint;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.Map;

import static capsule.gametest.CapsuleTestUtils.assertTrue;
import static capsule.gametest.CapsuleTestUtils.capture;
import static capsule.gametest.CapsuleTestUtils.template;

public class BlueprintCostTests {

    @GameTest(template = "empty")
    public static void pottedPlantsAreNotFree(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.POTTED_DANDELION);
        helper.getLevel().setBlock(helper.absolutePos(new BlockPos(1, 2, 1)), Blocks.ATTACHED_MELON_STEM.defaultBlockState(), 2 | 16);
        ItemStack capsule = capture(helper, new BlockPos(0, 1, 0), 3);

        Map<StructureSaver.ItemStackKey, Integer> cost = Blueprint.getMaterialList(template(helper, capsule), helper.getLevel(), null);

        assertTrue(helper, cost != null && Integer.valueOf(1).equals(cost.get(new StructureSaver.ItemStackKey(new ItemStack(Items.FLOWER_POT)))), "a flower pot should be required, got " + cost);
        assertTrue(helper, Integer.valueOf(1).equals(cost.get(new StructureSaver.ItemStackKey(new ItemStack(Items.DANDELION)))), "a dandelion should be required, got " + cost);
        assertTrue(helper, Integer.valueOf(1).equals(cost.get(new StructureSaver.ItemStackKey(new ItemStack(Items.MELON_SEEDS)))), "melon seeds should be required, got " + cost);
        helper.succeed();
    }
}
