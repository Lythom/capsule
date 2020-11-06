package capsule.blocks;

import capsule.Main;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;

import java.util.Map;

public class CapsuleBlocks {

    private final static ResourceLocation CAPSULE_MARKER_REGISTERY_NAME = new ResourceLocation(Main.MODID, "capsulemarker");
    public final static ResourceLocation CAPSULE_MARKER_TE_REGISTERY_NAME = new ResourceLocation(Main.MODID, "capsulemarker_te");

    public static BlockCapsuleMarker CAPSULE_MARKER;

    // testing blocks
    public static BlockCaptureCrasher blockCaptureCrasher;
    public final static String CAPTURE_CRASHER_REGISTERY_NAME = "capturecrasher";

    public static BlockDeployCrasher blockDeployCrasher;
    public final static String DEPLOY_CRASHER_REGISTERY_NAME = "deploycrasher";

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
        BlockItem markerItem = new BlockItem(CAPSULE_MARKER, new Item.Properties().group(Main.tabCapsule));
        markerItem.setRegistryName(CAPSULE_MARKER_REGISTERY_NAME);
        event.getRegistry().register(markerItem);

        // testing blocks
        Map<String, String> env = System.getenv();
        if ("DEV".equals(env.get("__ENV__"))) {
            event.getRegistry().register(new BlockItem(blockCaptureCrasher, new Item.Properties().group(Main.tabCapsule)).setRegistryName(blockCaptureCrasher.getRegistryName()));
            event.getRegistry().register(new BlockItem(blockDeployCrasher, new Item.Properties().group(Main.tabCapsule)).setRegistryName(blockDeployCrasher.getRegistryName()));
        }
    }
}
