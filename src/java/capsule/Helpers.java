package capsule;

import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.WorldServer;

public class Helpers {

	/**
	 * 
	 * @param sourceWorld
	 *            from world
	 * @param destWorld
	 *            to world
	 * @param srcOriginPos
	 *            from pos
	 * @param destOriginPos
	 *            to pos
	 * @param size
	 *            using a square having a size of "size" blocks
	 * @param overridable
	 *            The blocks in this list can be lost to allow a merge
	 * @param keepSource
	 *            copy only, don't remove blocks from sourceWorld and allow
	 *            duplication.
	 * @param sourceIgnorePos
	 *            This blocks won't be transfered from source
	 * @param outOccupiedDestPos
	 *            This blocks were already present at destination beofre the
	 *            merge
	 * @return
	 */
	public static boolean swapRegions(WorldServer sourceWorld, WorldServer destWorld, BlockPos srcOriginPos, BlockPos destOriginPos, int size,
			List<IBlockState> overridable, List<IBlockState> excluded, boolean keepSource, Map<BlockPos, IBlockState> sourceIgnorePos,
			Map<BlockPos, IBlockState> outOccupiedDestPos) {

		IBlockState air = Blocks.air.getDefaultState();
		if (!isDestinationValid(sourceWorld, destWorld, srcOriginPos, destOriginPos, size, overridable, excluded, outOccupiedDestPos)) {
			return false;
		}

		// 1st copy to dest world
		for (int y = size - 1; y >= 0; y--) {
			for (int x = 0; x < size; x++) {
				for (int z = 0; z < size; z++) {

					BlockPos srcPos = srcOriginPos.add(x, y, z);

					IBlockState srcState = sourceWorld.getBlockState(srcPos);

					// don't copy excluded blocks
					// if must copy
					if (!excluded.contains(srcState) && (sourceIgnorePos == null || !(sourceIgnorePos.keySet().contains(srcPos) && sourceIgnorePos.get(srcPos).equals(srcState)))) {

						BlockPos destPos = destOriginPos.add(x, y, z);
						IBlockState destState = destWorld.getBlockState(destPos);

						// store the dest block if it's overridable
						if (destState == air || overridable.contains(destState)) {
							// copy block without update
							destWorld.setBlockState(destPos, srcState, 4);

							// check tileEntity
							TileEntity srcTE = sourceWorld.getTileEntity(srcPos);
							TileEntity destTE = destWorld.getTileEntity(destPos);

							if (srcTE != null && destTE != null) {
								NBTTagCompound nbt = new NBTTagCompound();
								srcTE.setPos(destPos);
								srcTE.setWorldObj(destWorld);
								srcTE.writeToNBT(nbt);
								destTE.readFromNBT(nbt);

							}
						} // end if dest is overridable

						if (!keepSource) {
							sourceWorld.removeTileEntity(srcPos);
							sourceWorld.setBlockState(srcPos, Blocks.air.getDefaultState(), 4);
						}

					} // end if must copy
				}
			}
		}

		// mark everything for update
		// update surrounding blocks as well
		for (int y = size; y >= -1; y--) {
			for (int x = -1; x < size+1; x++) {
				for (int z = -1; z < size+1; z++) {

					BlockPos srcPos = srcOriginPos.add(x, y, z);
					BlockPos destPos = destOriginPos.add(x, y, z);

					sourceWorld.markBlockForUpdate(srcPos);
					destWorld.markBlockForUpdate(destPos);

				}
			}
		}

		return true;

	}

	/**
	 * Check whether a merge can be done at the destination
	 * 
	 * @param sourceWorld
	 * @param destWorld
	 * @param srcOriginPos
	 * @param destOriginPos
	 * @param size
	 * @param overridable
	 * @param outOccupiedPositions
	 *            Output param, the positions occupied a destination that will
	 *            have to be ignored on
	 * @return List<BlockPos> occupied but not blocking positions
	 */
	public static boolean isDestinationValid(WorldServer sourceWorld, WorldServer destWorld, BlockPos srcOriginPos, BlockPos destOriginPos, int size,
			List<IBlockState> overridable, List<IBlockState> excluded, Map<BlockPos, IBlockState> outOccupiedPositions) {

		IBlockState air = Blocks.air.getDefaultState();

		for (int y = size - 1; y >= 0; y--) {
			for (int x = 0; x < size; x++) {
				for (int z = 0; z < size; z++) {

					BlockPos srcPos = srcOriginPos.add(x, y, z);
					IBlockState srcState = sourceWorld.getBlockState(srcPos);

					BlockPos destPos = destOriginPos.add(x, y, z);
					IBlockState destState = destWorld.getBlockState(destPos);

					boolean destOccupied = (destState != air && !overridable.contains(destState));
					if (destState != air && outOccupiedPositions != null) {
						outOccupiedPositions.put(destPos, destState);
					}
					
					boolean srcOccupied = (srcState != air && !overridable.contains(srcState));
					List entities = destWorld.getEntitiesWithinAABB(
							EntityLivingBase.class,
							AxisAlignedBB.fromBounds(destPos.getX(), destPos.getY(), destPos.getZ(), destPos.getX() +1, destPos.getY()+1, destPos.getZ()+1)
					);

					// if destination is occupied, and source is neither
					// excluded from transportation, nor can't be overriden by
					// destination, then the merge can't be done.
					if ((entities.size() > 0 && srcOccupied) || (destOccupied && !excluded.contains(srcState) && !overridable.contains(srcState))) {
						if(entities.size() > 0){
							System.out.println(entities.get(0));
						}
						return false;
					}

				}
			}
		}

		return true;
	}

	@SuppressWarnings({ "unchecked" })
	public static BlockPos findBottomBlock(EntityItem entityItem, List<IBlockState> excludedBlocks) {
		if (entityItem.getEntityWorld() == null)
			return null;

		double i = entityItem.posX;
		double j = entityItem.posY;
		double k = entityItem.posZ;

		Iterable<BlockPos> blockPoss = BlockPos.getAllInBox(new BlockPos(i, j - 1, k), new BlockPos(i + 1, j + 1, k + 1));
		BlockPos closest = null;
		double closestDistance = 1000;
		for (BlockPos pos : blockPoss) {
			IBlockState blockState = entityItem.worldObj.getBlockState(new BlockPos(i, j - 1, k));
			double distance = pos.distanceSqToCenter(i, j, k);
			if (!excludedBlocks.contains(blockState) && distance < closestDistance) {
				closest = pos;
				closestDistance = distance;
			}
		}

		return closest;
	}

	public static BlockPos findClosestBlock(EntityItem entityItem, List<Block> excludedBlocks) {
		if (entityItem.getEntityWorld() == null)
			return null;

		double i = entityItem.posX;
		double j = entityItem.posY;
		double k = entityItem.posZ;

		@SuppressWarnings("unchecked")
		Iterable<BlockPos> blockPoss = BlockPos.getAllInBox(new BlockPos(i - 1, j - 1, k - 1), new BlockPos(i + 1, j + 1, k + 1));
		BlockPos closest = null;
		double closestDistance = 1000;
		for (BlockPos pos : blockPoss) {
			Block block = entityItem.worldObj.getBlockState(pos).getBlock();
			double distance = pos.distanceSqToCenter(i, j, k);
			if (!excludedBlocks.contains(block) && distance < closestDistance) {
				closest = pos;
				closestDistance = distance;
			}
		}

		return closest;
	}

	@SuppressWarnings("rawtypes")
	public static BlockPos findSpecificBlock(EntityItem entityItem, int maxRange, Class searchedBlock) {
		if (entityItem.getEntityWorld() == null || searchedBlock == null)
			return null;

		double i = entityItem.posX;
		double j = entityItem.posY;
		double k = entityItem.posZ;

		for (int range = 1; range < maxRange; range++) {
			@SuppressWarnings("unchecked")
			Iterable<BlockPos> blockPoss = BlockPos.getAllInBoxMutable(new BlockPos(i - range, j - range, k - range),
					new BlockPos(i + range, j + range, k + range));
			for (BlockPos pos : blockPoss) {
				Block block = entityItem.worldObj.getBlockState(pos).getBlock();
				if (block.getClass().equals(searchedBlock)) {
					return pos.add(0, 0, 0); // return a copy
				}
			}
		}

		return null;
	}

	/*
	 * Color stuff
	 */

	/**
	 * Return whether the specified armor has a color.
	 */
	public static boolean hasColor(ItemStack stack) {
		return (!stack.hasTagCompound() ? false
				: (!stack.getTagCompound().hasKey("display", 10) ? false : stack.getTagCompound().getCompoundTag("display").hasKey("color", 3)));
	}

	/**
	 * Return the color for the specified ItemStack.
	 */
	public static int getColor(ItemStack stack) {
		NBTTagCompound nbttagcompound = stack.getTagCompound();

		if (nbttagcompound != null) {
			NBTTagCompound nbttagcompound1 = nbttagcompound.getCompoundTag("display");

			if (nbttagcompound1 != null && nbttagcompound1.hasKey("color", 3)) {
				return nbttagcompound1.getInteger("color");
			}
		}

		return 0xFFFFFF;
	}

	/**
	 * Remove the color from the specified ItemStack.
	 */
	public static void removeColor(ItemStack stack) {
		NBTTagCompound nbttagcompound = stack.getTagCompound();

		if (nbttagcompound != null) {
			NBTTagCompound nbttagcompound1 = nbttagcompound.getCompoundTag("display");

			if (nbttagcompound1.hasKey("color")) {
				nbttagcompound1.removeTag("color");
			}
		}
	}

	/**
	 * Sets the color of the specified ItemStack
	 */
	public static void setColor(ItemStack stack, int color) {
		NBTTagCompound nbttagcompound = stack.getTagCompound();

		if (nbttagcompound == null) {
			nbttagcompound = new NBTTagCompound();
			stack.setTagCompound(nbttagcompound);
		}

		NBTTagCompound nbttagcompound1 = nbttagcompound.getCompoundTag("display");

		if (!nbttagcompound.hasKey("display", 10)) {
			nbttagcompound.setTag("display", nbttagcompound1);
		}

		nbttagcompound1.setInteger("color", color);
	}

	public static int getStoredEnchantmentLevel(int enchID, ItemStack stack) {
		if (stack == null || !(stack.getItem() instanceof ItemEnchantedBook)) {
			return 0;
		} else {
			NBTTagList nbttaglist = ((ItemEnchantedBook) stack.getItem()).getEnchantments(stack);

			if (nbttaglist == null) {
				return 0;
			} else {
				for (int j = 0; j < nbttaglist.tagCount(); ++j) {
					short short1 = nbttaglist.getCompoundTagAt(j).getShort("id");
					short short2 = nbttaglist.getCompoundTagAt(j).getShort("lvl");

					if (short1 == enchID) {
						return short2;
					}
				}

				return 0;
			}
		}
	}

}
