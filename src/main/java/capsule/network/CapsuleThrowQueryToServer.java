package capsule.network;

import capsule.helpers.Capsule;
import capsule.items.CapsuleItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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
        final ServerPlayer sendingPlayer = ctx.get().getSender();
        if (sendingPlayer == null) {
            LOGGER.error("ServerPlayerEntity was null when " + this.getClass().getName() + " was received");
            return;
        }

        ItemStack heldItem = sendingPlayer.getMainHandItem();
        ServerLevel world = sendingPlayer.getLevel();
        ctx.get().enqueueWork(() -> {
            if (heldItem.getItem() instanceof CapsuleItem) {
                if (instant && pos != null) {
                    int size = CapsuleItem.getSize(heldItem);
                    int extendLength = (size - 1) / 2;
                    // instant capsule initial capture
                    if (CapsuleItem.hasState(heldItem, EMPTY)) {
                        boolean captured = Capsule.captureAtPosition(heldItem, sendingPlayer.getUUID(), size, sendingPlayer.getLevel(), pos);
                        if (captured) {
                            BlockPos center = pos.offset(0, size / 2, 0);
                            CapsuleNetwork.wrapper.send(
                                    PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(center.getX(), center.getY(), center.getZ(), 200 + size, sendingPlayer.getCommandSenderWorld().dimension())),
                                    new CapsuleUndeployNotifToClient(center, sendingPlayer.blockPosition(), size, CapsuleItem.getStructureName(heldItem))
                            );
                        }
                    }
                    // instant deployment
                    else {
                        boolean deployed = Capsule.deployCapsule(heldItem, pos.offset(0, -1, 0), sendingPlayer.getUUID(), extendLength, world);
                        if (deployed) {
                            world.playSound(null, pos, SoundEvents.ARROW_SHOOT, SoundSource.BLOCKS, 0.4F, 0.1F);
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

    public CapsuleThrowQueryToServer(FriendlyByteBuf buf) {
        try {
            this.instant = buf.readBoolean();
            boolean hasPos = buf.readBoolean();
            if (hasPos) {
                this.pos = BlockPos.of(buf.readLong());
            }

        } catch (IndexOutOfBoundsException ignored) {
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(this.instant);
        boolean hasPos = this.pos != null;
        buf.writeBoolean(hasPos);
        if (hasPos) {
            buf.writeLong(this.pos.asLong());
        }
    }

    @Override
    public String toString() {
        return getClass().toString();
    }
}