package capsule.gametest;

import capsule.CapsuleMod;
import capsule.blocks.BlockCapsuleMarker;
import capsule.blocks.BlockEntityCapture;
import capsule.blocks.CapsuleBlocks;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItem.CapsuleState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static capsule.gametest.CapsuleTestUtils.assertTrue;

@GameTestHolder(CapsuleMod.MODID)
@PrefixGameTestTemplate(false)
public class CaptureBaseTests {

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void freshCaptureBaseDispensesOnFirstSignal(GameTestHelper helper) {
        helper.setBlock(2, 1, 2, Blocks.STONE);
        helper.setBlock(2, 2, 2, Blocks.OAK_PLANKS);
        ItemStack capsule = CapsuleTestUtils.capture(helper, new BlockPos(1, 1, 1), 3);
        BlockPos markerPos = new BlockPos(5, 1, 5);
        helper.setBlock(markerPos, CapsuleBlocks.CAPSULE_MARKER.get().defaultBlockState().setValue(BlockCapsuleMarker.FACING, Direction.UP));
        BlockEntityCapture marker = helper.getBlockEntity(markerPos);
        marker.setItem(0, capsule);
        BlockPos powerPos = markerPos.east();

        helper.startSequence()
                .thenExecute(() -> helper.setBlock(powerPos, Blocks.REDSTONE_BLOCK))
                .thenWaitUntil(() -> {
                    helper.assertBlockPresent(Blocks.STONE, 5, 2, 5);
                    helper.assertBlockPresent(Blocks.OAK_PLANKS, 5, 3, 5);
                })
                .thenExecute(() -> {
                    assertTrue(helper, CapsuleItem.hasState(marker.getItem(0), CapsuleState.DEPLOYED), "dispensed capsule should be deployed");
                    helper.setBlock(powerPos, Blocks.AIR);
                })
                .thenIdle(5)
                .thenExecute(() -> helper.setBlock(powerPos, Blocks.REDSTONE_BLOCK))
                .thenWaitUntil(() -> helper.assertBlockNotPresent(Blocks.STONE, 5, 2, 5))
                .thenExecute(() -> {
                    assertTrue(helper, CapsuleItem.hasState(marker.getItem(0), CapsuleState.LINKED), "dispensed capsule should be linked again");
                    helper.assertBlockNotPresent(Blocks.OAK_PLANKS, 5, 3, 5);
                })
                .thenSucceed();
    }
}
