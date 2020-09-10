package capsule.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockCaptureCrasher extends Block {
    public BlockCaptureCrasher(String name, Material material) {
        super(material);
        this.setUnlocalizedName(name);
        this.setHardness(5);
        this.setResistance(1000);
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, BlockState state) {
        super.breakBlock(worldIn, pos, state);
        throw new RuntimeException("testing purpose");
    }
}
