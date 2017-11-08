package capsule.blocks;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

public class TileEntityCapture extends TileEntity {

    public static List<TileEntityCapture> instances = new ArrayList<>();

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
    //  it uses getUpdatePacket() and onDataPacket() to do this
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        NBTTagCompound nbtTagCompound = new NBTTagCompound();
        writeToNBT(nbtTagCompound);
        int metadata = getBlockMetadata();
        return new SPacketUpdateTileEntity(this.pos, metadata, nbtTagCompound);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }

    // we only update the position and id, the rest is client side only
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound parentNBTTagCompound) {
        parentNBTTagCompound.setString("id", "capsulemarker_te");
        parentNBTTagCompound.setInteger("x", this.pos.getX());
        parentNBTTagCompound.setInteger("y", this.pos.getY());
        parentNBTTagCompound.setInteger("z", this.pos.getZ());
        return parentNBTTagCompound;
    }

    // This is where you load the data that you saved in writeToNBT
    // we only update the position, the rest is client side only
    @Override
    public void readFromNBT(NBTTagCompound parentNBTTagCompound) {

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

    public int getSize() {
        int size = 0;
        if (this.getTileData().hasKey("size")) {
            size = this.getTileData().getInteger("size");
        }
        return size;
    }

    public int getColor() {
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

        AxisAlignedBB box = new AxisAlignedBB(source.getX(), source.getY(), source.getZ(), end.getX(),
                end.getY(), end.getZ());

        return box;
    }
}
