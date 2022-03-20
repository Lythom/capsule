package capsule.blocks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

public class TileEntityCapture extends DispenserBlockEntity {

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
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        CompoundTag nbtTagCompound = new CompoundTag();
        save(nbtTagCompound);
        return new ClientboundBlockEntityDataPacket(this.getBlockPos(), 0, nbtTagCompound);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        load(this.level.getBlockState(pkt.getPos()), pkt.getTag());
    }

    /**
     * @return an appropriately size AABB for the TileEntity
     */
    @OnlyIn(Dist.CLIENT)
    @Override
    public AABB getRenderBoundingBox() {
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

    public AABB getBoundingBox() {

        int size = this.getSize();
        BlockPos source = this.getBlockPos().offset(-size, -size, -size);
        BlockPos end = this.getBlockPos().offset(size, size, size);

        AABB box = new AABB(source.getX(), source.getY(), source.getZ(), end.getX(),
                end.getY(), end.getZ());

        return box;
    }
}
