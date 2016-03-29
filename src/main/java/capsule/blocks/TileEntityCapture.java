package capsule.blocks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntityCapture extends TileEntity {

	public static List<TileEntityCapture> instances = new ArrayList<TileEntityCapture>();
	
	public TileEntityCapture() {
		super();
		instances.add(this);
	}
	
	
	@Override
	public void onChunkUnload() {
		this.getTileData().setInteger("size", 0);
	}

	/**
	 * Don't render the gem if the player is too far away
	 * 
	 * @return the maximum distance squared at which the TESR should render
	 */
	@SideOnly(Side.CLIENT)
	@Override
	public double getMaxRenderDistanceSquared() {
		final int MAXIMUM_DISTANCE_IN_BLOCKS = 32;
		return MAXIMUM_DISTANCE_IN_BLOCKS * MAXIMUM_DISTANCE_IN_BLOCKS;
	}
	
	// When the world loads from disk, the server needs to send the TileEntity information to the client
	//  it uses getDescriptionPacket() and onDataPacket() to do this
	@Override
	public Packet<?> getDescriptionPacket() {
		NBTTagCompound nbtTagCompound = new NBTTagCompound();
		writeToNBT(nbtTagCompound);
		int metadata = getBlockMetadata();
		return new S35PacketUpdateTileEntity(this.pos, metadata, nbtTagCompound);
	}

	@Override
	public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
		readFromNBT(pkt.getNbtCompound());
	}

	// we only update the position and id, the rest is client side only
	@Override
	public void writeToNBT(NBTTagCompound parentNBTTagCompound)
	{
		parentNBTTagCompound.setString("id", "capsulemarker_te");
		parentNBTTagCompound.setInteger("x", this.pos.getX());
		parentNBTTagCompound.setInteger("y", this.pos.getY());
		parentNBTTagCompound.setInteger("z", this.pos.getZ());
	}

	// This is where you load the data that you saved in writeToNBT
	// we only update the position, the rest is client side only
	@Override
	public void readFromNBT(NBTTagCompound parentNBTTagCompound)
	{
		
		this.pos = new BlockPos(parentNBTTagCompound.getInteger("x"), parentNBTTagCompound.getInteger("y"), parentNBTTagCompound.getInteger("z"));
	}

	/**
	 * @return an appropriately size AABB for the TileEntity
	 */
	@SideOnly(Side.CLIENT)
	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return this.getBoundingBox();
	}
	
	public int getSize(){
		int size = 0;
		if (this.getTileData().hasKey("size")) {
			size = this.getTileData().getInteger("size");
		}
		return size;
	}
	
	public int getColor(){
		int color = 0;
		if (this.getTileData().hasKey("color")) {
			color = this.getTileData().getInteger("color");
		}
		return color;
	}
	
	public AxisAlignedBB getBoundingBox() {
		
		int size = this.getSize();
		int exdendLength = (size - 1) / 2;

		BlockPos source = this.getPos().add(-exdendLength, 1, -exdendLength);
		BlockPos end = source.add(size, size, size);

		AxisAlignedBB box = AxisAlignedBB.fromBounds(source.getX(), source.getY(), source.getZ(), end.getX(),
				end.getY(), end.getZ());

		return box;
	}
}
