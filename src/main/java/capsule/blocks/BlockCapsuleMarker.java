/**
 * 
 */
package capsule.blocks;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

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
		this.setBlockName(unlocalizedName);
		this.setHardness(20);
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

	protected BlockState createBlockState() {
		return new BlockState(this, new IProperty[] { PROJECTING });
	}

	public int getMetaFromState(IBlockState state) {
		return 0;
	}
	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public int getRenderType() {
		return 3;
	}

}
