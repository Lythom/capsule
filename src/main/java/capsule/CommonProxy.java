package capsule;

import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import capsule.blocks.CapsuleBlocksRegistrer;
import capsule.command.CapsuleCommand;
import capsule.enchantments.Enchantments;
import capsule.items.CapsuleItemsRegistrer;
import capsule.loot.CapsuleLootTableHook;
import capsule.loot.LootPathData;
import capsule.network.CapsuleContentPreviewQueryToServer;
import capsule.network.CapsuleThrowQueryHandler;
import capsule.network.CapsuleThrowQueryToServer;
import capsule.network.CapsuleContentPreviewQueryHandler;
import capsule.network.CapsuleContentPreviewAnswerToClient;
import capsule.network.LabelEditedMessageToServer;
import capsule.network.LabelEditedMessageToServerMessageHandler;
import capsule.network.CapsuleContentPreviewAnswerHandler;
import net.minecraft.block.Block;
import net.minecraft.command.NumberInvalidException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class CommonProxy {
	
	protected static final Logger LOGGER = LogManager.getLogger(CommonProxy.class);

	public static SimpleNetworkWrapper simpleNetworkWrapper;
	public static byte CAPSULE_CHANNEL_MESSAGE_ID = 1;

	public void preInit(FMLPreInitializationEvent event) {
		Config.config = new Configuration(event.getSuggestedConfigurationFile());
		Config.config.load();

		Enchantments.initEnchantments();
		CapsuleItemsRegistrer.createItems(Main.MODID);
		CapsuleBlocksRegistrer.createBlocks(Main.MODID);

		// network stuff
		simpleNetworkWrapper = NetworkRegistry.INSTANCE.newSimpleChannel("CapsuleChannel");
		// client ask server to edit capsule label
		simpleNetworkWrapper.registerMessage(LabelEditedMessageToServerMessageHandler.class, LabelEditedMessageToServer.class, CAPSULE_CHANNEL_MESSAGE_ID++, Side.SERVER);
		// client ask server data needed to preview a deploy
		simpleNetworkWrapper.registerMessage(CapsuleContentPreviewQueryHandler.class, CapsuleContentPreviewQueryToServer.class, CAPSULE_CHANNEL_MESSAGE_ID++, Side.SERVER);
		// client ask server to throw item to a specific position
		simpleNetworkWrapper.registerMessage(CapsuleThrowQueryHandler.class, CapsuleThrowQueryToServer.class, CAPSULE_CHANNEL_MESSAGE_ID++, Side.SERVER);
		// server sends to client the data needed to preview a deploy
		simpleNetworkWrapper.registerMessage(CapsuleContentPreviewAnswerHandler.class, CapsuleContentPreviewAnswerToClient.class, CAPSULE_CHANNEL_MESSAGE_ID++, Side.CLIENT);
	}

	public void init(FMLInitializationEvent event) {
		
		MinecraftForge.EVENT_BUS.register(Enchantments.recallEnchant);
	}

	public void postInit(FMLPostInitializationEvent event) {
		CapsuleItemsRegistrer.registerRecipes();
		CapsuleBlocksRegistrer.registerRecipes();
		
		// upgrade limits
		Property upgradesLimit = Config.config.get("Balancing", "capsuleUpgradesLimit", 10);
		upgradesLimit.setComment("Number of upgrades an empty capsules can get to improve capacity. If <= 0, the capsule won't be able to upgrade.");
		Config.upgradeLimit = upgradesLimit.getInt();
		
		// Excluded
		String[] excludedBlocksOP = ArrayUtils.addAll(
				Helpers.serializeBlockArray(new Block[] { Blocks.AIR, Blocks.STRUCTURE_VOID }),
				new String[] { 
						"ic2:te", 
						"opencomputers:robot", 
						"bloodmagic:BlockAlchemyTable",
						"tconstruct:seared","tconstruct:smeltery_controller","tconstruct:seared_tank","tconstruct:faucet","tconstruct:casting","tconstruct:smeltery_io","tconstruct:seared_glass"
				}
		);
		String[] excludedBlocks = ArrayUtils.addAll(
				Helpers.serializeBlockArray(new Block[] { Blocks.AIR, Blocks.BEDROCK, Blocks.MOB_SPAWNER, Blocks.END_PORTAL, Blocks.END_PORTAL_FRAME }),
				excludedBlocksOP
		);
		Property excludedBlocksProp = Config.config.get("Balancing", "excludedBlocks",excludedBlocks);
		excludedBlocksProp.setComment("List of block ids that will never be captured by a non overpowered capsule. While capturing, the blocks will stay in place.\n Ex: minecraft:mob_spawner");
		Block[] exBlocks = null;
		try {
			exBlocks = Helpers.deserializeBlockArray(excludedBlocksProp.getStringList());
		} catch (NumberInvalidException e) {
			LOGGER.error("Error while decoding excludedBlocksProp property from config/capsule.cfg", e);
		}
		if (exBlocks != null) {
			Config.excludedBlocks = Arrays.asList(exBlocks);
		}
		
		// OP Excluded
		Property opExcludedBlocksProp = Config.config.get("Balancing", "opExcludedBlocks",excludedBlocksOP);
		opExcludedBlocksProp.setComment("List of block ids that will never be captured even with an overpowered capsule. While capturing, the blocks will stay in place.\n Ex: minecraft:mob_spawner");
		Block[] opExBlocks = null;
		try {
			opExBlocks = Helpers.deserializeBlockArray(opExcludedBlocksProp.getStringList());
		} catch (NumberInvalidException e) {
			LOGGER.error("Error while decoding opExcludedBlocks property from config/capsule.cfg", e);
		}
		if (opExBlocks != null) {
			Config.opExcludedBlocks = Arrays.asList(opExBlocks);
		}
		
		// Overridable
		Property overridableBlocksProp = Config.config.get("Balancing", "overridableBlocks",
				Helpers.serializeBlockArray(new Block[] { Blocks.AIR, Blocks.WATER, Blocks.LEAVES,
						Blocks.LEAVES2, Blocks.TALLGRASS, Blocks.RED_FLOWER, Blocks.YELLOW_FLOWER,
						Blocks.SNOW_LAYER, Blocks.BROWN_MUSHROOM, Blocks.RED_MUSHROOM, Blocks.DOUBLE_PLANT }));
		overridableBlocksProp.setComment("List of block ids that can be overriden while teleporting blocks.\nPut there blocks that the player don't care about (grass, leaves) so they don't prevent the capsule from deploying.");
		
		Block[] ovBlocks = null;
		try {
			ovBlocks = Helpers.deserializeBlockArray(overridableBlocksProp.getStringList());
		} catch (NumberInvalidException e) {
			LOGGER.error("Error while decoding overridableBlocks property from config/capsule.cfg", e);
		}
		if (ovBlocks != null) {
			Config.overridableBlocks = Arrays.asList(ovBlocks);
		}
		
		// CapsuleTemplate Paths
		Property lootTemplatesPathsProp = Config.config.get("loots", "lootTemplatesPaths", new String[]{
				"config/capsules/loot/common",
				"config/capsules/loot/uncommon",
				"config/capsules/loot/rare",
				"assets/capsules/loot/common",
				"assets/capsules/loot/uncommon",
				"assets/capsules/loot/rare"
			});
		lootTemplatesPathsProp.setComment("List of paths where the mod will look for structureBlock files. Each save will have a chance to appear as a reward capsule in a dungeon chest.");
		Config.lootTemplatesPaths = lootTemplatesPathsProp.getStringList();
		
		Property rewardTemplatesPathProp = Config.config.get("loots", "rewardTemplatesPath", "config/capsules/rewards");
		rewardTemplatesPathProp.setComment("Paths where the mod will look for structureBlock files when invoking command /capsule fromStructure <structureName>.");
		Config.rewardTemplatesPath = rewardTemplatesPathProp.getString();
		
		// init paths properties from config
		for (int i = 0; i < Config.lootTemplatesPaths.length; i++) {
			String path = Config.lootTemplatesPaths[i];
			
			if(!Config.lootTemplatesData.containsKey(path)){
				Config.lootTemplatesData.put(path, new LootPathData());
			}
			Property pathDataWeight = Config.config.get("loots:"+path, "weight", path.endsWith("rare") ? 1 : path.endsWith("uncommon") ? 6 : 12);
			pathDataWeight.setComment("Chances to get a capsule from this folder. Higher means more common. Default : 1 (rare), 6 (uncommon) or 12 (common)");
			Config.lootTemplatesData.get(path).weigth = pathDataWeight.getInt();
		}
		
		Config.config.save();
		
		CapsuleLootTableHook lootTableHook = new CapsuleLootTableHook();
		MinecraftForge.EVENT_BUS.register(lootTableHook);

	}

	public void serverStarting(FMLServerStartingEvent e) {
		e.registerServerCommand(new CapsuleCommand());
		StructureSaver.loadLootList(e.getServer());
	}

	public void openGuiScreen(EntityPlayer playerIn) {

	}
}
