package capsule.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.List;

import static capsule.gametest.CapsuleTestUtils.assertTrue;
import static capsule.gametest.CapsuleTestUtils.capture;
import static capsule.gametest.CapsuleTestUtils.deploy;

public class ItemFrameTests {

    /**
     * The deploy is far enough from the capture for the frame's saved block position to be refused when it is not updated.
     */
    @GameTest(template = "empty17")
    public static void deployedItemFramesHangOnTheirBlocks(GameTestHelper helper) {
        helper.setBlock(0, 2, 1, Blocks.STONE);
        ItemFrame frame = new ItemFrame(helper.getLevel(), helper.absolutePos(new BlockPos(1, 2, 1)), Direction.EAST);
        frame.setItem(new ItemStack(Items.DIAMOND));
        helper.getLevel().addFreshEntity(frame);
        ItemStack capsule = capture(helper, new BlockPos(0, 1, 0), 3);

        try (LogCapture logs = LogCapture.open()) {
            assertTrue(helper, deploy(helper, capsule, new BlockPos(14, 0, 14), null), "deploy should succeed");
            assertTrue(helper, logs.containing("Block-attached entity at invalid position").isEmpty(), "invalid position logged: " + logs.messages());
        }

        helper.assertBlockPresent(Blocks.STONE, 13, 2, 14);
        List<ItemFrame> frames = helper.getLevel().getEntities(EntityType.ITEM_FRAME, new AABB(helper.absolutePos(new BlockPos(12, 1, 12))).inflate(4), f -> true);
        assertTrue(helper, frames.size() == 1, "one item frame should be deployed, got " + frames.size());
        ItemFrame deployed = frames.get(0);
        assertTrue(helper, deployed.getPos().equals(helper.absolutePos(new BlockPos(14, 2, 14))) && deployed.getDirection() == Direction.EAST,
                "the frame should hang east of the stone, got " + helper.relativePos(deployed.getPos()) + " " + deployed.getDirection());
        assertTrue(helper, deployed.survives() && deployed.getItem().is(Items.DIAMOND), "the frame should hold on its block with its diamond");
        helper.succeed();
    }
}
