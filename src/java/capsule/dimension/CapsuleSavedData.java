/**
 * 
 */
package capsule.dimension;

import java.util.HashMap;

import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

/**
 * @author Lythom
 *
 */
public class CapsuleSavedData extends WorldSavedData {

	private HashMap<Integer, BlockPos> lastReservedPosition = new HashMap<Integer, BlockPos>();

	/**
	 * @param name
	 */
	public CapsuleSavedData(String name) {
		super(name);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		// retrieve lastReservedPosition
		for(Object keyObj : nbt.getKeySet()){
			String key = String.valueOf(keyObj);
			NBTTagCompound blockNBT = nbt.getCompoundTag(key);
			BlockPos pos = new BlockPos(blockNBT.getInteger("x"), blockNBT.getInteger("y"), blockNBT.getInteger("Z"));
			lastReservedPosition.put(Integer.parseInt(key), pos);
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		// serialize lastReservedPosition
		for(Integer size : lastReservedPosition.keySet()){
			NBTTagCompound pos = new NBTTagCompound();
			BlockPos lastBlockpos = lastReservedPosition.get(size);
			if(lastBlockpos != null){
				pos.setInteger("x", lastBlockpos.getX());
				pos.setInteger("y", lastBlockpos.getY());
				pos.setInteger("z", lastBlockpos.getZ());
				nbt.setTag(size.toString(), pos);
			}
			
		}
	}

	/**
	 * Prepare a place to put a Capsule content.
	 * Protect the zone with bedrock.
	 * @param sideLength
	 * @return a sideLength x sideLength x sideLength 1st blockPos in CapsuleWorld
	 */
	public BlockPos reserveNextAvailablePositionForSize(int sideLength) {
		BlockPos lastPos = lastReservedPosition.get(sideLength);
		BlockPos nextPos = null;
		if (lastPos == null) {
			nextPos = new BlockPos(0, 1, getSizeZPosition(sideLength));
		} else {
			nextPos = lastPos.add(sideLength + 2, 0, 0);
		}
		lastReservedPosition.put(sideLength, nextPos);
		this.markDirty(); // ask the server to save the data on next map save
		
		// create a bedrock barrier around
		WorldServer capsuleWorld = DimensionManager.getWorld(CapsuleDimension.dimensionId);
		BlockPos nextPosEnd = nextPos.add(sideLength+1, sideLength+1, sideLength+1);
		@SuppressWarnings("unchecked")
		Iterable<BlockPos> borders = BlockPos.getAllInBoxMutable(nextPos, nextPosEnd);
		for (BlockPos pos : borders) {
			if(pos.getX() == nextPos.getX() || pos.getX() == nextPosEnd.getX() || pos.getZ() == nextPos.getZ() || pos.getZ() == nextPosEnd.getZ()) {
				capsuleWorld.setBlockState(pos, Blocks.bedrock.getDefaultState());
			}
		}
		
		return nextPos.add(1,0,1); // the caller doesn't care about the borders, it's internal stuff.
	}

	public int getSizeZPosition(int size) {
		if (size <= 1) {
			return 3;
		} else {
			return getSizeZPosition(size - 1) + size + 2; // +2 because we add borders for each storage
		}
	}
}
