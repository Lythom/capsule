package capsule.gametest;

import capsule.CapsuleMod;
import capsule.helpers.Spacial;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static capsule.gametest.CapsuleTestUtils.assertTrue;
import static capsule.gametest.CapsuleTestUtils.capture;
import static capsule.gametest.CapsuleTestUtils.deploy;

@GameTestHolder(CapsuleMod.MODID)
@PrefixGameTestTemplate(false)
public class DeployPositionTests {

    private static BlockPos aimTop(GameTestHelper helper, BlockPos relative) {
        BlockPos pos = helper.absolutePos(relative);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        return Spacial.getDeployPosition(helper.getLevel(), hit).subtract(helper.absolutePos(BlockPos.ZERO));
    }

    @GameTest(template = "empty")
    public static void deployReplacesTheAimedSnowLayer(GameTestHelper helper) {
        helper.setBlock(4, 1, 4, Blocks.STONE);
        helper.setBlock(4, 2, 4, Blocks.SNOW);
        helper.setBlock(6, 1, 6, Blocks.STONE);
        BlockPos onSnow = aimTop(helper, new BlockPos(4, 2, 4));
        BlockPos onStone = aimTop(helper, new BlockPos(6, 1, 6));

        assertTrue(helper, onSnow.equals(new BlockPos(4, 2, 4)), "aiming at a snow layer should deploy in place of it, got " + onSnow);
        assertTrue(helper, onStone.equals(new BlockPos(6, 2, 6)), "aiming at stone should deploy above it, got " + onStone);

        helper.setBlock(1, 1, 1, Blocks.GOLD_BLOCK);
        ItemStack capsule = capture(helper, new BlockPos(1, 1, 1), 1);
        assertTrue(helper, deploy(helper, capsule, onSnow.below(), null), "deploy should replace the snow layer");
        helper.assertBlockPresent(Blocks.GOLD_BLOCK, 4, 2, 4);
        helper.succeed();
    }
}
