package capsule.network;

import capsule.Config;
import capsule.StructureSaver;
import capsule.helpers.Capsule;
import capsule.items.CapsuleItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.Util;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.function.Supplier;

import static capsule.items.CapsuleItem.CapsuleState.DEPLOYED;

public class CapsuleLeftClickQueryToServer {

    protected static final Logger LOGGER = LogManager.getLogger(CapsuleContentPreviewQueryToServer.class);

    public CapsuleLeftClickQueryToServer() {
    }

    public CapsuleLeftClickQueryToServer(FriendlyByteBuf buf) {
    }

    public void onServer(Supplier<NetworkEvent.Context> ctx) {
        final ServerPlayer sendingPlayer = ctx.get().getSender();
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
                    TextComponent message = new TextComponent("Missing :");
                    for (Map.Entry<StructureSaver.ItemStackKey, Integer> entry : missing.entrySet()) {
                        message.append("\n* " + entry.getValue() + " ");
                        message.append(entry.getKey().itemStack.getItem().getName(entry.getKey().itemStack));
                    }
                    sendingPlayer.sendMessage(message, Util.NIL_UUID);
                }
            } else if (stack.getItem() instanceof CapsuleItem && CapsuleItem.canRotate(stack)) {
                StructurePlaceSettings placement = CapsuleItem.getPlacement(stack);
                if (sendingPlayer.isShiftKeyDown()) {
                    if (Config.allowMirror) {
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
                        sendingPlayer.sendMessage(new TranslatableComponent("[ ]: " + Capsule.getMirrorLabel(placement)), Util.NIL_UUID);
                    } else {
                        sendingPlayer.sendMessage(new TranslatableComponent("Mirroring disabled by config"), Util.NIL_UUID);
                    }
                } else {
                    placement.setRotation(placement.getRotation().getRotated(Rotation.CLOCKWISE_90));
                    sendingPlayer.sendMessage(new TranslatableComponent("⟳: " + Capsule.getRotationLabel(placement)), Util.NIL_UUID);
                }
                CapsuleItem.setPlacement(stack, placement);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    @Override
    public String toString() {
        return getClass().toString();
    }

}