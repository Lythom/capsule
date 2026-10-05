package capsule.neoforge;

import capsule.platform.PayloadRegistrar;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

record NeoForgePayloadRegistrar(net.neoforged.neoforge.network.registration.PayloadRegistrar registrar) implements PayloadRegistrar {
    private static final Logger LOGGER = LogManager.getLogger(NeoForgePayloadRegistrar.class);

    @Override
    public <T extends CustomPacketPayload> void playToServer(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, BiConsumer<T, ServerPlayer> handler) {
        registrar.playToServer(type, codec, (payload, context) -> context.enqueueWork(() -> handler.accept(payload, (ServerPlayer) context.player()))
                .exceptionally(e -> {
                    LOGGER.error("Failed to handle {}", type.id(), e);
                    return null;
                }));
    }

    @Override
    public <T extends CustomPacketPayload> void playToClient(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, Consumer<T> handler) {
        registrar.playToClient(type, codec, (payload, context) -> context.enqueueWork(() -> handler.accept(payload))
                .exceptionally(e -> {
                    LOGGER.error("Failed to handle {}", type.id(), e);
                    return null;
                }));
    }
}
