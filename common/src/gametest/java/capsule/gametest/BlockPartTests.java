package capsule.gametest;

import capsule.blocks.BlockCapsuleMarker;
import capsule.blocks.CapsuleBlocks;
import capsule.helpers.Capsule;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItem.CapsuleState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static capsule.gametest.CapsuleTestUtils.assertTrue;

/**
 * A capture never cuts a block of two parts in two: a capture whose cube holds one half of a door or a bed and not the
 * other is refused with a message, and takes nothing. A cube holding both parts takes them together.
 */
public class BlockPartTests {
    static final BlockState DOOR_LOWER = Blocks.OAK_DOOR.defaultBlockState();
    static final BlockState DOOR_UPPER = DOOR_LOWER.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
    static final BlockState BED_FOOT = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.EAST);
    static final BlockState BED_HEAD = BED_FOOT.setValue(BedBlock.PART, BedPart.HEAD);

    static void door(GameTestHelper helper, BlockPos lower) {
        helper.setBlock(lower.below(), Blocks.STONE);
        helper.setBlock(lower, DOOR_LOWER);
        helper.setBlock(lower.above(), DOOR_UPPER);
    }

    /**
     * A bed facing east: its head is east of its foot.
     */
    static void bed(GameTestHelper helper, BlockPos foot) {
        helper.setBlock(foot, BED_FOOT);
        helper.setBlock(foot.east(), BED_HEAD);
    }

    static boolean told(List<Component> messages, String key) {
        return messages.stream().anyMatch(m -> m.getContents() instanceof TranslatableContents t && t.getKey().equals(key));
    }

    static void assertStands(GameTestHelper helper, BlockPos pos, BlockState state, String what) {
        assertTrue(helper, helper.getBlockState(pos) == state, what + " stands at " + pos + ": " + helper.getBlockState(pos));
    }

    @GameTest(template = "empty")
    public static void aCaptureThatWouldCutADoorInTwoIsRefused(GameTestHelper helper) {
        List<Component> messages = new ArrayList<>();
        ServerPlayer player = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(7, 1, 7), messages);
        // the cube is x, y, z 1..3: the lower half of the door is in its top layer, the upper half above it
        door(helper, new BlockPos(2, 3, 2));
        helper.setBlock(1, 1, 1, Blocks.GOLD_BLOCK);
        ItemStack capsule = CapsuleTestUtils.emptyCapsule(3);

        boolean captured = Capsule.captureAtPosition(capsule, player, 3, helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)));

        assertTrue(helper, !captured && CapsuleItem.hasState(capsule, CapsuleState.EMPTY), "the capture is refused and the capsule stays empty");
        assertStands(helper, new BlockPos(2, 3, 2), DOOR_LOWER, "the lower half");
        assertStands(helper, new BlockPos(2, 4, 2), DOOR_UPPER, "the upper half");
        helper.assertBlockPresent(Blocks.GOLD_BLOCK, 1, 1, 1);
        helper.assertBlockPresent(Blocks.STONE, 2, 2, 2);
        assertTrue(helper, told(messages, "capsule.error.cutsBlock"), "the player is told why: " + messages);
        CapsuleTestUtils.removePlayer(player);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aThrownCapsuleRefusesToCutADoorOnACaptureBase(GameTestHelper helper) {
        List<Component> messages = new ArrayList<>();
        ServerPlayer thrower = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(7, 1, 7), messages);
        BlockPos marker = new BlockPos(4, 1, 4);
        helper.setBlock(marker, CapsuleBlocks.CAPSULE_MARKER.get().defaultBlockState().setValue(BlockCapsuleMarker.FACING, Direction.UP));
        // the cube above the base is x, z 3..5 and y 2..4: the lower half of the door stands beside the base, its upper half in the cube
        door(helper, new BlockPos(3, 1, 4));
        helper.setBlock(4, 3, 4, Blocks.GOLD_BLOCK);
        ItemStack capsule = CapsuleTestUtils.emptyCapsule(3);
        CapsuleItem.setState(capsule, CapsuleState.EMPTY_ACTIVATED);
        Vec3 landing = helper.absoluteVec(new Vec3(4.5, 2, 4.5));
        ItemEntity thrown = new ItemEntity(helper.getLevel(), landing.x, landing.y, landing.z, capsule);
        thrown.setThrower(thrower);

        Capsule.handleItemEntityOnGround(thrown, capsule);

        assertTrue(helper, CapsuleItem.hasState(capsule, CapsuleState.EMPTY), "the capsule stays empty: " + CapsuleItem.getState(capsule));
        assertStands(helper, new BlockPos(3, 1, 4), DOOR_LOWER, "the lower half");
        assertStands(helper, new BlockPos(3, 2, 4), DOOR_UPPER, "the upper half");
        helper.assertBlockPresent(Blocks.GOLD_BLOCK, 4, 3, 4);
        assertTrue(helper, told(messages, "capsule.error.cutsBlock"), "the thrower is told why: " + messages);
        CapsuleTestUtils.removePlayer(thrower);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void anInstantCaptureRefusesHalfABed(GameTestHelper helper) {
        List<Component> messages = new ArrayList<>();
        ServerPlayer player = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(7, 1, 7), messages);
        bed(helper, new BlockPos(2, 1, 2));
        ItemStack capsule = CapsuleTestUtils.emptyCapsule(1);
        player.setItemInHand(InteractionHand.MAIN_HAND, capsule);

        Capsule.handleThrowQuery(player, helper.absolutePos(new BlockPos(3, 1, 2)), true);

        assertTrue(helper, CapsuleItem.hasState(capsule, CapsuleState.EMPTY), "the capsule stays empty: " + CapsuleItem.getState(capsule));
        assertStands(helper, new BlockPos(2, 1, 2), BED_FOOT, "the foot");
        assertStands(helper, new BlockPos(3, 1, 2), BED_HEAD, "the head");
        assertTrue(helper, told(messages, "capsule.error.cutsBlock"), "the player is told why: " + messages);
        CapsuleTestUtils.removePlayer(player);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aCubeHoldingBothPartsTakesAndDeploysThemTogether(GameTestHelper helper) {
        ServerPlayer player = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(7, 1, 0));
        door(helper, new BlockPos(1, 2, 1));
        bed(helper, new BlockPos(2, 1, 2));

        ItemStack capsule = CapsuleTestUtils.capture(helper, new BlockPos(1, 1, 1), 3);

        helper.assertBlockNotPresent(Blocks.OAK_DOOR, 1, 2, 1);
        helper.assertBlockNotPresent(Blocks.OAK_DOOR, 1, 3, 1);
        helper.assertBlockNotPresent(Blocks.RED_BED, 2, 1, 2);
        helper.assertBlockNotPresent(Blocks.RED_BED, 3, 1, 2);
        assertTrue(helper, CapsuleTestUtils.deploy(helper, capsule, new BlockPos(6, 0, 6), player), "the capsule deploys");
        // the content starts at (5, 1, 5)
        assertStands(helper, new BlockPos(5, 2, 5), DOOR_LOWER, "the deployed lower half");
        assertStands(helper, new BlockPos(5, 3, 5), DOOR_UPPER, "the deployed upper half");
        assertStands(helper, new BlockPos(6, 1, 6), BED_FOOT, "the deployed foot");
        assertStands(helper, new BlockPos(7, 1, 6), BED_HEAD, "the deployed head");
        CapsuleTestUtils.removePlayer(player);
        helper.succeed();
    }
}
