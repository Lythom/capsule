package capsule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.NumberInvalidException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
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
			List<Block> overridable, List<Block> excluded, boolean keepSource, Map<BlockPos, Block> sourceIgnorePos,
			Map<BlockPos, Block> outOccupiedDestPos, List<String> outEntityBlocking) {

		Block air = Blocks.air;
		if (!isDestinationValid(sourceWorld, destWorld, srcOriginPos, destOriginPos, size, overridable, excluded, outOccupiedDestPos, outEntityBlocking)) {
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
					if (!excluded.contains(srcState.getBlock()) && (sourceIgnorePos == null || !(sourceIgnorePos.keySet().contains(srcPos) && sourceIgnorePos.get(srcPos).equals(srcState.getBlock())))) {

						BlockPos destPos = destOriginPos.add(x, y, z);
						IBlockState destState = destWorld.getBlockState(destPos);

						// store the dest block if it's overridable
						if (air.equals(destState.getBlock()) || overridable.contains(destState.getBlock())) {
							// copy block without update
							destWorld.setBlockState(destPos, srcState, 7);
							destWorld.notifyNeighborsOfStateChange(destPos, srcState.getBlock());

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
							sourceWorld.setBlockState(srcPos, Blocks.air.getDefaultState(), 7);
						}

					} // end if must copy
				}
			}
		}
		
		// attempt to TP armor stand. Not working for now : they don't land on the right position, and it's really CPU intensive
//		List<EntityArmorStand> armorstands = sourceWorld.getEntitiesWithinAABB(
//			EntityArmorStand.class, 
//			new AxisAlignedBB(
//				srcOriginPos.getX(), srcOriginPos.getY(), srcOriginPos.getZ(), 
//				srcOriginPos.getX() + size + 1, srcOriginPos.getY() + size + 1, srcOriginPos.getZ() + size + 1
//			)
//		);
//		
//		for(EntityArmorStand armorstand : armorstands){
//			BlockPos relativePos = armorstand.getPosition().add(-srcOriginPos.getX(), -srcOriginPos.getY(), -srcOriginPos.getZ());
//			armorstand.changeDimension(destWorld.provider.getDimension());
//			armorstand.setPositionAndUpdate(destOriginPos.getX() + relativePos.getX(), destOriginPos.getY() + relativePos.getY(), destOriginPos.getZ() + relativePos.getZ());
//		}

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
			List<Block> overridable, List<Block> excluded, Map<BlockPos, Block> outOccupiedPositions, List<String> outEntityBlocking) {

		IBlockState air = Blocks.air.getDefaultState();

		for (int y = size - 1; y >= 0; y--) {
			for (int x = 0; x < size; x++) {
				for (int z = 0; z < size; z++) {

					BlockPos srcPos = srcOriginPos.add(x, y, z);
					Block srcState = sourceWorld.getBlockState(srcPos).getBlock();

					BlockPos destPos = destOriginPos.add(x, y, z);
					Block destState = destWorld.getBlockState(destPos).getBlock();

					boolean destOccupied = (destState != air && !overridable.contains(destState));
					if (destState != air && outOccupiedPositions != null) {
						outOccupiedPositions.put(destPos, destState);
					}
					
					boolean srcOccupied = (srcState != air && !overridable.contains(srcState));
					@SuppressWarnings("rawtypes")
					List entities = destWorld.getEntitiesWithinAABB(
							EntityLivingBase.class,
							new AxisAlignedBB(destPos.getX(), destPos.getY(), destPos.getZ(), destPos.getX() +1, destPos.getY()+1, destPos.getZ()+1)
					);

					// if destination is occupied, and source is neither
					// excluded from transportation, nor can't be overriden by
					// destination, then the merge can't be done.
					if ((entities.size() > 0 && srcOccupied) || (destOccupied && !excluded.contains(srcState) && !overridable.contains(srcState))) {
						if(entities.size() > 0 && outEntityBlocking != null){
							for(Object e : entities){
								Entity entity = (Entity)e;
								if(entity != null){
									outEntityBlocking.add(entity.getName());
								}
							}
							
						}
						return false;
					}
				}
			}
		}

		return true;
	}

	public static BlockPos findBottomBlock(EntityItem entityItem, List<Block> excludedBlocks) {
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
			Iterable<MutableBlockPos> blockPoss = BlockPos.getAllInBoxMutable(new BlockPos(i - range, j - range, k - range),
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
	
	public static Block[] deserializeBlockArray(String[] blockIds) throws NumberInvalidException {
			ArrayList<Block> states = new ArrayList<Block>();
			for (int i = 0; i < blockIds.length; i++) {
				Block b = Block.getBlockFromName(blockIds[i]);
				if(b != null){
					states.add(b);
				} else {
					System.err.println(String.format("Block not retrieved found from config name : %s. This block won't be considered in the overridable or excluded blocks list when capturing with capsule.", blockIds[i]));
				}
			}
			Block[] output = new Block[states.size()];
			return states.toArray(output);

	}
	
	public static String[] serializeBlockArray(Block[] states) {

		String[] blocksNames = new String[states.length];
		for(int i = 0; i < states.length; i++){
			blocksNames[i] = ((ResourceLocation)Block.blockRegistry.getNameForObject(states[i])).toString();
		}
		return blocksNames;

	}

}
