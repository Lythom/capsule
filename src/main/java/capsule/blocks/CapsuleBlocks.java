package capsule.blocks;

import capsule.Main;
import capsule.items.CapsuleItem;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
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

        blockCapsuleMarker = new BlockCapsuleMarker(CapsuleBlocks.CAPSULE_MARKER_REGISTERY_NAME, Material.ROCK);
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

    public static void registerItemBlocks(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(new ItemBlock(blockCapsuleMarker).setRegistryName(blockCapsuleMarker.getRegistryName()));

        // testing blocks
        Map<String, String> env = System.getenv();
        if ("DEV".equals(env.get("__ENV__"))) {
            event.getRegistry().register(new ItemBlock(blockCaptureCrasher).setRegistryName(blockCaptureCrasher.getRegistryName()));
            event.getRegistry().register(new ItemBlock(blockDeployCrasher).setRegistryName(blockDeployCrasher.getRegistryName()));
        }
    }
}
