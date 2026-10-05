package capsule.clientsmoke;

import capsule.CapsuleMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Dev only: runs the client smoke test when -Dcapsule.clientsmoke=true.
 */
@Mod(value = CapsuleMod.MODID, dist = Dist.CLIENT)
public class NeoForgeClientSmoke {

    public NeoForgeClientSmoke() {
        if (!ClientSmokeTest.ENABLED) return;
        ClientSmokeTest.init();
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> ClientSmokeTest.onClientTick());
    }
}
