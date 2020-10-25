package capsule.blocks;

import capsule.Main;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

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
        blockCapsuleMarker.setCreativeTab(Main.tabCapsule);

        event.getRegistry().register(blockCapsuleMarker);
        GameRegistry.registerTileEntity(TileEntityCapture.class, new ResourceLocation("capsule", CapsuleBlocks.CAPSULE_MARKER_TE_REGISTERY_NAME));

        // testing blocks
        Map<String, String> env = System.getenv();
        if ("DEV".equals(env.get("__ENV__"))) {
            blockCaptureCrasher =  new BlockCaptureCrasher(CapsuleBlocks.CAPTURE_CRASHER_REGISTERY_NAME, Material.ROCK);
            blockCaptureCrasher.setRegistryName(CapsuleBlocks.CAPTURE_CRASHER_REGISTERY_NAME);
            blockCaptureCrasher.setCreativeTab(Main.tabCapsule);
            event.getRegistry().register(blockCaptureCrasher);

            blockDeployCrasher = new BlockDeployCrasher(CapsuleBlocks.DEPLOY_CRASHER_REGISTERY_NAME, Material.ROCK);
            blockDeployCrasher.setRegistryName(CapsuleBlocks.DEPLOY_CRASHER_REGISTERY_NAME);
            blockDeployCrasher.setCreativeTab(Main.tabCapsule);
            event.getRegistry().register(blockDeployCrasher);
        }
    }

    @SuppressWarnings("ConstantConditions")
    public static void registerItemBlocks(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(new BlockItem(blockCapsuleMarker).setRegistryName(blockCapsuleMarker.getRegistryName()));

        // testing blocks
        Map<String, String> env = System.getenv();
        if ("DEV".equals(env.get("__ENV__"))) {
            event.getRegistry().register(new BlockItem(blockCaptureCrasher).setRegistryName(blockCaptureCrasher.getRegistryName()));
            event.getRegistry().register(new BlockItem(blockDeployCrasher).setRegistryName(blockDeployCrasher.getRegistryName()));
        }
    }
}
