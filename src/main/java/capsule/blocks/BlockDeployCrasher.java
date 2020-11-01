package capsule.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;

/**
 * Will crash if placed at y > 70
 */
public class BlockDeployCrasher extends Block {
    public BlockDeployCrasher() {
        super(Block.Properties.create(Material.ROCK, MaterialColor.STONE)
                .hardnessAndResistance(5.0F, 1000.0F)
                .sound(SoundType.STONE)
                .harvestTool(ToolType.PICKAXE)
                .harvestLevel(0));
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
        if (pos.getY() > 70) {
            throw new RuntimeException("testing purpose deploy crasher");
        }
    }
}
