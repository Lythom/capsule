package capsule.blocks;

import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.IProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockReader;
import net.minecraftforge.common.ToolType;

public class BlockCapsuleMarker extends Block implements ITileEntityProvider {
//
//    TODO lighten block when empty capsle in hand
//    TODO already there positions ignored :(
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

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(PROJECTING);
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Override
    public TileEntity createNewTileEntity(IBlockReader worldIn) {
        return new TileEntityCapture();
    }

    @Override
    public BlockState getStateAtViewpoint(BlockState state, IBlockReader world, BlockPos pos, Vec3d viewpoint) {
        TileEntity tileentity = world.getTileEntity(pos);
        return state.with(PROJECTING, this.isProjecting((TileEntityCapture) tileentity));
    }

    public Boolean isProjecting(TileEntityCapture tec) {
        return tec != null && tec.getTileData().getInt("size") > 0;
    }
}
