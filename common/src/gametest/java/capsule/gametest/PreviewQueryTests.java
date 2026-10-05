package capsule.gametest;

import capsule.items.CapsuleItem;
import capsule.network.handler.ServerPayloadHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import static capsule.gametest.CapsuleTestUtils.assertTrue;

public class PreviewQueryTests {

    /**
     * A client taking another capsule in hand asks its preview before the server knows the new held item: the answer
     * must be the template of the asked capsule, not of the one still held on the server.
     */
    @GameTest(template = "empty")
    public static void previewAnswersTheAskedCapsule(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.STONE);
        helper.setBlock(5, 1, 5, Blocks.GOLD_BLOCK);
        ItemStack stone = CapsuleTestUtils.capture(helper, new BlockPos(1, 1, 1), 1);
        ItemStack gold = CapsuleTestUtils.capture(helper, new BlockPos(5, 1, 5), 1);
        ServerPlayer player = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(7, 1, 7));
        player.setItemInHand(InteractionHand.MAIN_HAND, stone);
        player.getInventory().setItem(5, gold);

        ItemStack previewed = ServerPayloadHandler.previewedCapsule(player, CapsuleItem.getStructureName(gold));
        assertTrue(helper, previewed == gold, "the preview of the gold capsule should use the gold capsule, got " + previewed);
        assertTrue(helper, ServerPayloadHandler.previewedCapsule(player, "unknown") == null, "a template the player has no capsule of is not previewed");
        CapsuleTestUtils.removePlayer(player);
        helper.succeed();
    }
}
