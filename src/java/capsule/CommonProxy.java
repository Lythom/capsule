package capsule;

import capsule.blocks.CapsuleBlocksRegistrer;
import capsule.dimension.CapsuleDimensionRegistrer;
import capsule.items.CapsuleItemsRegistrer;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;

public class CommonProxy {

	public void preInit(FMLPreInitializationEvent event) {
		CapsuleItemsRegistrer.createItems(Main.MODID);
		CapsuleBlocksRegistrer.createBlocks(Main.MODID);
		Config.config = new Configuration(event.getSuggestedConfigurationFile());
		Config.config.load();
	}

	public void init(FMLInitializationEvent event) {
		
	}

	public void postInit(FMLPostInitializationEvent event) {
		CapsuleItemsRegistrer.registerRecipes();
		CapsuleBlocksRegistrer.registerRecipes();
	}
	
	public void serverAboutToStart(FMLServerAboutToStartEvent evt) {
		CapsuleDimensionRegistrer.registerDimension();
	}
}
