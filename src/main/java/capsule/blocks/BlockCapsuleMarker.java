package capsule.blocks;

import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.IProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockReader;
import net.minecraftforge.common.ToolType;

public class BlockCapsuleMarker extends ContainerBlock {
//
//    TODO lighten block when empty capsle in hand
//    TODO TESR should have depth
//    TODO Test item rendering override
//
    /**
     * Whether this fence connects in the northern direction
     */
    public static final IProperty<Boolean> PROJECTING = BooleanProperty.create("projecting");

    public BlockCapsuleMarker() {
        super(Block.Properties.create(Material.ROCK, MaterialColor.STONE)
                .hardnessAndResistance(5.0F, 1000.0F)
                .sound(SoundType.STONE)
                .harvestTool(ToolType.PICKAXE)
                .harvestLevel(0));

        this.setDefaultState(this.stateContainer.getBaseState()
                .with(PROJECTING, Boolean.FALSE));
    }

    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(PROJECTING);
    }

    @Override
    public TileEntity createNewTileEntity(IBlockReader worldIn) {
        return new TileEntityCapture();
    }
}
