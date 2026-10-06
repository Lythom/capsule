package capsule.gametest;

import capsule.helpers.Capsule;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;

import java.util.List;

import static capsule.gametest.CapsuleTestUtils.capture;

/**
 * What a claim mod must veto, given a claim of owner where member may build: inside it, a stranger's capture, the
 * capture base the stranger placed, a capture base placed before Capsule 9 and a dispenser are refused, the member's
 * and owner's capture and the owner's base are allowed; outside it, the stranger's capture and nobody's (a dispenser,
 * the capture of a test capsule) are allowed.
 */
record ClaimModScenario(ServerPlayer owner, ServerPlayer member, ServerPlayer stranger,
                        BlockPos strangerInside, BlockPos memberInside, BlockPos ownerInside, BlockPos outside,
                        BlockPos ownerBase, BlockPos strangerBase, BlockPos legacyBase, BlockPos dispenserInside,
                        BlockPos dispenserOutside) {

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
        List<BlockPos> machines = List.of(ownerBase, strangerBase, legacyBase, dispenserInside, dispenserOutside);
        for (BlockPos machine : machines) {
            helper.setBlock(outside, Blocks.STONE);
            ItemStack capsule = capture(helper, outside, 1);
            helper.assertBlockNotPresent(Blocks.STONE, outside);
            DispenserBlockEntity blockEntity = machine == ownerBase ? ClaimTests.captureBase(helper, machine, owner)
                    : machine == strangerBase ? ClaimTests.captureBase(helper, machine, stranger)
                    : machine == legacyBase ? ClaimTests.captureBase(helper, machine, null)
                    : ClaimTests.dispenser(helper, machine);
            blockEntity.setItem(0, capsule);
        }
        helper.startSequence()
                .thenExecute(() -> machines.forEach(machine -> helper.setBlock(machine.below(), Blocks.REDSTONE_BLOCK)))
                .thenWaitUntil(() -> {
                    helper.assertBlockPresent(Blocks.STONE, ownerBase.above());
                    helper.assertBlockPresent(Blocks.STONE, dispenserOutside.above());
                })
                .thenIdle(10)
                .thenExecute(() -> {
                    cleanup.run();
                    for (BlockPos refused : List.of(strangerBase, legacyBase, dispenserInside)) {
                        helper.assertBlockNotPresent(Blocks.STONE, refused.above());
                        CapsuleTestUtils.assertTrue(helper, ((DispenserBlockEntity) helper.getBlockEntity(refused)).getItem(0).getCount() == 1, "the refused capsule stays in the machine");
                    }
                })
                .thenSucceed();
    }

    private static void captureOne(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
        helper.setBlock(pos, Blocks.STONE);
        Capsule.captureAtPosition(CapsuleTestUtils.emptyCapsule(1), player, 1, helper.getLevel(), helper.absolutePos(pos));
    }
}
