package capsule.blocks;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;

import java.util.Map;

public class CapsuleBlocks {

    public static BlockCapsuleMarker blockCapsuleMarker;
    public final static String CAPSULE_MARKER_REGISTERY_NAME = "capsulemarker";
    public final static String CAPSULE_MARKER_TE_REGISTERY_NAME = "capsulemarker-te";

    // testing blocks
    public static BlockCaptureCrasher blockCaptureCrasher;
    public final static String CAPTURE_CRASHER_REGISTERY_NAME = "capturecrasher";

    public static BlockDeployCrasher blockDeployCrasher;
    public final static String DEPLOY_CRASHER_REGISTERY_NAME = "deploycrasher";


    public static void registerBlocks(RegistryEvent.Register<Block> event) {

        blockCapsuleMarker = new BlockCapsuleMarker();
        blockCapsuleMarker.setRegistryName(CapsuleBlocks.CAPSULE_MARKER_REGISTERY_NAME);
        event.getRegistry().register(blockCapsuleMarker);

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
    public static void registerItemBlocks(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(new BlockItem(blockCapsuleMarker, new Item.Properties()).setRegistryName(blockCapsuleMarker.getRegistryName()));

        // testing blocks
        Map<String, String> env = System.getenv();
        if ("DEV".equals(env.get("__ENV__"))) {
            event.getRegistry().register(new BlockItem(blockCaptureCrasher, new Item.Properties()).setRegistryName(blockCaptureCrasher.getRegistryName()));
            event.getRegistry().register(new BlockItem(blockDeployCrasher, new Item.Properties()).setRegistryName(blockDeployCrasher.getRegistryName()));
        }
    }
}
