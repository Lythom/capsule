package capsule.network;

import capsule.CapsuleMod;
import capsule.network.handler.ClientPayloadHandler;
import capsule.network.handler.ServerPayloadHandler;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class CapsuleNetwork {
    public static void setupPackets(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(CapsuleMod.MODID).versioned("1.0");

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
