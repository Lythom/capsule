package capsule.gametest;

import capsule.CapsuleMod;
import capsule.enchantments.CapsuleEnchantments;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItem.CapsuleState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static capsule.gametest.CapsuleTestUtils.assertTrue;
import static capsule.gametest.CapsuleTestUtils.capture;

@GameTestHolder(CapsuleMod.MODID)
@PrefixGameTestTemplate(false)
public class RecallTests {

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void recallLetsAnEarlyCollidingCapsuleDeploy(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.GOLD_BLOCK);
        ItemStack capsule = capture(helper, new BlockPos(1, 1, 1), 1);
        capsule.enchant(helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(CapsuleEnchantments.RECALL), 1);
        CapsuleItem.setState(capsule, CapsuleState.ACTIVATED);
        CapsuleTestUtils.fill(helper, new BlockPos(0, 0, 0), new BlockPos(8, 0, 8), Blocks.STONE.defaultBlockState());
        helper.setBlock(5, 3, 5, Blocks.STONE);
        ServerPlayer player = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(7, 1, 7));

        // starts right under a ceiling so that it collides during its first tick
        Vec3 pos = helper.absoluteVec(new Vec3(5.5, 2.7, 5.5));
        ItemEntity entity = new ItemEntity(helper.getLevel(), pos.x, pos.y, pos.z, capsule, 0, 0.3, 0);
        entity.setThrower(player);
        helper.getLevel().addFreshEntity(entity);

        helper.succeedWhen(() -> {
            assertTrue(helper, BlockPos.betweenClosedStream(new BlockPos(0, 1, 0), new BlockPos(8, 4, 8)).anyMatch(p -> helper.getBlockState(p).is(Blocks.GOLD_BLOCK)), "capsule should deploy its gold block");
            CapsuleTestUtils.removePlayer(player);
        });
    }
}
