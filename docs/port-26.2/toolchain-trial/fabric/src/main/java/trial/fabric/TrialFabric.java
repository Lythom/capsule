package trial.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import trial.common.TrialCommon;

public final class TrialFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Identifier id = TrialCommon.id("templates");
        ServerTickEvents.END_LEVEL_TICK.register(level -> { });
        ResourceLoader.get(PackType.SERVER_DATA);
        System.out.println("trial loaded " + id);
    }
}
