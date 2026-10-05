package capsule.gametest;

import capsule.Config;
import capsule.StructureSaver;
import capsule.helpers.Capsule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static capsule.gametest.CapsuleTestUtils.assertTrue;

/**
 * Sophisticated Storage (#115): its storages keep the block entity tag they were loaded from as their content, so
 * starters sharing that tag with the cached reward template emptied each other.
 * Registered when Sophisticated Storage is loaded, see docs/TESTING.md.
 */
public class SophisticatedStorageTests {

    private static IItemHandler items(GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), null);
    }

    private static int diamonds(GameTestHelper helper, BlockPos pos) {
        IItemHandler handler = items(helper, pos);
        int count = 0;
        for (int slot = 0; handler != null && slot < handler.getSlots(); slot++) {
            if (handler.getStackInSlot(slot).is(Items.DIAMOND)) count += handler.getStackInSlot(slot).getCount();
        }
        return count;
    }

    @GameTest(template = "empty")
    public static void rewardBarrelsDoNotShareTheirContent(GameTestHelper helper) throws IOException {
        Block barrel = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("sophisticatedstorage", "barrel"));
        BlockPos source = new BlockPos(1, 1, 1);
        helper.setBlock(source, barrel);
        items(helper, source).insertItem(0, new ItemStack(Items.DIAMOND, 16), false);
        ItemStack linked = CapsuleTestUtils.capture(helper, source, 1);
        assertTrue(helper, helper.getBlockState(source).isAir(), "the barrel should be captured");

        String rewardPath = Config.rewardTemplatesPath + "/sophisticated_barrel_test";
        Path rewardFile = Path.of(rewardPath + ".nbt");
        Files.createDirectories(rewardFile.getParent());
        NbtIo.writeCompressed(StructureSaver.getTemplateNBTData(CapsuleTestUtils.template(helper, linked)), rewardFile);
        try {
            ItemStack reward = Capsule.newRewardCapsuleItemStack(rewardPath, 0, 0, 1, null, null);
            assertTrue(helper, CapsuleTestUtils.deploy(helper, reward, new BlockPos(4, 0, 4), null), "first deploy should succeed");
            BlockPos first = new BlockPos(4, 1, 4);
            assertTrue(helper, diamonds(helper, first) == 16, "the first barrel should hold 16 diamonds, got " + diamonds(helper, first));
            IItemHandler handler = items(helper, first);
            for (int slot = 0; slot < handler.getSlots(); slot++) handler.extractItem(slot, 64, false);

            assertTrue(helper, CapsuleTestUtils.deploy(helper, reward, new BlockPos(6, 0, 6), null), "second deploy should succeed");
            BlockPos second = new BlockPos(6, 1, 6);
            assertTrue(helper, diamonds(helper, second) == 16, "emptying the first barrel emptied the second one: " + diamonds(helper, second) + " diamonds");
        } finally {
            Files.deleteIfExists(rewardFile);
        }
        helper.succeed();
    }
}
