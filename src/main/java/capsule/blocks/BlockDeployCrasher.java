package capsule.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Will crash if placed at y > 70
 */
public class BlockDeployCrasher extends Block {
    public BlockDeployCrasher(String name, Material material) {
        super(material);
        this.setUnlocalizedName(name);
        this.setHardness(5);
        this.setResistance(1000);
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, BlockState state) {
        super.onBlockAdded(worldIn, pos, state);
        if (pos.getY() > 70) {
            throw new RuntimeException("testing purpose deploy crasher");
        }
    }
}
