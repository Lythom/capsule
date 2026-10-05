package capsule.gametest;

import capsule.Config;
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
import static capsule.gametest.CapsuleTestUtils.capture;
import static capsule.gametest.CapsuleTestUtils.deploy;

public class UndeployDelayTests {

    private static ItemStack deployedInstantCapsule(GameTestHelper helper, ServerPlayer player) {
        helper.setBlock(1, 1, 1, Blocks.GOLD_BLOCK);
        ItemStack capsule = capture(helper, new BlockPos(1, 1, 1), 1);
        deploy(helper, capsule, new BlockPos(4, 0, 4), player);
        player.setItemInHand(InteractionHand.MAIN_HAND, capsule);
        return capsule;
    }

    @GameTest(template = "empty")
    public static void instantCapsuleUndeploysAfterRelog(GameTestHelper helper) {
        ServerPlayer player = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(7, 1, 7));
        player.tickCount = 10_000;
        ItemStack capsule = deployedInstantCapsule(helper, player);
        CapsuleItem.setUndeployDelay(capsule, helper.getLevel());
        CapsuleTestUtils.removePlayer(player);

        player.tickCount = 0; // a new ServerPlayer instance after relog, respawn or restart
        helper.runAfterDelay(10, () -> {
            capsule.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            assertTrue(helper, CapsuleItem.hasState(capsule, CapsuleState.LINKED), "capsule should be undeployed");
            helper.assertBlockNotPresent(Blocks.GOLD_BLOCK, 4, 1, 4);
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void legacyUndeployDelayDoesNotBlockUndeploy(GameTestHelper helper) {
        ServerPlayer player = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(7, 1, 7));
        ItemStack capsule = deployedInstantCapsule(helper, player);
        NBTHelper.updateTag(capsule, tag -> tag.putInt("undeployDelay", 1_000_000));
        CapsuleTestUtils.removePlayer(player);

        capsule.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        assertTrue(helper, CapsuleItem.hasState(capsule, CapsuleState.LINKED), "capsule should be undeployed");
        helper.assertBlockNotPresent(Blocks.GOLD_BLOCK, 4, 1, 4);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void activatedCapsuleTimesOutAfterRelog(GameTestHelper helper) {
        ServerPlayer player = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(7, 1, 7));
        player.tickCount = 10_000;
        helper.setBlock(1, 1, 1, Blocks.GOLD_BLOCK);
        ItemStack capsule = capture(helper, new BlockPos(0, 1, 0), 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, capsule);
        capsule.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        assertTrue(helper, CapsuleItem.hasState(capsule, CapsuleState.ACTIVATED), "capsule should be activated");

        player.tickCount = 0;
        helper.runAfterDelay(Config.previewDisplayDuration + 5, () -> {
            capsule.inventoryTick(helper.getLevel(), player, 0, true);
            assertTrue(helper, CapsuleItem.hasState(capsule, CapsuleState.LINKED), "activation should time out");
            CapsuleTestUtils.removePlayer(player);
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void instantCapsuleCannotUndeployInstantly(GameTestHelper helper) {
        ServerPlayer player = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(7, 1, 7));
        ItemStack capsule = deployedInstantCapsule(helper, player);
        CapsuleItem.setUndeployDelay(capsule, helper.getLevel());
        CapsuleTestUtils.removePlayer(player);

        capsule.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        assertTrue(helper, CapsuleItem.hasState(capsule, CapsuleState.DEPLOYED), "the click that deployed must not undeploy");
        helper.succeed();
    }
}
