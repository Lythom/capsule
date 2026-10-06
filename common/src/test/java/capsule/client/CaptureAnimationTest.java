package capsule.client;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaptureAnimationTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    /**
     * Stone where solid, air elsewhere, counting the blocks read.
     */
    static class CountingLevel implements BlockGetter {
        final Predicate<BlockPos> solid;
        int reads = 0;

        CountingLevel(Predicate<BlockPos> solid) {
            this.solid = solid;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            reads++;
            return solid.test(pos) ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState();
        }

        @Override
        public BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return getBlockState(pos).getFluidState();
        }

        @Override
        public int getHeight() {
            return 384;
        }

        @Override
        public int getMinBuildHeight() {
            return -64;
        }
    }

    @Test
    void smallCapturesKeepTheirBlocks() {
        CountingLevel level = new CountingLevel(pos -> pos.getY() == 0);
        Map<BlockPos, BlockState> standing = CaptureAnimation.standing(level, BlockPos.ZERO, 3);

        assertEquals(9, standing.size());
    }

    @Test
    void largeCapturesStopReadingPastTheBlockLimit() {
        CountingLevel level = new CountingLevel(pos -> true);
        Map<BlockPos, BlockState> standing = CaptureAnimation.standing(level, BlockPos.ZERO, 31);

        assertTrue(standing.isEmpty());
        assertTrue(level.reads <= CaptureAnimation.MAX_BLOCKS + 1, "read " + level.reads + " blocks");
    }

    @Test
    void hugeCapturesAreNotRead() {
        CountingLevel level = new CountingLevel(pos -> false);
        Map<BlockPos, BlockState> standing = CaptureAnimation.standing(level, BlockPos.ZERO, 255);

        assertTrue(standing.isEmpty());
        assertEquals(0, level.reads, "read " + level.reads + " blocks");
    }
}
