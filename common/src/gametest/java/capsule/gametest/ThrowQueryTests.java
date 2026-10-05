package capsule.gametest;

import capsule.helpers.Capsule;
import capsule.helpers.NBTHelper;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItem.CapsuleState;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import static capsule.gametest.CapsuleTestUtils.assertTrue;

public class ThrowQueryTests {

    private static ServerPlayer playerHolding(GameTestHelper helper, ItemStack capsule) {
        ServerPlayer player = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(7, 1, 7));
        player.setItemInHand(InteractionHand.MAIN_HAND, capsule);
        return player;
    }

    @GameTest(template = "empty")
    public static void instantCaptureWorksAtPreviewRange(GameTestHelper helper) {
        helper.setBlock(2, 1, 2, Blocks.STONE);
        ItemStack capsule = CapsuleTestUtils.emptyCapsule(1);
        ServerPlayer player = playerHolding(helper, capsule);

        Capsule.handleThrowQuery(player, helper.absolutePos(new BlockPos(2, 1, 2)), true);

        assertTrue(helper, CapsuleItem.hasState(capsule, CapsuleState.LINKED), "instant capture should succeed");
        helper.assertBlockNotPresent(Blocks.STONE, 2, 1, 2);
        CapsuleTestUtils.removePlayer(player);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void instantQueryIsRefusedForNonInstantCapsules(GameTestHelper helper) {
        helper.setBlock(2, 1, 2, Blocks.STONE);
        ItemStack capsule = CapsuleTestUtils.emptyCapsule(3);
        ServerPlayer player = playerHolding(helper, capsule);

        Capsule.handleThrowQuery(player, helper.absolutePos(new BlockPos(1, 1, 1)), true);

        assertTrue(helper, CapsuleItem.hasState(capsule, CapsuleState.EMPTY), "a 3x3x3 capsule must not capture instantly");
        helper.assertBlockPresent(Blocks.STONE, 2, 1, 2);
        CapsuleTestUtils.removePlayer(player);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void instantQueryIsRefusedOutOfRange(GameTestHelper helper) {
        helper.setBlock(2, 1, 2, Blocks.STONE);
        ItemStack capsule = CapsuleTestUtils.emptyCapsule(1);
        ServerPlayer player = playerHolding(helper, capsule);
        player.teleportTo(player.getX() + 100, player.getY(), player.getZ());

        Capsule.handleThrowQuery(player, helper.absolutePos(new BlockPos(2, 1, 2)), true);

        assertTrue(helper, CapsuleItem.hasState(capsule, CapsuleState.EMPTY), "a block 100 blocks away must not be captured");
        helper.assertBlockPresent(Blocks.STONE, 2, 1, 2);
        CapsuleTestUtils.removePlayer(player);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void throwOutOfRangeFallsWhereItLands(GameTestHelper helper) {
        ItemStack capsule = CapsuleTestUtils.emptyCapsule(3);
        ServerPlayer player = playerHolding(helper, capsule);

        Capsule.handleThrowQuery(player, helper.absolutePos(new BlockPos(2, 1, 2)).offset(100, 0, 0), false);

        assertTrue(helper, player.getMainHandItem().isEmpty(), "the capsule should be thrown");
        assertTrue(helper, !NBTHelper.getOrCreateTag(capsule).contains("deployAt"), "an out of range target must be ignored");
        CapsuleTestUtils.removePlayer(player);
        helper.killAllEntities();
        helper.succeed();
    }
}
