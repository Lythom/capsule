package capsule.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Helpers of the NeoForge mod GameTests.
 */
public class NeoForgeTestUtils {

    /**
     * The item handlers of the block at pos, unsided first.
     */
    public static Stream<IItemHandler> items(GameTestHelper helper, BlockPos pos) {
        return Stream.concat(Stream.of((Direction) null), Arrays.stream(Direction.values()))
                .map(side -> helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), side))
                .filter(Objects::nonNull);
    }

    public static int count(GameTestHelper helper, BlockPos pos, Item item) {
        return items(helper, pos).mapToInt(handler -> IntStream.range(0, handler.getSlots())
                .map(slot -> handler.getStackInSlot(slot).is(item) ? handler.getStackInSlot(slot).getCount() : 0).sum()).max().orElse(0);
    }

    /**
     * Places the item on top of the block at floor as the player would, so that blocks spanning several positions place
     * all of them.
     */
    public static void placeOn(GameTestHelper helper, ServerPlayer player, BlockPos floor, ItemStack stack) {
        BlockPos pos = helper.absolutePos(floor);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false)));
    }

}
