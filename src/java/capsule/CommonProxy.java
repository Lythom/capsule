package capsule;

import capsule.blocks.CapsuleBlocksRegistrer;
import capsule.dimension.CapsuleDimensionRegistrer;
import capsule.items.CapsuleItemsRegistrer;
import capsule.network.LabelEditedMessageToServer;
import capsule.network.MessageHandlerOnServer;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class CommonProxy {

	public static SimpleNetworkWrapper simpleNetworkWrapper;
	public static final byte LABEL_EDITED_ID = 1;


	public void preInit(FMLPreInitializationEvent event) {
		CapsuleItemsRegistrer.createItems(Main.MODID);
		CapsuleBlocksRegistrer.createBlocks(Main.MODID);
		Config.config = new Configuration(event.getSuggestedConfigurationFile());
		Config.config.load();
		
		// network stuff
		simpleNetworkWrapper = NetworkRegistry.INSTANCE.newSimpleChannel("CapsuleChannel");
	    simpleNetworkWrapper.registerMessage(MessageHandlerOnServer.class, LabelEditedMessageToServer.class,
	    		LABEL_EDITED_ID, Side.SERVER);
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
