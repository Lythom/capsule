package capsule.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class CapsuleNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static byte CAPSULE_CHANNEL_MESSAGE_ID = 1;

    public static SimpleChannel wrapper = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("capsule", "capsule_channel"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void setup() {
        // client ask server to edit capsule label
        wrapper.registerMessage(CAPSULE_CHANNEL_MESSAGE_ID++, LabelEditedMessageToServer.class, LabelEditedMessageToServer::toBytes, LabelEditedMessageToServer::new, LabelEditedMessageToServer::onServer);
        // client ask server data needed to preview a deploy
        wrapper.registerMessage(CAPSULE_CHANNEL_MESSAGE_ID++, CapsuleContentPreviewQueryToServer.class, CapsuleContentPreviewQueryToServer::toBytes, CapsuleContentPreviewQueryToServer::new, CapsuleContentPreviewQueryToServer::onServer);
        // client ask server to throw item to a specific position
        wrapper.registerMessage(CAPSULE_CHANNEL_MESSAGE_ID++, CapsuleThrowQueryToServer.class, CapsuleThrowQueryToServer::toBytes, CapsuleThrowQueryToServer::new, CapsuleThrowQueryToServer::onServer);
        // client ask server to reload the held blueprint capsule
        wrapper.registerMessage(CAPSULE_CHANNEL_MESSAGE_ID++, CapsuleLeftClickQueryToServer.class, CapsuleLeftClickQueryToServer::toBytes, CapsuleLeftClickQueryToServer::new, CapsuleLeftClickQueryToServer::onServer);
        // server sends to client the data needed to preview a deploy
        wrapper.registerMessage(CAPSULE_CHANNEL_MESSAGE_ID++, CapsuleContentPreviewAnswerToClient.class, CapsuleContentPreviewAnswerToClient::toBytes, CapsuleContentPreviewAnswerToClient::new, CapsuleContentPreviewAnswerToClient::onClient);
        // server sends to client the data needed to render undeploy
        wrapper.registerMessage(CAPSULE_CHANNEL_MESSAGE_ID++, CapsuleUndeployNotifToClient.class, CapsuleUndeployNotifToClient::toBytes, CapsuleUndeployNotifToClient::new, CapsuleUndeployNotifToClient::onClient);
        // server sends to client the full NBT for display
        wrapper.registerMessage(CAPSULE_CHANNEL_MESSAGE_ID++, CapsuleFullContentAnswerToClient.class, CapsuleFullContentAnswerToClient::toBytes, CapsuleFullContentAnswerToClient::new, CapsuleFullContentAnswerToClient::onClient);

    }
}
