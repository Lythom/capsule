package capsule.gametest;

import capsule.blocks.BlockCapsuleMarker;
import capsule.blocks.BlockEntityCapture;
import capsule.blocks.CapsuleBlocks;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItem.CapsuleState;
import capsule.helpers.MinecraftNBT;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import static capsule.gametest.CapsuleTestUtils.assertTrue;

public class CaptureBaseTests {

    @CapsuleGameTest(template = "empty", timeoutTicks = 200)
    public static void freshCaptureBaseDispensesOnFirstSignal(GameTestHelper helper) {
        helper.setBlock(2, 1, 2, Blocks.STONE);
        helper.setBlock(2, 2, 2, Blocks.OAK_PLANKS);
        ItemStack capsule = CapsuleTestUtils.capture(helper, new BlockPos(1, 1, 1), 3);
        BlockPos markerPos = new BlockPos(5, 1, 5);
        helper.setBlock(markerPos, CapsuleBlocks.CAPSULE_MARKER.get().defaultBlockState().setValue(BlockCapsuleMarker.FACING, Direction.UP));
        BlockEntityCapture marker = helper.getBlockEntity(markerPos, BlockEntityCapture.class);
        marker.setItem(0, capsule);
        BlockPos powerPos = markerPos.east();

        helper.startSequence()
                .thenExecute(() -> helper.setBlock(powerPos, Blocks.REDSTONE_BLOCK))
                .thenWaitUntil(() -> {
                    helper.assertBlockPresent(Blocks.STONE, 5, 2, 5);
                    helper.assertBlockPresent(Blocks.OAK_PLANKS, 5, 3, 5);
                })
                .thenExecute(() -> {
                    assertTrue(helper, CapsuleItem.hasState(marker.getItem(0), CapsuleState.DEPLOYED), "dispensed capsule should be deployed");
                    helper.setBlock(powerPos, Blocks.AIR);
                })
                .thenIdle(5)
                .thenExecute(() -> helper.setBlock(powerPos, Blocks.REDSTONE_BLOCK))
                .thenWaitUntil(() -> helper.assertBlockNotPresent(Blocks.STONE, 5, 2, 5))
                .thenExecute(() -> {
                    assertTrue(helper, CapsuleItem.hasState(marker.getItem(0), CapsuleState.LINKED), "dispensed capsule should be linked again");
                    helper.assertBlockNotPresent(Blocks.OAK_PLANKS, 5, 3, 5);
                })
                .thenSucceed();
    }

    /**
     * A capture base saved on Minecraft 1.21.1 holding a capsule: the data fixers leave the items of a modded block
     * entity in the 1.21.1 format, whose dyed_color and enchantments no longer decode.
     */
    @CapsuleGameTest(template = "empty")
    public static void captureBaseOf1211KeepsItsCapsule(GameTestHelper helper) throws Exception {
        BlockPos markerPos = new BlockPos(4, 1, 4);
        helper.setBlock(markerPos, CapsuleBlocks.CAPSULE_MARKER.get().defaultBlockState().setValue(BlockCapsuleMarker.FACING, Direction.UP));
        BlockEntityCapture marker = helper.getBlockEntity(markerPos, BlockEntityCapture.class);
        CompoundTag saved = TagParser.parseCompoundFully("{id:\"capsule:capsulemarker_te\",placer:[I;1,2,3,4],Items:[{Slot:0b,count:1,id:\"capsule:capsule\","
                + "components:{\"minecraft:custom_data\":{state:2,size:3,structureName:\"c-test-player-0\"},"
                + "\"minecraft:dyed_color\":{rgb:1122867,show_in_tooltip:0b},\"minecraft:enchantments\":{levels:{\"minecraft:loyalty\":2}}}}]}");
        marker.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));

        ItemStack capsule = marker.getItem(0);
        int loyalty = capsule.getEnchantments().getLevel(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOYALTY));
        assertTrue(helper, capsule.getItem() instanceof CapsuleItem && CapsuleItem.hasState(capsule, CapsuleState.LINKED)
                && "c-test-player-0".equals(CapsuleItem.getStructureName(capsule)), "the base should keep its linked capsule, got " + capsule);
        assertTrue(helper, MinecraftNBT.hasColor(capsule) && MinecraftNBT.getColor(capsule) == 1122867, "the capsule should keep its color, got " + capsule.getComponentsPatch());
        assertTrue(helper, loyalty == 2, "the capsule should keep Loyalty II, got " + capsule.getEnchantments());
        assertTrue(helper, marker.getPlacer() != null, "the base should keep its placer");
        helper.succeed();
    }
}
