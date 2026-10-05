package capsule.clientsmoke;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * Dev only: runs the client smoke test when -Dcapsule.clientsmoke=true.
 */
public class FabricClientSmoke implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        if (!ClientSmokeTest.ENABLED) return;
        ClientSmokeTest.init();
        ClientTickEvents.END_CLIENT_TICK.register(client -> ClientSmokeTest.onClientTick());
    }
}
