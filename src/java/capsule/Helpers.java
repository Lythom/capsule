package capsule;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
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
}
