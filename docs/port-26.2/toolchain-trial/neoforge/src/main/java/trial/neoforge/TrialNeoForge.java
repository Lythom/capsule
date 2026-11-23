package trial.neoforge;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.common.NeoForge;
import trial.common.TrialCommon;

@Mod(TrialCommon.MODID)
public final class TrialNeoForge {
    public TrialNeoForge(IEventBus modBus, ModContainer container) {
        Identifier id = TrialCommon.id("templates");
        NeoForge.EVENT_BUS.addListener((AddServerReloadListenersEvent e) -> { });
        System.out.println("trial loaded " + id);
    }
}
