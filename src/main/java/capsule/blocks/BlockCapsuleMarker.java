package capsule.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ToolType;

public class BlockCapsuleMarker extends DispenserBlock {
    public BlockCapsuleMarker() {
        super(BlockCapsuleMarker.Properties.of(Material.STONE, MaterialColor.STONE)
                .strength(3.5F)
                .sound(SoundType.STONE)
                .harvestTool(ToolType.PICKAXE)
                .harvestLevel(0));

        this.registerDefaultState(this.stateDefinition.any());
    }

    @Override
    public BlockEntity newBlockEntity(BlockGetter worldIn) {
        return new TileEntityCapture();
    }

    public void neighborChanged(BlockState p_220069_1_, Level p_220069_2_, BlockPos p_220069_3_, Block p_220069_4_, BlockPos p_220069_5_, boolean p_220069_6_) {
        boolean flag = p_220069_2_.hasNeighborSignal(p_220069_3_) || p_220069_2_.hasNeighborSignal(p_220069_3_.above());
        boolean flag1 = p_220069_1_.getValue(TRIGGERED);
        if (flag && !flag1) {
            p_220069_2_.getBlockTicks().scheduleTick(p_220069_3_, this, 4);
            p_220069_2_.setBlock(p_220069_3_, p_220069_1_.setValue(TRIGGERED, Boolean.valueOf(true)), 4);
        } else if (!flag && flag1) {
            p_220069_2_.setBlock(p_220069_3_, p_220069_1_.setValue(TRIGGERED, Boolean.valueOf(false)), 4);
        }
    }
}
