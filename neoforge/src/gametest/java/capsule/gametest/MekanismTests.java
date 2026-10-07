package capsule.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import static capsule.gametest.CapsuleTestUtils.assertNeverCaptured;
import static capsule.gametest.CapsuleTestUtils.assertTrue;
import static capsule.gametest.CapsuleTestUtils.block;
import static capsule.gametest.CapsuleTestUtils.item;

/**
 * Mekanism: a moved Digital Miner could not be broken and stopped working, its bounding blocks pointing to its old
 * position. Mekanism tags the miner and its bounding blocks c:relocation_not_supported, which Capsule excludes; a bin
 * moves with its content.
 * Registered when Mekanism is loaded (-Pincompat), see docs/TESTING.md.
 */
public class MekanismTests {

    @GameTest(template = "empty")
    public static void digitalMinerIsNeverCaptured(GameTestHelper helper) {
        CapsuleTestUtils.fill(helper, new BlockPos(0, 0, 0), new BlockPos(8, 0, 8), Blocks.STONE.defaultBlockState());
        ServerPlayer player = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(4, 1, 1));
        NeoForgeTestUtils.placeOn(helper, player, new BlockPos(4, 0, 4), new ItemStack(item("mekanism:digital_miner")));
        CapsuleTestUtils.removePlayer(player);
        BlockPos[] miner = BlockPos.betweenClosedStream(new BlockPos(0, 1, 0), new BlockPos(8, 8, 8))
                .filter(pos -> BuiltInRegistries.BLOCK.getKey(helper.getBlockState(pos).getBlock()).getNamespace().equals("mekanism"))
                .map(BlockPos::immutable).toArray(BlockPos[]::new);
        assertTrue(helper, miner.length > 1, "the digital miner should be placed with its bounding blocks, got " + miner.length + " blocks");
        assertNeverCaptured(helper, new BlockPos(0, 1, 0), 7, miner);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mekanismBinMovesWithItsContent(GameTestHelper helper) {
        BlockPos bin = new BlockPos(1, 1, 1), moved = new BlockPos(4, 1, 4);
        helper.setBlock(bin, block("mekanism:basic_bin"));
        assertTrue(helper, NeoForgeTestUtils.items(helper, bin).anyMatch(handler -> ItemHandlerHelper.insertItem(handler, new ItemStack(Items.COBBLESTONE, 64), false).isEmpty()), "the bin refused cobblestone");
        ItemStack capsule = CapsuleTestUtils.capture(helper, bin, 1);
        assertTrue(helper, CapsuleTestUtils.deploy(helper, capsule, moved.below(), null), "deploy should succeed");
        int count = NeoForgeTestUtils.count(helper, moved, Items.COBBLESTONE);
        assertTrue(helper, count == 64, "the deployed bin holds " + count + " of its 64 cobblestone");
        helper.succeed();
    }
}
