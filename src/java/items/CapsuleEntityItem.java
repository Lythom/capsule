/**
 * 
 */
package items;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/**
 * @author Lythom
 *
 */
public class CapsuleEntityItem extends EntityItem {

	/**
	 * @param worldIn
	 */
	public CapsuleEntityItem(World worldIn) {
		super(worldIn);
	}

	/**
	 * @param worldIn
	 * @param x
	 * @param y
	 * @param z
	 */
	public CapsuleEntityItem(World worldIn, double x, double y, double z) {
		super(worldIn, x, y, z);
	}

	/**
	 * @param worldIn
	 * @param x
	 * @param y
	 * @param z
	 * @param stack
	 */
	public CapsuleEntityItem(World worldIn, double x, double y, double z, ItemStack stack) {
		super(worldIn, x, y, z, stack);
	}
	
	@Override
	public void onEntityUpdate() {
		super.onEntityUpdate();
		
		if (this.isCollided && this.getEntityWorld() != null) {
			this.getEntityItem().setItemDamage(2);
			BlockPos blockPos = this.findClosestBlock();
			this.getEntityWorld().setBlockState(blockPos, Blocks.obsidian.getDefaultState());
			this.getEntityWorld().removeEntity(this);
		}
	}

	private BlockPos findClosestBlock() {
		if(this.getEntityWorld() == null) return null;
		
		int i = MathHelper.floor_double(this.posX);
        int j = MathHelper.floor_double(this.posY);
        int k = MathHelper.floor_double(this.posZ);

        Iterable<BlockPos> blockPoss = BlockPos.getAllInBox(new BlockPos(i - 1, j - 1, k - 1), new BlockPos(i + 1, j + 1, k + 1));
        BlockPos closest = null;
        double closestDistance = 1000;
        for( BlockPos pos : blockPoss) {
        	Block block = this.worldObj.getBlockState(pos).getBlock();
        	double distance = pos.distanceSqToCenter(i, j, k);
        	if (block.getMaterial() != Material.air && pos.distanceSqToCenter(i, j, k) < closestDistance) {
        		closest = pos;
            	closestDistance = distance;
            }
        }
        
		return closest;
	}
	
	

}
