package capsule;

import capsule.blocks.CapsuleBlocks;
import capsule.command.CapsuleCommand;
import capsule.enchantments.Enchantments;
import capsule.helpers.Files;
import capsule.items.CapsuleItems;
import capsule.loot.CapsuleLootTableHook;
import capsule.loot.StarterLoot;
import capsule.network.*;
import capsule.network.client.CapsuleContentPreviewAnswerHandler;
import capsule.network.client.CapsuleFullContentAnswerHandler;
import capsule.network.client.CapsuleUndeployNotifHandler;
import capsule.network.server.CapsuleContentPreviewQueryHandler;
import capsule.network.server.CapsuleLeftClickQueryHandler;
import capsule.network.server.CapsuleThrowQueryHandler;
import capsule.network.server.LabelEditedMessageToServerHandler;
import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import java.io.File;
import java.util.ArrayList;

@Mod.EventBusSubscriber
public class CommonProxy {

    public static SimpleNetworkWrapper simpleNetworkWrapper;
    public static byte CAPSULE_CHANNEL_MESSAGE_ID = 1;

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        CapsuleBlocks.registerBlocks(event);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        CapsuleBlocks.registerItemBlocks(event);
        CapsuleItems.registerItems(event);
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        ArrayList<String> prefabsTemplatesList = Files.populatePrefabs(Config.configDir, Config.prefabsTemplatesPath);
        CapsuleItems.registerRecipes(event, prefabsTemplatesList);
        // + other recipes in assets.capsule.recipes
    }

    @SubscribeEvent
    public static void registerEnchantments(RegistryEvent.Register<Enchantment> event) {
        Enchantments.registerEnchantments(event);
    }

    public void preInit(FMLPreInitializationEvent event) {
        Config.configDir = new File(event.getModConfigurationDirectory(), "capsule");
        Configuration config = new Configuration(event.getSuggestedConfigurationFile());
        Config.readConfig(config);
        Config.initLootConfigs();
        Config.initRecipesConfigs();
        Config.initEnchantsConfigs();

        // copy default config and structures to config/capsule folder, and load them in Config.
        refreshConfigTemplates();

        // network stuff
        simpleNetworkWrapper = NetworkRegistry.INSTANCE.newSimpleChannel("CapsuleChannel");
        // client ask server to edit capsule label
        simpleNetworkWrapper.registerMessage(LabelEditedMessageToServerHandler.class, LabelEditedMessageToServer.class, CAPSULE_CHANNEL_MESSAGE_ID++, Side.SERVER);
        // client ask server data needed to preview a deploy
        simpleNetworkWrapper.registerMessage(CapsuleContentPreviewQueryHandler.class, CapsuleContentPreviewQueryToServer.class, CAPSULE_CHANNEL_MESSAGE_ID++, Side.SERVER);
        // client ask server to throw item to a specific position
        simpleNetworkWrapper.registerMessage(CapsuleThrowQueryHandler.class, CapsuleThrowQueryToServer.class, CAPSULE_CHANNEL_MESSAGE_ID++, Side.SERVER);
        // client ask server to reload the held blueprint capsule
        simpleNetworkWrapper.registerMessage(CapsuleLeftClickQueryHandler.class, CapsuleLeftClickQueryToServer.class, CAPSULE_CHANNEL_MESSAGE_ID++, Side.SERVER);
        // server sends to client the data needed to preview a deploy
        simpleNetworkWrapper.registerMessage(CapsuleContentPreviewAnswerHandler.class, CapsuleContentPreviewAnswerToClient.class, CAPSULE_CHANNEL_MESSAGE_ID++, Side.CLIENT);
        simpleNetworkWrapper.registerMessage(CapsuleFullContentAnswerHandler.class, CapsuleFullContentAnswerToClient.class, CAPSULE_CHANNEL_MESSAGE_ID++, Side.CLIENT);
        // server sends to client the data needed to render undeploy
        simpleNetworkWrapper.registerMessage(CapsuleUndeployNotifHandler.class, CapsuleUndeployNotifToClient.class, CAPSULE_CHANNEL_MESSAGE_ID++, Side.CLIENT);
    }

    public void refreshConfigTemplates() {
        Files.populateAndLoadLootList(Config.configDir, Config.lootTemplatesPaths, Config.lootTemplatesData);
        Config.starterTemplatesList = Files.populateStarters(Config.configDir, Config.starterTemplatesPath);
        Config.blueprintWhitelist = Files.populateWhitelistConfig(Config.configDir);
    }

    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(Enchantments.recallEnchant);
        MinecraftForge.EVENT_BUS.register(CapsuleItems.capsule);
        MinecraftForge.EVENT_BUS.register(StarterLoot.instance);
    }

    public void postInit(FMLPostInitializationEvent event) {
        Config.initCaptureConfigs();
        if (Config.config.hasChanged()) {
            Config.config.save();
        }

        CapsuleLootTableHook lootTableHook = new CapsuleLootTableHook();
        MinecraftForge.EVENT_BUS.register(lootTableHook);
    }

    public void serverStarting(FMLServerStartingEvent e) {
        e.registerServerCommand(new CapsuleCommand());
        refreshConfigTemplates();
    }

    public void openGuiScreen(EntityPlayer playerIn) {

    }
}
