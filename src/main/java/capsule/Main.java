package capsule;

import capsule.client.ClientProxy;
import capsule.tabs.CapsuleTabs;
import net.minecraft.creativetab.ItemGroup;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;

@Mod(Main.MODID)
public class Main {
    public static final String MODID = "capsule";
    public static ItemGroup tabCapsule = new CapsuleTabs(ItemGroup.getNextID(), "capsule");

    public static CommonProxy proxy = DistExecutor.runForDist(() -> ClientProxy::new, () -> CommonProxy::new);;

    @SubscribeEvent
    public void setup(FMLCommonSetupEvent e) {
        proxy.setup(e);
    }

    @SubscribeEvent
    public void init(FMLInitializationEvent e) {
        proxy.init(e);
    }

    @SubscribeEvent
    public void postInit(FMLPostInitializationEvent e) {
        proxy.postInit(e);
    }

    @SubscribeEvent
    public void serverStarting(FMLServerStartingEvent e) {
        proxy.serverStarting(e);
    }

}
