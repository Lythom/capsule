package capsule.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

public class BlockEntityCapture extends DispenserBlockEntity {

    public static final List<BlockEntityCapture> instances = new ArrayList<>();

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

    /**
     * @return an appropriately size AABB for the BlockEntity
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
