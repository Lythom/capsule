package capsule.gametest;

import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.GameTestTicker;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * NeoForge registers and ticks GameTests outside production only: on a production GameTest server
 * (-Dneoforge.gameTestServer=true, see scripts/prod-gametest.sh) the test mod does it.
 */
@EventBusSubscriber(modid = NeoForgeGameTests.MODID)
public class ProductionGameTests {
    private static final boolean ENABLED = FMLLoader.isProduction() && Boolean.getBoolean("neoforge.gameTestServer");

    @SubscribeEvent
    public static void register(FMLCommonSetupEvent event) {
        if (ENABLED) GameTestRegistry.register(NeoForgeGameTests.class);
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        if (ENABLED && event.getServer().tickRateManager().runsNormally()) GameTestTicker.SINGLETON.tick();
    }
}
