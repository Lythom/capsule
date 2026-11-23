package capsule.blocks;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class BlockEntityCapture extends DispenserBlockEntity {

    /**
     * Data version of Minecraft 1.21.1, the last version Capsule 9 ran on.
     */
    private static final int MINECRAFT_1_21_1 = 3955;

    public static final List<BlockEntityCapture> instances = new CopyOnWriteArrayList<>();

    // zone previewed on the client while an empty capsule is held
    private int size = 0;
    private int color = 0;
    // claim mods check the captures and deploys of the base as this player; bases placed before Capsule 9.1 have none
    @Nullable
    private UUID placer = null;

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

    @Nullable
    public UUID getPlacer() {
        return placer;
    }

    public void setPlacer(@Nullable UUID placer) {
        this.placer = placer;
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        // UUIDUtil.CODEC writes the int array that CompoundTag#putUUID wrote up to 1.21.4, so bases of older worlds keep their placer
        output.storeNullable("placer", UUIDUtil.CODEC, placer);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        placer = input.read("placer", UUIDUtil.CODEC).orElse(null);
        upgradeLegacyItems(input);
    }

    /**
     * The data fixers upgrade the items of vanilla containers when a world of an older Minecraft version loads, not
     * those of a modded block entity: a capsule held by a base saved on 1.21.1 still has the 1.21.1 item format
     * (dyed_color {rgb:...}, enchantments {levels:...}), which no longer decodes, and the base would keep it without
     * its color or enchantments. Such items are upgraded from the 1.21.1 format; items that decode are left alone.
     */
    private void upgradeLegacyItems(ValueInput input) {
        RegistryOps<Tag> ops = input.lookup().createSerializationContext(NbtOps.INSTANCE);
        int currentVersion = SharedConstants.getCurrentVersion().dataVersion().version();
        for (CompoundTag tag : input.listOrEmpty("Items", CompoundTag.CODEC)) {
            if (!ItemStackWithSlot.CODEC.parse(ops, tag).isError()) continue;
            CompoundTag upgraded = (CompoundTag) DataFixers.getDataFixer()
                    .update(References.ITEM_STACK, new Dynamic<>(NbtOps.INSTANCE, tag), MINECRAFT_1_21_1, currentVersion).getValue();
            DataResult<ItemStackWithSlot> item = ItemStackWithSlot.CODEC.parse(ops, upgraded);
            item.result().filter(i -> !item.isError() && i.isValidInContainer(getContainerSize())).ifPresent(i -> getItems().set(i.slot(), i.stack()));
        }
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
