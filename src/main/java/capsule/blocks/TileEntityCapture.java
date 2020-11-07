package capsule.blocks;

import capsule.CapsuleMod;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

public class TileEntityCapture extends TileEntity {

    public static List<TileEntityCapture> instances = new ArrayList<>();

    public TileEntityCapture() {
        super(CapsuleMod.MARKER_TE);
        instances.add(this);
    }

    /**
     * Don't render the gem if the player is too far away
     *
     * @return the maximum distance squared at which the TESR should render
     */
    @OnlyIn(Dist.CLIENT)
    @Override
    public double getMaxRenderDistanceSquared() {
        final int MAXIMUM_DISTANCE_IN_BLOCKS = 32;
        return MAXIMUM_DISTANCE_IN_BLOCKS * MAXIMUM_DISTANCE_IN_BLOCKS;
    }

    // When the world loads from disk, the server needs to send the TileEntity information to the client
    //  it uses getUpdatePacket() and onDataPacket() to do this
    @Override
    public SUpdateTileEntityPacket getUpdatePacket() {
        CompoundNBT nbtTagCompound = new CompoundNBT();
        write(nbtTagCompound);
        return new SUpdateTileEntityPacket(this.pos, 0, nbtTagCompound);
    }

    @Override
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
        read(pkt.getNbtCompound());
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
        int exdendLength = (size - 1) / 2;

        BlockPos source = this.getPos().add(-exdendLength, 1, -exdendLength);
        BlockPos end = source.add(size, size, size);

        AxisAlignedBB box = new AxisAlignedBB(source.getX(), source.getY(), source.getZ(), end.getX(),
                end.getY(), end.getZ());

        return box;
    }
}
