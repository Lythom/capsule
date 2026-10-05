package capsule.gametest;

import capsule.helpers.Capsule;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItem.CapsuleState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import static capsule.gametest.CapsuleTestUtils.assertTrue;

/**
 * Waystones (#121): ghost blocks and broken doors were reported after capturing waystones on 1.20.1. On 1.21.1
 * Waystones tags its blocks c:relocation_not_supported, so capsules leave them in place.
 * Registered when Waystones is loaded, see docs/TESTING.md.
 */
public class WaystonesTests {

    private static final BlockPos WAYSTONE = new BlockPos(1, 1, 1);

    private static void placeDoubleBlock(GameTestHelper helper, BlockPos lower, BlockState state) {
        helper.setBlock(lower, state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
        helper.setBlock(lower.above(), state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
    }

    private static void assertDoubleBlock(GameTestHelper helper, BlockPos lower, Block block) {
        BlockState bottom = helper.getBlockState(lower), top = helper.getBlockState(lower.above());
        assertTrue(helper, bottom.is(block) && bottom.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER, "lower half of " + block + " expected at " + lower + ", got " + bottom);
        assertTrue(helper, top.is(block) && top.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER, "upper half of " + block + " expected at " + lower.above() + ", got " + top);
    }

    private static void assertWaystoneIntact(GameTestHelper helper, Block waystone, String step) {
        assertDoubleBlock(helper, WAYSTONE, waystone);
        assertTrue(helper, helper.getLevel().getBlockEntity(helper.absolutePos(WAYSTONE)) != null, "the waystone lost its block entity after " + step);
    }

    private static void waystoneStaysAndDoorMoves(GameTestHelper helper, boolean overpowered) {
        Block waystone = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("waystones", "waystone"));
        // doors need a floor
        CapsuleTestUtils.fill(helper, new BlockPos(1, 0, 1), new BlockPos(8, 0, 8), Blocks.STONE.defaultBlockState());
        placeDoubleBlock(helper, WAYSTONE, waystone.defaultBlockState());
        placeDoubleBlock(helper, new BlockPos(3, 1, 1), Blocks.OAK_DOOR.defaultBlockState());

        ItemStack capsule = Capsule.newEmptyCapsuleItemStack(0, 0, 5, overpowered, null, 0);
        assertTrue(helper, Capsule.captureAtPosition(capsule, null, 5, helper.getLevel(), helper.absolutePos(new BlockPos(0, 1, 0))), "capture should succeed");
        assertWaystoneIntact(helper, waystone, "capture");
        helper.assertBlockNotPresent(Blocks.OAK_DOOR, 3, 1, 1);
        helper.assertBlockNotPresent(Blocks.OAK_DOOR, 3, 2, 1);

        assertTrue(helper, CapsuleTestUtils.deploy(helper, capsule, new BlockPos(4, 0, 4), null), "deploy should succeed");
        helper.runAfterDelay(5, () -> {
            assertDoubleBlock(helper, new BlockPos(5, 1, 3), Blocks.OAK_DOOR);
            assertWaystoneIntact(helper, waystone, "deploy");

            Capsule.resentToCapsule(capsule, helper.getLevel(), null);
            assertTrue(helper, CapsuleItem.hasState(capsule, CapsuleState.LINKED), "undeploy should succeed");
            helper.runAfterDelay(5, () -> {
                BlockPos.betweenClosed(new BlockPos(2, 1, 2), new BlockPos(6, 5, 6)).forEach(p ->
                        assertTrue(helper, helper.getBlockState(p).isAir(), "ghost block after undeploy at " + p + ": " + helper.getBlockState(p)));
                assertWaystoneIntact(helper, waystone, "undeploy");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void waystoneStaysWhenCapturedWithADoor(GameTestHelper helper) {
        waystoneStaysAndDoorMoves(helper, false);
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void waystoneStaysWhenCapturedWithADoorByAnOverpoweredCapsule(GameTestHelper helper) {
        waystoneStaysAndDoorMoves(helper, true);
    }
}
