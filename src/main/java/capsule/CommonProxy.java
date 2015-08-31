package capsule;

import capsule.blocks.CapsuleBlocksRegistrer;
import capsule.command.CapsuleCommand;
import capsule.dimension.CapsuleDimensionRegistrer;
import capsule.enchantments.Enchantments;
import capsule.items.CapsuleItemsRegistrer;
import capsule.network.LabelEditedMessageToServer;
import capsule.network.MessageHandlerOnServer;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.config.Configuration;

public class CommonProxy {

	public static SimpleNetworkWrapper simpleNetworkWrapper;
	public static final int LABEL_EDITED_ID = 1;

	public void preInit(FMLPreInitializationEvent event) {
		Config.config = new Configuration(event.getSuggestedConfigurationFile());
		Config.config.load();

		Enchantments.initEnchantments();
		CapsuleItemsRegistrer.createItems(Main.MODID);
		CapsuleBlocksRegistrer.createBlocks(Main.MODID);

		// network stuff
		simpleNetworkWrapper = NetworkRegistry.INSTANCE.newSimpleChannel("CapsuleChannel");
		simpleNetworkWrapper.registerMessage(MessageHandlerOnServer.class, LabelEditedMessageToServer.class, LABEL_EDITED_ID, Side.SERVER);
	}

	public void init(FMLInitializationEvent event) {
		FMLCommonHandler.instance().bus().register(Enchantments.recallEnchant);
	}

	public void postInit(FMLPostInitializationEvent event) {
		CapsuleItemsRegistrer.registerRecipes();
		CapsuleBlocksRegistrer.registerRecipes();
	}

	public void serverAboutToStart(FMLServerAboutToStartEvent evt) {
		CapsuleDimensionRegistrer.registerDimension();
	}

	public void serverStarting(FMLServerStartingEvent e) {
		e.registerServerCommand(new CapsuleCommand());
	}
	
	public void openGuiScreen(EntityPlayer playerIn){

	}
}
