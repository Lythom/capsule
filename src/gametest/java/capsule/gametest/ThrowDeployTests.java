package capsule.gametest;

import capsule.CapsuleMod;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItem.CapsuleState;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static capsule.gametest.CapsuleTestUtils.capture;

@GameTestHolder(CapsuleMod.MODID)
@PrefixGameTestTemplate(false)
public class ThrowDeployTests {

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void blindThrowDeploysOnTheGround(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.GOLD_BLOCK);
        ItemStack capsule = capture(helper, new BlockPos(1, 1, 1), 1);
        CapsuleItem.setState(capsule, CapsuleState.ACTIVATED);
        CapsuleTestUtils.fill(helper, new BlockPos(0, 0, 0), new BlockPos(8, 0, 8), Blocks.STONE.defaultBlockState());

        Vec3 pos = helper.absoluteVec(new Vec3(4.3, 1.5, 4.3));
        helper.getLevel().addFreshEntity(new ItemEntity(helper.getLevel(), pos.x, pos.y, pos.z, capsule, 0, 0, 0));

        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.GOLD_BLOCK, 4, 1, 4);
            helper.assertBlockNotPresent(Blocks.GOLD_BLOCK, 4, 2, 4);
        });
    }
}
