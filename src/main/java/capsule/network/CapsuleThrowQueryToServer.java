package capsule.network;

import capsule.helpers.Capsule;
import capsule.items.CapsuleItem;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

import static capsule.items.CapsuleItem.CapsuleState.EMPTY;

public class CapsuleThrowQueryToServer {

    protected static final Logger LOGGER = LogManager.getLogger(CapsuleThrowQueryToServer.class);

    public BlockPos pos = null;
    public boolean instant = false;

    public CapsuleThrowQueryToServer(BlockPos pos, boolean instant) {
        this.pos = pos;
        this.instant = instant;
    }

    public void onServer(Supplier<NetworkEvent.Context> ctx) {
        final ServerPlayerEntity sendingPlayer = ctx.get().getSender();
        if (sendingPlayer == null) {
            LOGGER.error("ServerPlayerEntity was null when " + this.getClass().getName() + " was received");
            return;
        }

        ItemStack heldItem = sendingPlayer.getHeldItemMainhand();
        ServerWorld world = sendingPlayer.getServerWorld();
        ctx.get().enqueueWork(() -> {
            if (heldItem.getItem() instanceof CapsuleItem) {
                if (instant && pos != null) {
                    int size = CapsuleItem.getSize(heldItem);
                    int extendLength = (size - 1) / 2;
                    // instant capsule initial capture
                    if (CapsuleItem.hasState(heldItem, EMPTY)) {
                        boolean captured = Capsule.captureAtPosition(heldItem, sendingPlayer.getUniqueID(), size, sendingPlayer.getServerWorld(), pos);
                        if (captured) {
                            BlockPos center = pos.add(0, size / 2, 0);
                            CapsuleNetwork.wrapper.send(
                                    PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(center.getX(), center.getY(), center.getZ(), 200 + size, sendingPlayer.dimension)),
                                    new CapsuleUndeployNotifToClient(center, sendingPlayer.getPosition(), size)
                            );
                        }
                    }
                    // instant deployment
                    else {
                        boolean deployed = Capsule.deployCapsule(heldItem, pos.add(0, -1, 0), sendingPlayer.getUniqueID(), extendLength, world);
                        if (deployed) {
                            world.playSound(null, pos, SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.BLOCKS, 0.4F, 0.1F);
                            Capsule.showDeployParticules(world, pos, size);
                        }
                        if (deployed && CapsuleItem.isOneUse(heldItem)) {
                            heldItem.shrink(1);
                        }
                    }
                }
                if (!instant) {
                    Capsule.throwCapsule(heldItem, sendingPlayer, pos);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public CapsuleThrowQueryToServer(PacketBuffer buf) {
        try {
            this.instant = buf.readBoolean();
            boolean hasPos = buf.readBoolean();
            if (hasPos) {
                this.pos = BlockPos.fromLong(buf.readLong());
            }

        } catch (IndexOutOfBoundsException ignored) {
        }
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeBoolean(this.instant);
        boolean hasPos = this.pos != null;
        buf.writeBoolean(hasPos);
        if (hasPos) {
            buf.writeLong(this.pos.toLong());
        }
    }

    @Override
    public String toString() {
        return getClass().toString();
    }
}