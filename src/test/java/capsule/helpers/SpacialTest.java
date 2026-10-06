package capsule.helpers;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpacialTest {

    @Test
    void aCapsuleOnTheGroundDeploysOnTheBlockItRestsOn() {
        // a capsule resting on the ground block at y=1 is exactly at y=2
        assertEquals(new BlockPos(4, 1, 4), Spacial.findBottomBlock(4.3, 2.0, 4.7));
        assertEquals(new BlockPos(4, 1, 4), Spacial.findBottomBlock(4.8, 2.0, 4.2));
    }

    @Test
    void aCapsuleOnASlabDeploysOnTheSlab() {
        assertEquals(new BlockPos(4, 2, 4), Spacial.findBottomBlock(4.5, 2.5, 4.5));
    }
}
