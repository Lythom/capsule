package capsule;

import capsule.blocks.CapsuleBlocksRegistrer;
import capsule.command.CapsuleCommand;
import capsule.enchantments.Enchantments;
import capsule.items.CapsuleItemsRegistrer;
import capsule.loot.CapsuleLootTableHook;
import capsule.network.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CommonProxy {

    public static SimpleNetworkWrapper simpleNetworkWrapper;
    public static byte CAPSULE_CHANNEL_MESSAGE_ID = 1;

    public void preInit(FMLPreInitializationEvent event) {
        Configuration config = new Configuration(event.getSuggestedConfigurationFile());
        Config.readConfig(config);

        Enchantments.initEnchantments();
        CapsuleItemsRegistrer.createItems();
        CapsuleBlocksRegistrer.createBlocks();

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

        Config.config.save();
        if (Config.config.hasChanged()) {
            Config.config.save();
        }

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
