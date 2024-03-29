package capsule.network;

import capsule.StructureSaver;
import capsule.helpers.Capsule;
import capsule.items.CapsuleItem;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.Util;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.function.Supplier;

import static capsule.items.CapsuleItem.CapsuleState.DEPLOYED;

public class CapsuleLeftClickQueryToServer {

    protected static final Logger LOGGER = LogManager.getLogger(CapsuleContentPreviewQueryToServer.class);

    public CapsuleLeftClickQueryToServer() {
    }

    public CapsuleLeftClickQueryToServer(PacketBuffer buf) {
    }

    public void onServer(Supplier<NetworkEvent.Context> ctx) {
        final ServerPlayerEntity sendingPlayer = ctx.get().getSender();
        if (sendingPlayer == null) {
            LOGGER.error("ServerPlayerEntity was null when " + this.getClass().getName() + " was received");
            return;
        }

        ctx.get().enqueueWork(() -> {
            // read the content of the template and send it back to the client
            ItemStack stack = sendingPlayer.getMainHandItem();
            if (stack.getItem() instanceof CapsuleItem && CapsuleItem.isBlueprint(stack) && CapsuleItem.hasState(stack, DEPLOYED)) {
                // Reload if no missing materials
                Map<StructureSaver.ItemStackKey, Integer> missing = Capsule.reloadBlueprint(stack, sendingPlayer.getLevel(), sendingPlayer);
                if (missing != null && missing.size() > 0) {
                    StringTextComponent message = new StringTextComponent("Missing :");
                    for (Map.Entry<StructureSaver.ItemStackKey, Integer> entry : missing.entrySet()) {
                        message.append("\n* " + entry.getValue() + " ");
                        message.append(entry.getKey().itemStack.getItem().getName(entry.getKey().itemStack));
                    }
                    sendingPlayer.sendMessage(message, Util.NIL_UUID);
                }
            } else if (stack.getItem() instanceof CapsuleItem && CapsuleItem.canRotate(stack)) {
                PlacementSettings placement = CapsuleItem.getPlacement(stack);
                if (sendingPlayer.isShiftKeyDown()) {
                    switch (placement.getMirror()) {
                        case FRONT_BACK:
                            placement.setMirror(Mirror.LEFT_RIGHT);
                            break;
                        case LEFT_RIGHT:
                            placement.setMirror(Mirror.NONE);
                            break;
                        case NONE:
                            placement.setMirror(Mirror.FRONT_BACK);
                            break;
                    }
                    sendingPlayer.sendMessage(new TranslationTextComponent("[ ]: " + Capsule.getMirrorLabel(placement)), Util.NIL_UUID);
                } else {
                    placement.setRotation(placement.getRotation().getRotated(Rotation.CLOCKWISE_90));
                    sendingPlayer.sendMessage(new TranslationTextComponent("⟳: " + Capsule.getRotationLabel(placement)), Util.NIL_UUID);
                }
                CapsuleItem.setPlacement(stack, placement);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public void toBytes(PacketBuffer buf) {
    }

    @Override
    public String toString() {
        return getClass().toString();
    }

}