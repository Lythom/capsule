package capsule.gametest;

import capsule.StructureSaver;
import capsule.helpers.Capsule;
import capsule.structure.CapsuleTemplate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Arrays;
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

    @GameTest(template = "empty17", timeoutTicks = 200)
    public static void starterCraftingTablesHaveAFreeFace(GameTestHelper helper) {
        List<String> starters = BundledContentTests.bundledTemplates(helper).stream().filter(path -> path.contains("/starters/")).toList();
        BlockPos anchor = new BlockPos(8, 0, 8);
        List<String> checked = new ArrayList<>();
        List<String> blocked = new ArrayList<>();

        for (String path : starters) {
            CapsuleTemplate template = StructureSaver.getTemplateForReward(helper.getLevel().getServer(), path).getRight();
            int size = Math.max(template.getSize().getX(), Math.max(template.getSize().getY(), template.getSize().getZ())) | 1;
            CapsuleTestUtils.deploy(helper, Capsule.newRewardCapsuleItemStack(path, 0, 0, size, null, null), anchor, null);
            int half = size / 2;
            BoundingBox box = BoundingBox.fromCorners(anchor.offset(-half, 1, -half), anchor.offset(half, size, half));
            BlockPos.betweenClosedStream(box).filter(pos -> helper.getBlockState(pos).is(Blocks.CRAFTING_TABLE)).map(BlockPos::immutable).forEach(table -> {
                checked.add(path + " " + table);
                if (Arrays.stream(Direction.values()).noneMatch(face -> isFreeFace(helper, box, table, face))) blocked.add(path + " " + table);
            });
            helper.getLevel().getEntitiesOfClass(ItemFrame.class, AABB.of(box).move(helper.absolutePos(BlockPos.ZERO)))
                    .stream().filter(frame -> !frame.survives()).forEach(frame -> blocked.add(path + " frame dropping at " + helper.relativePos(frame.getPos())));
            CapsuleTestUtils.clear(helper, new BlockPos(0, 1, 0), new BlockPos(16, 16, 16));
        }

        assertTrue(helper, checked.size() >= starters.size(), "every starter should have a crafting table, checked " + checked);
        assertTrue(helper, blocked.isEmpty(), "crafting tables without a free face inside the starter, or frames dropping: " + blocked);
        helper.succeed();
    }

    /**
     * An air block inside the structure in front of the face, without an item frame hanging on that face.
     */
    private static boolean isFreeFace(GameTestHelper helper, BoundingBox box, BlockPos table, Direction face) {
        BlockPos front = table.relative(face);
        BlockPos absoluteFront = helper.absolutePos(front);
        return box.isInside(front) && helper.getBlockState(front).isAir()
                && helper.getLevel().getEntitiesOfClass(ItemFrame.class, new AABB(absoluteFront))
                .stream().noneMatch(frame -> frame.getPos().equals(absoluteFront) && frame.getDirection() == face);
    }
}
