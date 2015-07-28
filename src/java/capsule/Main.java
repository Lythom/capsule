package capsule;

import capsule.dimension.CapsuleDimension;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;

@Mod(modid = Main.MODID, name = Main.MODNAME, version = Main.VERSION)
public class Main {
	
	public static final String MODID = "capsule";
    public static final String MODNAME = "Capsule";
    public static final String VERSION = "1.0.0";

    @Instance
    public static Main instance = new Main();
        
     
    @EventHandler
    public void preInit(FMLPreInitializationEvent e) {
    	CapsuleItems.createItems(Main.MODID);
    }
        
    @EventHandler
    public void init(FMLInitializationEvent e) {
    	CapsuleItems.registerRenderers(Main.MODID);
    }
        
    @EventHandler
    public void postInit(FMLPostInitializationEvent e) {
    	CapsuleItems.registerRecipes();
    }
    
	@EventHandler
	private void serverAboutToStart( FMLServerAboutToStartEvent evt )
	{
		CapsuleDimension.registerDimension();
	}
    
}
