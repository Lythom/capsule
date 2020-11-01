package capsule;

import capsule.client.ClientProxy;
import capsule.tabs.CapsuleTabs;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;

@Mod(Main.MODID)
public class Main {
    public static final String MODID = "capsule";
    public static ItemGroup tabCapsule = new CapsuleTabs(ItemGroup.getGroupCountSafe(), "capsule");

    public static CommonProxy proxy = DistExecutor.runForDist(() -> ClientProxy::new, () -> CommonProxy::new);;

    @SubscribeEvent
    public void setup(FMLCommonSetupEvent e) {
        proxy.setup(e);
    }

    @SubscribeEvent
    public void serverStarting(FMLServerStartingEvent e) {
        proxy.serverStarting(e);
    }

}
