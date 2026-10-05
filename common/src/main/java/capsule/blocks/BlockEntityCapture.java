package capsule.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BlockEntityCapture extends DispenserBlockEntity {

    public static final List<BlockEntityCapture> instances = new CopyOnWriteArrayList<>();

    // zone previewed on the client while an empty capsule is held
    private int size = 0;
    private int color = 0;

    public BlockEntityCapture(BlockPos p_155490_, BlockState p_155491_) {
        super(CapsuleBlocks.MARKER_TE.get(), p_155490_, p_155491_);
        instances.add(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        instances.remove(this);
    }

    // When the world loads from disk, the server needs to send the BlockEntity information to the client
    //  it uses getUpdatePacket() and onDataPacket() to do this
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public int getSize() {
        return size;
    }

    public int getColor() {
        return color;
    }

    public void setSizeAndColor(int size, int color) {
        this.size = size;
        this.color = color;
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
