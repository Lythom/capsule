package capsule;

import capsule.tabs.CapsuleTabs;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import net.minecraft.creativetab.CreativeTabs;

@Mod(modid = Main.MODID, name = Main.MODNAME, version = Main.VERSION)
public class Main {

	public static final String MODID = "capsule";
	public static final String MODNAME = "Capsule";
	public static final String VERSION = "1.0.1";
	
    public static CreativeTabs tabCapsule = new CapsuleTabs(CreativeTabs.getNextID(), "capsule");

	@Instance
	public static Main instance = new Main();
	
	@SidedProxy(serverSide = "capsule.CommonProxy", clientSide = "capsule.ClientProxy")
	public static CommonProxy proxy;

	@EventHandler
	public void preInit(FMLPreInitializationEvent e) {
		proxy.preInit(e);
	}

	@EventHandler
	public void init(FMLInitializationEvent e) {
		proxy.init(e);
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent e) {
		proxy.postInit(e);
	}
	
	@EventHandler public void serverStarting(FMLServerStartingEvent e){
		proxy.serverStarting(e);
	}

	@EventHandler
	private void serverAboutToStart(FMLServerAboutToStartEvent evt) {
		proxy.serverAboutToStart(evt);
	}

}
