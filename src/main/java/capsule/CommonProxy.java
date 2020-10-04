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
import capsule.network.client.CapsuleUndeployNotifHandler;
import capsule.network.server.CapsuleContentPreviewQueryHandler;
import capsule.network.server.CapsuleLeftClickQueryHandler;
import capsule.network.server.CapsuleThrowQueryHandler;
import capsule.network.server.LabelEditedMessageToServerHandler;
import capsule.recipes.*;
import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.SpecialRecipeSerializer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.ArrayList;

@Mod.EventBusSubscriber
public class CommonProxy {

    private static final String PROTOCOL_VERSION = "1";
    public static SimpleChannel simpleNetworkWrapper = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation("capsule", "CapsuleChannel"))
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .simpleChannel();
    public static byte CAPSULE_CHANNEL_MESSAGE_ID = 1;

    public static final IRecipeSerializer<BlueprintCapsuleRecipe> BLUEPRINT_CAPSULE_SERIALIZER = register("blueprint_capsule", new BlueprintCapsuleRecipe.Serializer());
    public static final SpecialRecipeSerializer<BlueprintChangeRecipe> BLUEPRINT_CHANGE_SERIALIZER = register("blueprint_change", new SpecialRecipeSerializer<>(BlueprintChangeRecipe::new));
    public static final SpecialRecipeSerializer<ClearCapsuleRecipe> CLEAR_CAPSULE_SERIALIZER = register("clear_capsule", new SpecialRecipeSerializer<>(ClearCapsuleRecipe::new));
    public static final SpecialRecipeSerializer<DyeCapsuleRecipe> DYE_CAPSULE_SERIALIZER = register("dye_capsule", new SpecialRecipeSerializer<>(DyeCapsuleRecipe::new));
    public static final SpecialRecipeSerializer<PrefabsBlueprintAggregatorRecipe> PREFABS_AGGREGATOR_SERIALIZER = register("aggregate_all_prefabs", new SpecialRecipeSerializer<>(PrefabsBlueprintAggregatorRecipe::new));

    private static <T extends IRecipeSerializer<? extends IRecipe<?>>> T register(final String name, final T t) {
        t.setRegistryName(new ResourceLocation(Main.MODID, name));
        return t;
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipeSerializer<?>> event) {
        event.getRegistry().register(BLUEPRINT_CAPSULE_SERIALIZER);
        event.getRegistry().register(BLUEPRINT_CHANGE_SERIALIZER);
        event.getRegistry().register(DYE_CAPSULE_SERIALIZER);
        event.getRegistry().register(CLEAR_CAPSULE_SERIALIZER);
        event.getRegistry().register(PREFABS_AGGREGATOR_SERIALIZER);

        ArrayList<String> prefabsTemplatesList = Files.populatePrefabs(Config.configDir.toFile(), Config.prefabsTemplatesPath.get());
        CapsuleItems.registerRecipes(event, prefabsTemplatesList);
        // + other recipes in assets.capsule.recipes
    }

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
    public static void registerEnchantments(RegistryEvent.Register<Enchantment> event) {
        Enchantments.registerEnchantments(event);
    }

    public void setup(FMLCommonSetupEvent event) {
        Config.configDir = FMLPaths.CONFIGDIR.get().resolve("capsule-client.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_CONFIG);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.COMMON_CONFIG);
        Config.loadConfig(Config.CLIENT_CONFIG, FMLPaths.CONFIGDIR.get().resolve("capsule-client.toml"));
        Config.loadConfig(Config.COMMON_CONFIG, FMLPaths.CONFIGDIR.get().resolve("capsule-common.toml"));

        // network stuff
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
        // server sends to client the data needed to render undeploy
        simpleNetworkWrapper.registerMessage(CapsuleUndeployNotifHandler.class, CapsuleUndeployNotifToClient.class, CAPSULE_CHANNEL_MESSAGE_ID++, Side.CLIENT);
    }

    public void serverStarting(FMLServerStartingEvent e) {
        e.registerServerCommand(new CapsuleCommand());
    }

    public void openGuiScreen(PlayerEntity playerIn) {

    }
}
