package capsule;

import net.minecraft.block.Block;
import net.minecraft.command.NumberInvalidException;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;

public class Helpers {

	public static BlockPos findBottomBlock(EntityItem entityItem) {
		return findBottomBlock(entityItem.getEntityWorld(), entityItem.posX, entityItem.posY, entityItem.posZ);
	}

	public static BlockPos findBottomBlock(World world, double x, double y, double z) {

		Iterable<BlockPos> blockPoss = BlockPos.getAllInBox(new BlockPos(x, y - 1, z), new BlockPos(x + 1, y + 1, z + 1));
		BlockPos closest = null;
		double closestDistance = 1000;
		for (BlockPos pos : blockPoss) {
			double distance = pos.distanceSqToCenter(x, y, z);
			if (distance < closestDistance) {
				closest = pos;
				closestDistance = distance;
			}
		}

		return closest;
	}

	public static RayTraceResult clientRayTracePreview(EntityPlayer thePlayer, float partialTicks) {
		int blockReachDistance = 18;
		Vec3d vec3d = thePlayer.getPositionEyes(partialTicks);
		Vec3d vec3d1 = thePlayer.getLook(partialTicks);
		Vec3d vec3d2 = vec3d.addVector(vec3d1.xCoord * blockReachDistance, vec3d1.yCoord * blockReachDistance, vec3d1.zCoord * blockReachDistance);
		RayTraceResult rtc = thePlayer.getEntityWorld().rayTraceBlocks(vec3d, vec3d2, false, true, true);
		return rtc;
	}

	@SuppressWarnings("rawtypes")
	public static BlockPos findSpecificBlock(EntityItem entityItem, int maxRange, Class searchedBlock) {
		if (searchedBlock == null)
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
					return new BlockPos(pos.getX(),pos.getY(),pos.getZ()); // return a copy
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
		if (!stack.hasTagCompound()) return false;
		if (!stack.getTagCompound().hasKey("display", 10)) return false;
		return stack.getTagCompound().getCompoundTag("display").hasKey("color", 3);
	}

	/**
	 * Return the color for the specified ItemStack.
	 */
	public static int getColor(ItemStack stack) {
		NBTTagCompound nbttagcompound = stack.getTagCompound();

		if (nbttagcompound != null) {
			NBTTagCompound nbttagcompound1 = nbttagcompound.getCompoundTag("display");
			if (nbttagcompound1.hasKey("color", 3)) {
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

	public static Block[] deserializeBlockArray(String[] blockIds) throws NumberInvalidException {
        ArrayList<Block> states = new ArrayList<Block>();
		for (String blockId : blockIds) {
			Block b = Block.getBlockFromName(blockId);
			if (b != null) {
				states.add(b);
			} else {
				System.err.println(String.format("Block not retrieved found from config name : %s. This block won't be considered in the overridable or excluded blocks list when capturing with capsule.", blockId));
			}
		}
        Block[] output = new Block[states.size()];
        return states.toArray(output);
	}

	public static String[] serializeBlockArray(Block[] states) {

		String[] blocksNames = new String[states.length];
		for(int i = 0; i < states.length; i++){
			blocksNames[i] = ((ResourceLocation)Block.REGISTRY.getNameForObject(states[i])).toString();
		}
		return blocksNames;

	}

}
