package capsule.gametest;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

@EventBusSubscriber(modid = NeoForgeGameTests.MODID)
public class NeoForgeGameTestSetup {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void serverAboutToStart(ServerAboutToStartEvent event) {
        GameTestProfiles.provideProfileCache(event.getServer());
    }

    @SubscribeEvent
    public static void blockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!TestProbe.canPlace(event.getPos())) event.setCanceled(true);
    }
}
