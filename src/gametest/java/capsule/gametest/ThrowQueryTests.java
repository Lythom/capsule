package capsule.gametest;

import capsule.CapsuleMod;
import capsule.helpers.Capsule;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItem.CapsuleState;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static capsule.gametest.CapsuleTestUtils.assertTrue;

@GameTestHolder(CapsuleMod.MODID)
@PrefixGameTestTemplate(false)
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
}
