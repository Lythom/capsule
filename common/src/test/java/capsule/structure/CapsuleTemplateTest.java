package capsule.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CapsuleTemplateTest {

    @Test
    void rotationKeepsContentInsideTheCapsuleCube() {
        StructurePlaceSettings placement = new StructurePlaceSettings().setRotation(Rotation.CLOCKWISE_90);
        BlockPos corner = new BlockPos(0, 0, 0);

        BlockPos rotated = CapsuleTemplate.calculateRelativePosition(placement, corner).offset(CapsuleTemplate.recenterRotation(1, placement));

        assertEquals(new BlockPos(2, 0, 0), rotated);
    }

    @Test
    void mirrorFlipsOneAxis() {
        assertEquals(new BlockPos(-1, 2, 3), CapsuleTemplate.getTransformedPos(new BlockPos(1, 2, 3), Mirror.FRONT_BACK, Rotation.NONE, BlockPos.ZERO));
        assertEquals(new BlockPos(1, 2, -3), CapsuleTemplate.getTransformedPos(new BlockPos(1, 2, 3), Mirror.LEFT_RIGHT, Rotation.NONE, BlockPos.ZERO));
    }
}
