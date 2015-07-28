package capsule;

import capsule.blocks.CapsuleBlocksRegistrer;
import capsule.dimension.CapsuleDimensionRegistrer;
import capsule.items.CapsuleItemsRegistrer;
import net.minecraftforge.common.config.Configuration;
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
    	CapsuleItemsRegistrer.createItems(Main.MODID);
    	CapsuleBlocksRegistrer.createBlocks(Main.MODID);
        Config.config = new Configuration(e.getSuggestedConfigurationFile());
        Config.config.load();
    }
        
    @EventHandler
    public void init(FMLInitializationEvent e) {
    	CapsuleItemsRegistrer.registerRenderers(Main.MODID);
    	CapsuleBlocksRegistrer.registerRenderers(Main.MODID);
    }
        
    @EventHandler
    public void postInit(FMLPostInitializationEvent e) {
    	CapsuleItemsRegistrer.registerRecipes();
    	CapsuleBlocksRegistrer.registerRecipes();
    }
    
	@EventHandler
	private void serverAboutToStart( FMLServerAboutToStartEvent evt )
	{
		CapsuleDimensionRegistrer.registerDimension();
	}
    
}
