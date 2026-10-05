package capsule.network;

import capsule.network.handler.ClientPayloadHandler;
import capsule.network.handler.ServerPayloadHandler;
import capsule.platform.PayloadRegistrar;

public class CapsuleNetwork {
    public static final String VERSION = "1.0";

    public static void registerPayloads(PayloadRegistrar registrar) {
        // client ask server to edit capsule label
        registrar.playToServer(LabelEditedMessageToServer.TYPE, LabelEditedMessageToServer.STREAM_CODEC,
                ServerPayloadHandler::handleLabel);
        // client ask server data needed to preview a deploy
        registrar.playToServer(CapsuleContentPreviewQueryToServer.TYPE, CapsuleContentPreviewQueryToServer.STREAM_CODEC,
                ServerPayloadHandler::handleContentPreviewQuery);
        // client ask server to throw item to a specific position
        registrar.playToServer(CapsuleThrowQueryToServer.TYPE, CapsuleThrowQueryToServer.STREAM_CODEC,
                ServerPayloadHandler::handleThrowQuery);
        // client ask server to reload the held blueprint capsule
        registrar.playToServer(CapsuleLeftClickQueryToServer.TYPE, CapsuleLeftClickQueryToServer.STREAM_CODEC,
                ServerPayloadHandler::handleLeftClickQuery);

        // server sends to client the data needed to preview a deploy
        registrar.playToClient(CapsuleContentPreviewAnswerToClient.TYPE, CapsuleContentPreviewAnswerToClient.STREAM_CODEC,
                ClientPayloadHandler::handleContentPreviewAnswer);
        // server sends to client the data needed to render undeploy
        registrar.playToClient(CapsuleUndeployNotifToClient.TYPE, CapsuleUndeployNotifToClient.STREAM_CODEC,
                ClientPayloadHandler::handleUndeployNotif);
        // server sends to client the full NBT for display
        registrar.playToClient(CapsuleFullContentAnswerToClient.TYPE, CapsuleFullContentAnswerToClient.STREAM_CODEC,
                ClientPayloadHandler::handleFullContentAnswer);
    }
}
