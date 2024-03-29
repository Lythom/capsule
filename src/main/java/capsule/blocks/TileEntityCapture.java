package capsule.blocks;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.DispenserTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

public class TileEntityCapture extends DispenserTileEntity {

    public static final List<TileEntityCapture> instances = new ArrayList<>();

    public TileEntityCapture() {
        super(CapsuleBlocks.MARKER_TE);
        instances.add(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        instances.remove(this);
    }

    /**
     * Don't render the gem if the player is too far away
     *
     * @return the maximum distance squared at which the TESR should render
     */
    @OnlyIn(Dist.CLIENT)
    @Override
    public double getViewDistance() {
        return 128;
    }

    // When the world loads from disk, the server needs to send the TileEntity information to the client
    //  it uses getUpdatePacket() and onDataPacket() to do this
    @Override
    public SUpdateTileEntityPacket getUpdatePacket() {
        CompoundNBT nbtTagCompound = new CompoundNBT();
        save(nbtTagCompound);
        return new SUpdateTileEntityPacket(this.getBlockPos(), 0, nbtTagCompound);
    }

    @Override
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
        load(this.level.getBlockState(pkt.getPos()), pkt.getTag());
    }

    /**
     * @return an appropriately size AABB for the TileEntity
     */
    @OnlyIn(Dist.CLIENT)
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return this.getBoundingBox();
    }

    public int getSize() {
        int size = 0;
        if (this.getTileData().contains("size")) {
            size = this.getTileData().getInt("size");
        }
        return size;
    }

    public int getColor() {
        int color = 0;
        if (this.getTileData().contains("color")) {
            color = this.getTileData().getInt("color");
        }
        return color;
    }

    public AxisAlignedBB getBoundingBox() {

        int size = this.getSize();
        BlockPos source = this.getBlockPos().offset(-size, -size, -size);
        BlockPos end = this.getBlockPos().offset(size, size, size);

        AxisAlignedBB box = new AxisAlignedBB(source.getX(), source.getY(), source.getZ(), end.getX(),
                end.getY(), end.getZ());

        return box;
    }
}
