package capsule;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.WorldServer;

public class Helpers {

	public static void teleportBlock(WorldServer sourceWorld, WorldServer destWorld, BlockPos srcPos, BlockPos destPos) {
		TileEntity srcTE = sourceWorld.getTileEntity(srcPos);
		IBlockState srcState = sourceWorld.getBlockState(srcPos);
		
		// store the current block
		destWorld.setBlockState(destPos, srcState);
		TileEntity destTE = destWorld.getTileEntity(destPos);
		
		if(srcTE != null && destTE != null){
			NBTTagCompound nbt = new NBTTagCompound();
			srcTE.setPos(destPos);
			srcTE.setWorldObj(destWorld);
			srcTE.writeToNBT(nbt);
			destTE.readFromNBT(nbt);
		}
		
		// remove from the world the stored block
		sourceWorld.removeTileEntity(srcPos);
		sourceWorld.setBlockState(srcPos, Blocks.air.getDefaultState());
		
		destWorld.markBlockForUpdate(destPos);
		sourceWorld.markBlockForUpdate(srcPos);
	}
	
	@SuppressWarnings({ "unchecked" })
	public static BlockPos findBottomBlock(EntityItem entityItem, List<Block> excludedBlocks) {
		if(entityItem.getEntityWorld() == null) return null;
		
		double i = entityItem.posX;
		double j = entityItem.posY;
		double k = entityItem.posZ;

        Iterable<BlockPos> blockPoss = BlockPos.getAllInBox(new BlockPos(i, j - 1, k), new BlockPos(i + 1, j + 1, k + 1));
        BlockPos closest = null;
        double closestDistance = 1000;
        for( BlockPos pos : blockPoss) {
        	Block block = entityItem.worldObj.getBlockState(new BlockPos(i, j - 1, k)).getBlock();
        	double distance = pos.distanceSqToCenter(i, j, k);
        	if (!excludedBlocks.contains(block) &&  distance < closestDistance) {
        		closest = pos;
            	closestDistance = distance;
            }
        }
        
		return closest;
	}
	
	
	
	/*
	 * Color stuff
	 */
	
	/**
     * Return whether the specified armor has a color.
     */
    public static boolean hasColor(ItemStack stack)
    {
        return (!stack.hasTagCompound() ? false : (!stack.getTagCompound().hasKey("display", 10) ? false : stack.getTagCompound().getCompoundTag("display").hasKey("color", 3)));
    }

    /**
     * Return the color for the specified ItemStack.
     */
    public static int getColor(ItemStack stack)
    {
        NBTTagCompound nbttagcompound = stack.getTagCompound();

        if (nbttagcompound != null)
        {
            NBTTagCompound nbttagcompound1 = nbttagcompound.getCompoundTag("display");

            if (nbttagcompound1 != null && nbttagcompound1.hasKey("color", 3))
            {
                return nbttagcompound1.getInteger("color");
            }
        }

        return 0xFFFFFF;
    }

    /**
     * Remove the color from the specified ItemStack.
     */
    public static void removeColor(ItemStack stack)
    {
        NBTTagCompound nbttagcompound = stack.getTagCompound();

        if (nbttagcompound != null)
        {
            NBTTagCompound nbttagcompound1 = nbttagcompound.getCompoundTag("display");

            if (nbttagcompound1.hasKey("color"))
            {
                nbttagcompound1.removeTag("color");
            }
        }
    }

    /**
     * Sets the color of the specified ItemStack
     */
    public static void setColor(ItemStack stack, int color)
    {
        NBTTagCompound nbttagcompound = stack.getTagCompound();

        if (nbttagcompound == null)
        {
            nbttagcompound = new NBTTagCompound();
            stack.setTagCompound(nbttagcompound);
        }

        NBTTagCompound nbttagcompound1 = nbttagcompound.getCompoundTag("display");

        if (!nbttagcompound.hasKey("display", 10))
        {
            nbttagcompound.setTag("display", nbttagcompound1);
        }

        nbttagcompound1.setInteger("color", color);
    }
}
