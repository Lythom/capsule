package capsule.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;

/**
 * Will crash if placed at y > 70
 */
public class BlockDeployCrasher extends Block {
    public BlockDeployCrasher() {
        super(Block.Properties.of(Material.STONE, MaterialColor.STONE)
                .sound(SoundType.STONE));
    }

    @Override
    public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(worldIn, pos, state, placer, stack);
        if (pos.getY() > 70) {
            throw new RuntimeException("testing purpose deploy crasher");
        }
    }
}
