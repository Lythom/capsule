package capsule.gametest;

import capsule.blocks.BlockEntityCapture;
import capsule.helpers.Capsule;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

import static capsule.gametest.CapsuleTestUtils.capture;

/**
 * What a claim mod must veto, given a claim of owner where member may build: inside it, a stranger's capture and the
 * capture base the stranger placed are refused, the member's and owner's capture and the owner's base are allowed;
 * outside it, the stranger's capture is allowed.
 */
record ClaimModScenario(ServerPlayer owner, ServerPlayer member, ServerPlayer stranger,
                        BlockPos strangerInside, BlockPos memberInside, BlockPos ownerInside, BlockPos outside,
                        BlockPos ownerBase, BlockPos strangerBase) {

    void run(GameTestHelper helper, Runnable cleanup) {
        // they played before: the fake players acting for their capture bases are known to the claim mods
        GameProfileCache profiles = helper.getLevel().getServer().getProfileCache();
        if (profiles != null) List.of(owner, member, stranger).forEach(player -> profiles.add(player.getGameProfile()));
        try {
            captureOne(helper, stranger, strangerInside);
            captureOne(helper, member, memberInside);
            captureOne(helper, owner, ownerInside);
            captureOne(helper, stranger, outside);
            helper.assertBlockPresent(Blocks.STONE, strangerInside);
            helper.assertBlockNotPresent(Blocks.STONE, memberInside);
            helper.assertBlockNotPresent(Blocks.STONE, ownerInside);
            helper.assertBlockNotPresent(Blocks.STONE, outside);
        } catch (RuntimeException e) {
            cleanup.run();
            throw e;
        }
        for (BlockPos base : List.of(ownerBase, strangerBase)) {
            helper.setBlock(outside, Blocks.STONE);
            ItemStack capsule = capture(helper, outside, 1);
            ClaimTests.captureBase(helper, base, base == ownerBase ? owner : stranger).setItem(0, capsule);
        }
        helper.startSequence()
                .thenExecute(() -> List.of(ownerBase, strangerBase).forEach(base -> helper.setBlock(base.below(), Blocks.REDSTONE_BLOCK)))
                .thenWaitUntil(() -> helper.assertBlockPresent(Blocks.STONE, ownerBase.above()))
                .thenIdle(10)
                .thenExecute(() -> {
                    cleanup.run();
                    helper.assertBlockNotPresent(Blocks.STONE, strangerBase.above());
                    CapsuleTestUtils.assertTrue(helper, ((BlockEntityCapture) helper.getBlockEntity(strangerBase)).getItem(0).getCount() == 1, "the refused capsule stays in the base");
                })
                .thenSucceed();
    }

    private static void captureOne(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
        helper.setBlock(pos, Blocks.STONE);
        Capsule.captureAtPosition(CapsuleTestUtils.emptyCapsule(1), player, 1, helper.getLevel(), helper.absolutePos(pos));
    }
}
