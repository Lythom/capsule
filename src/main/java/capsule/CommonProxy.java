package capsule;

import capsule.blocks.CapsuleBlocks;
import capsule.blocks.TileEntityCapture;
import capsule.command.CapsuleCommand;
import capsule.enchantments.Enchantments;
import capsule.items.CapsuleItems;
import capsule.network.*;
import capsule.recipes.*;
import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.SpecialRecipeSerializer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(value = Dist.DEDICATED_SERVER, modid = Main.MODID, bus = Bus.MOD)
public class CommonProxy {

    protected static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(CommonProxy.class);

    private static final String PROTOCOL_VERSION = "1";
    public static SimpleChannel simpleNetworkWrapper = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("capsule", "capsule_channel"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    public static byte CAPSULE_CHANNEL_MESSAGE_ID = 1;

    public static final BlueprintCapsuleRecipe.Serializer BLUEPRINT_CAPSULE_SERIALIZER = register("blueprint_capsule", new BlueprintCapsuleRecipe.Serializer());
    public static final SpecialRecipeSerializer<BlueprintChangeRecipe> BLUEPRINT_CHANGE_SERIALIZER = register("blueprint_change", new SpecialRecipeSerializer<>(BlueprintChangeRecipe::new));
    public static final SpecialRecipeSerializer<ClearCapsuleRecipe> CLEAR_CAPSULE_SERIALIZER = register("clear_capsule", new SpecialRecipeSerializer<>(ClearCapsuleRecipe::new));
    public static final SpecialRecipeSerializer<DyeCapsuleRecipe> DYE_CAPSULE_SERIALIZER = register("dye_capsule", new SpecialRecipeSerializer<>(DyeCapsuleRecipe::new));
    public static final SpecialRecipeSerializer<PrefabsBlueprintAggregatorRecipe> PREFABS_AGGREGATOR_SERIALIZER = register("aggregate_all_prefabs", new SpecialRecipeSerializer<>(PrefabsBlueprintAggregatorRecipe::new));
    public static final RecoveryCapsuleRecipe.Serializer RECOVERY_CAPSULE_SERIALIZER = register("recovery_capsule", new RecoveryCapsuleRecipe.Serializer());
    public static final UpgradeCapsuleRecipe.Serializer UPGRADE_CAPSULE_SERIALIZER = register("upgrade_capsule", new UpgradeCapsuleRecipe.Serializer());

    public static final TileEntityType<TileEntityCapture> MARKER_TE = buildTileEntity(TileEntityCapture::new, CapsuleBlocks.CAPSULE_MARKER_TE_REGISTERY_NAME, CapsuleBlocks.CAPSULE_MARKER);

    private static <T extends IRecipeSerializer<? extends IRecipe<?>>> T register(final String name, final T t) {
        t.setRegistryName(new ResourceLocation(Main.MODID, name));
        return t;
    }

    @SubscribeEvent
    public static void registerRecipeSerializers(RegistryEvent.Register<IRecipeSerializer<?>> event) {
        event.getRegistry().register(BLUEPRINT_CAPSULE_SERIALIZER);
        event.getRegistry().register(BLUEPRINT_CHANGE_SERIALIZER);
        event.getRegistry().register(CLEAR_CAPSULE_SERIALIZER);
        event.getRegistry().register(DYE_CAPSULE_SERIALIZER);
        event.getRegistry().register(PREFABS_AGGREGATOR_SERIALIZER);
        event.getRegistry().register(RECOVERY_CAPSULE_SERIALIZER);
        event.getRegistry().register(UPGRADE_CAPSULE_SERIALIZER);
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

    private static <T extends TileEntity> TileEntityType<T> buildTileEntity(Supplier<T> supplier, ResourceLocation name, Block... blocks) {
        TileEntityType<T> te = TileEntityType.Builder.create(supplier, blocks).build(null);
        te.setRegistryName(name);
        return te;
    }

    @SubscribeEvent
    public static void registerTileEntities(final RegistryEvent.Register<TileEntityType<?>> event) {
        event.getRegistry().register(MARKER_TE);
    }

    @SubscribeEvent
    public static void registerEnchantments(RegistryEvent.Register<Enchantment> event) {
        Enchantments.registerEnchantments(event);
    }

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        Config.configDir = FMLPaths.CONFIGDIR.get().resolve("capsule-client.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_CONFIG);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.COMMON_CONFIG);
        Config.loadConfig(Config.CLIENT_CONFIG, FMLPaths.CONFIGDIR.get().resolve("capsule-client.toml"));
        Config.loadConfig(Config.COMMON_CONFIG, FMLPaths.CONFIGDIR.get().resolve("capsule-common.toml"));

        // network stuff
        // client ask server to edit capsule label
        simpleNetworkWrapper.registerMessage(CAPSULE_CHANNEL_MESSAGE_ID++, LabelEditedMessageToServer.class, LabelEditedMessageToServer::toBytes, LabelEditedMessageToServer::new, LabelEditedMessageToServer::onServer);
        // client ask server data needed to preview a deploy
        simpleNetworkWrapper.registerMessage(CAPSULE_CHANNEL_MESSAGE_ID++, CapsuleContentPreviewQueryToServer.class, CapsuleContentPreviewQueryToServer::toBytes, CapsuleContentPreviewQueryToServer::new, CapsuleContentPreviewQueryToServer::onServer);
        // client ask server to throw item to a specific position
        simpleNetworkWrapper.registerMessage(CAPSULE_CHANNEL_MESSAGE_ID++, CapsuleThrowQueryToServer.class, CapsuleThrowQueryToServer::toBytes, CapsuleThrowQueryToServer::new, CapsuleThrowQueryToServer::onServer);
        // client ask server to reload the held blueprint capsule
        simpleNetworkWrapper.registerMessage(CAPSULE_CHANNEL_MESSAGE_ID++, CapsuleLeftClickQueryToServer.class, CapsuleLeftClickQueryToServer::toBytes, CapsuleLeftClickQueryToServer::new, CapsuleLeftClickQueryToServer::onServer);
        // server sends to client the data needed to preview a deploy
        simpleNetworkWrapper.registerMessage(CAPSULE_CHANNEL_MESSAGE_ID++, CapsuleContentPreviewAnswerToClient.class, CapsuleContentPreviewAnswerToClient::toBytes, CapsuleContentPreviewAnswerToClient::new, CapsuleContentPreviewAnswerToClient::onClient);
        // server sends to client the data needed to render undeploy
        simpleNetworkWrapper.registerMessage(CAPSULE_CHANNEL_MESSAGE_ID++, CapsuleUndeployNotifToClient.class, CapsuleUndeployNotifToClient::toBytes, CapsuleUndeployNotifToClient::new, CapsuleUndeployNotifToClient::onClient);
    }

    @SubscribeEvent
    public static void serverStarting(FMLServerStartingEvent e) {
        CapsuleCommand.register(e.getCommandDispatcher());
    }

    public void openGuiScreen(PlayerEntity playerIn) {

    }
}
