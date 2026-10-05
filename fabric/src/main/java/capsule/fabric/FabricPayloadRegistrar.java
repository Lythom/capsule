package capsule.fabric;

import capsule.platform.PayloadRegistrar;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Payload types are registered on both sides, client receivers only by the client entrypoint.
 */
class FabricPayloadRegistrar implements PayloadRegistrar {
    private static final Logger LOGGER = LogManager.getLogger(FabricPayloadRegistrar.class);
    private static final List<Runnable> CLIENT_RECEIVERS = new ArrayList<>();

    static void registerClientReceivers() {
        CLIENT_RECEIVERS.forEach(Runnable::run);
    }

    @Override
    public <T extends CustomPacketPayload> void playToServer(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, BiConsumer<T, ServerPlayer> handler) {
        PayloadTypeRegistry.playC2S().register(type, codec);
        ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) -> handle(type, () -> handler.accept(payload, context.player())));
    }

    @Override
    public <T extends CustomPacketPayload> void playToClient(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, Consumer<T> handler) {
        PayloadTypeRegistry.playS2C().register(type, codec);
        CLIENT_RECEIVERS.add(() -> ClientPlayNetworking.registerGlobalReceiver(type, (payload, context) -> handle(type, () -> handler.accept(payload))));
    }

    private static void handle(CustomPacketPayload.Type<?> type, Runnable handler) {
        try {
            handler.run();
        } catch (RuntimeException e) {
            LOGGER.error("Failed to handle {}", type.id(), e);
        }
    }
}
