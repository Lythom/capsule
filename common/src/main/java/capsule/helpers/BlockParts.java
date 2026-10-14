package capsule.helpers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Blocks made of two parts, each a block of its own: the halves of a door, a tall plant or a two-tall modded machine
 * (DOUBLE_BLOCK_HALF), the head and the foot of a bed (BED_PART). A capture never takes one part without the other.
 */
public final class BlockParts {

    private BlockParts() {
    }

    /**
     * The position of the other part of the block at pos, when the block has two parts and the other one stands there.
     */
    @Nullable
    public static BlockPos otherPart(BlockGetter level, BlockPos pos, BlockState state) {
        Property<?> part;
        BlockPos other;
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            part = BlockStateProperties.DOUBLE_BLOCK_HALF;
            other = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
        } else if (state.hasProperty(BlockStateProperties.BED_PART) && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            part = BlockStateProperties.BED_PART;
            other = pos.relative(BedBlock.getConnectedDirection(state));
        } else {
            return null;
        }
        BlockState otherState = level.getBlockState(other);
        return otherState.is(state.getBlock()) && otherState.getValue(part) != state.getValue(part) ? other : null;
    }

    /**
     * The first of the positions whose block has its other part outside the positions: taking them would cut that block
     * in two. Null when every block keeps its parts together.
     */
    @Nullable
    public static BlockPos firstCut(BlockGetter level, Collection<BlockPos> positions) {
        Set<BlockPos> taken = new HashSet<>(positions);
        for (BlockPos pos : positions) {
            BlockPos other = otherPart(level, pos, level.getBlockState(pos));
            if (other != null && !taken.contains(other)) return pos;
        }
        return null;
    }
}
