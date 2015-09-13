package capsule;

import java.util.Arrays;

import capsule.blocks.CapsuleBlocksRegistrer;
import capsule.command.CapsuleCommand;
import capsule.dimension.CapsuleDimensionRegistrer;
import capsule.enchantments.Enchantments;
import capsule.items.CapsuleItemsRegistrer;
import capsule.network.LabelEditedMessageToServer;
import capsule.network.MessageHandlerOnServer;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.NumberInvalidException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class CommonProxy {

	public static SimpleNetworkWrapper simpleNetworkWrapper;
	public static final byte LABEL_EDITED_ID = 1;

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
		
		Property excludedBlocksProp = Config.config.get("Balancing", "excludedBlocks",
				Helpers.serializeBlockArray(new Block[] { Blocks.air, Blocks.bedrock }));
		excludedBlocksProp.comment = "List of block ids that will never be captured. While capturing, the blocks will stay in place.\n Ex: minecraft:mob_spawner";
		Block[] exBlocks = null;
		try {
			exBlocks = Helpers.deserializeBlockArray(excludedBlocksProp.getStringList());
		} catch (NumberInvalidException e) {
			e.printStackTrace();
		}
		if (exBlocks != null) {
			Config.excludedBlocks = Arrays.asList(exBlocks);
		}

		Property overridableBlocksProp = Config.config.get("Balancing", "overridableBlocks",
				Helpers.serializeBlockArray(new Block[] { Blocks.air, Blocks.water, Blocks.leaves,
						Blocks.leaves2, Blocks.tallgrass, Blocks.red_flower, Blocks.yellow_flower,
						Blocks.snow_layer, Blocks.brown_mushroom, Blocks.red_mushroom }));
		overridableBlocksProp.comment = "List of block ids that can be overriden while teleporting blocks.\nPut there blocks that the player don't care about (grass, leaves) so they don't prevent the capsule from deploying.";
		
		Block[] ovBlocks = null;
		try {
			ovBlocks = Helpers.deserializeBlockArray(overridableBlocksProp.getStringList());
		} catch (NumberInvalidException e) {
			e.printStackTrace();
		}
		if (ovBlocks != null) {
			Config.overridableBlocks = Arrays.asList(ovBlocks);
		}
		
		Config.config.save();
	}

	public void serverAboutToStart(FMLServerAboutToStartEvent evt) {
		CapsuleDimensionRegistrer.registerDimension();
	}

	public void serverStarting(FMLServerStartingEvent e) {
		e.registerServerCommand(new CapsuleCommand());
	}

	public void openGuiScreen(EntityPlayer playerIn) {

	}
}
