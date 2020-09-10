package capsule.blocks;

import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ContainerBlock;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockCapsuleMarker extends ContainerBlock {

    /**
     * Whether this fence connects in the northern direction
     */
    public static final PropertyBool PROJECTING = PropertyBool.create("projecting");

    public BlockCapsuleMarker(String unlocalizedName, Material materialIn) {
        super(materialIn);
        this.setDefaultState(this.blockState.getBaseState().withProperty(PROJECTING, Boolean.FALSE));
        this.setUnlocalizedName(unlocalizedName);
        this.setHardness(5);
        this.setResistance(1000);
        this.setHarvestLevel("pickaxe", 0);

    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityCapture();
    }

    /**
     * Get the actual Block state of this Block at the given position. This
     * applies properties not visible in the metadata, such as fence
     * connections.
     */
    public BlockState getActualState(BlockState state, IBlockAccess worldIn, BlockPos pos) {

        TileEntity tileentity = worldIn instanceof ChunkCache ? ((ChunkCache)worldIn).getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK) : worldIn.getTileEntity(pos);
        return state.withProperty(PROJECTING, this.isProjecting((TileEntityCapture) tileentity));
    }

    public Boolean isProjecting(TileEntityCapture tec) {
        return tec != null && tec.getTileData().getInt("size") > 0;
    }

    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, new IProperty[]{PROJECTING});
    }

    public int getMetaFromState(BlockState state) {
        return 0;
    }

    @OnlyIn(Dist.CLIENT)
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT_MIPPED;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

}
