package capsule.blocks;

import capsule.CapsuleMod;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;

import java.util.Map;
import java.util.function.Supplier;

public class CapsuleBlocks {

    private final static ResourceLocation CAPSULE_MARKER_REGISTERY_NAME = new ResourceLocation(CapsuleMod.MODID, "capsulemarker");
    public final static ResourceLocation CAPSULE_MARKER_TE_REGISTERY_NAME = new ResourceLocation(CapsuleMod.MODID, "capsulemarker_te");

    public static BlockCapsuleMarker CAPSULE_MARKER;
    public static TileEntityType<TileEntityCapture> MARKER_TE;

    // testing blocks
    public static BlockCaptureCrasher blockCaptureCrasher;
    public final static String CAPTURE_CRASHER_REGISTERY_NAME = "capturecrasher";

    public static BlockDeployCrasher blockDeployCrasher;
    public final static String DEPLOY_CRASHER_REGISTERY_NAME = "deploycrasher";

    private static <T extends TileEntity> TileEntityType<T> buildTileEntity(Supplier<T> supplier, ResourceLocation name, Block... blocks) {
        TileEntityType<T> te = TileEntityType.Builder.of(supplier, blocks).build(null);
        te.setRegistryName(name);
        return te;
    }

    public static void registerBlocks(final RegistryEvent.Register<Block> event) {
        CAPSULE_MARKER = new BlockCapsuleMarker();
        CAPSULE_MARKER.setRegistryName(CAPSULE_MARKER_REGISTERY_NAME);
        event.getRegistry().register(CAPSULE_MARKER);

        // testing blocks
        Map<String, String> env = System.getenv();
        if ("DEV".equals(env.get("__ENV__"))) {
            blockCaptureCrasher = new BlockCaptureCrasher();
            blockCaptureCrasher.setRegistryName(CapsuleBlocks.CAPTURE_CRASHER_REGISTERY_NAME);
            event.getRegistry().register(blockCaptureCrasher);

            blockDeployCrasher = new BlockDeployCrasher();
            blockDeployCrasher.setRegistryName(CapsuleBlocks.DEPLOY_CRASHER_REGISTERY_NAME);
            event.getRegistry().register(blockDeployCrasher);
        }
    }

    @SuppressWarnings("ConstantConditions")
    public static void registerItemBlocks(final RegistryEvent.Register<Item> event) {
        BlockItem markerItem = new BlockItem(CAPSULE_MARKER, new Item.Properties().tab(CapsuleMod.tabCapsule));
        markerItem.setRegistryName(CAPSULE_MARKER_REGISTERY_NAME);
        event.getRegistry().register(markerItem);

        // testing blocks
        Map<String, String> env = System.getenv();
        if ("DEV".equals(env.get("__ENV__"))) {
            event.getRegistry().register(new BlockItem(blockCaptureCrasher, new Item.Properties().tab(CapsuleMod.tabCapsule)).setRegistryName(blockCaptureCrasher.getRegistryName()));
            event.getRegistry().register(new BlockItem(blockDeployCrasher, new Item.Properties().tab(CapsuleMod.tabCapsule)).setRegistryName(blockDeployCrasher.getRegistryName()));
        }
    }

    public static void registerTileEntities(final RegistryEvent.Register<TileEntityType<?>> event) {
        MARKER_TE = buildTileEntity(TileEntityCapture::new, CapsuleBlocks.CAPSULE_MARKER_TE_REGISTERY_NAME, CapsuleBlocks.CAPSULE_MARKER);
        event.getRegistry().register(MARKER_TE);
    }

    @OnlyIn(Dist.CLIENT)
    public static void bindTileEntitiesRenderer() {
        ClientRegistry.bindTileEntityRenderer(MARKER_TE, CaptureTESR::new);
    }
}
