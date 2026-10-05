package capsule.gametest;

import capsule.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

import static capsule.gametest.CapsuleTestUtils.capture;

public class ConfigTests {

    @GameTest(template = "empty")
    public static void configuredTagsExcludeBlocksFromCapture(GameTestHelper helper) {
        List<TagKey<Block>> configured = Config.excludedBlockTags;
        Config.excludedBlockTags = List.of(BlockTags.LOGS);
        try {
            helper.setBlock(1, 1, 1, Blocks.OAK_LOG);
            helper.setBlock(1, 2, 1, Blocks.STONE);
            capture(helper, new BlockPos(0, 1, 0), 3);
        } finally {
            Config.excludedBlockTags = configured;
        }

        helper.assertBlockPresent(Blocks.OAK_LOG, 1, 1, 1);
        helper.assertBlockNotPresent(Blocks.STONE, 1, 2, 1);
        helper.succeed();
    }
}
