/**
 * 
 */
package capsule.blocks;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author Samuel
 *
 */
public class BlockCapsuleMarker extends BlockContainer {

	/** Whether this fence connects in the northern direction */
	public static final PropertyBool PROJECTING = PropertyBool.create("projecting");

	/**
	 * @param materialIn
	 */
	public BlockCapsuleMarker(String unlocalizedName, Material materialIn) {
		super(materialIn);
		this.setDefaultState(this.blockState.getBaseState().withProperty(PROJECTING, Boolean.valueOf(false)));
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
	public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
		
		return state.withProperty(PROJECTING, this.isProjecting((TileEntityCapture)worldIn.getTileEntity(pos)));
	}
	
	public Boolean isProjecting(TileEntityCapture tec) {
		return tec != null && tec.getTileData() != null && tec.getTileData().getInteger("size") > 0;
	}

	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, new IProperty[] { PROJECTING });
	}

	public int getMetaFromState(IBlockState state) {
		return 0;
	}

	@SideOnly(Side.CLIENT)
	public BlockRenderLayer getBlockLayer() {
		return BlockRenderLayer.CUTOUT_MIPPED;
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.MODEL;
	}

}
