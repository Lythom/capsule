package capsule.blocks;

import capsule.Main;
import capsule.items.CapsuleItem;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class CapsuleBlocksRegistrer {

    public static BlockCapsuleMarker blockCapsuleMarker;
    public static String CAPSULE_MARKER_REGISTERY_NAME = "capsulemarker";
    public static String CAPSULE_MARKER_TE_REGISTERY_NAME = "capsulemarker-te";

    // testing blocks
    public static BlockCaptureCrasher blockCaptureCrasher;
    public static String CAPTURE_CRASHER_REGISTERY_NAME = "capturecrasher";

    public static void registerBlocks() {
        blockCapsuleMarker = new BlockCapsuleMarker(CAPSULE_MARKER_REGISTERY_NAME, Material.ROCK);
        blockCapsuleMarker.setCreativeTab(Main.tabCapsule);
        ItemBlock blockCapsuleMarkerItemBlock = new ItemBlock(blockCapsuleMarker);

        GameRegistry.register(blockCapsuleMarker.setRegistryName(CAPSULE_MARKER_REGISTERY_NAME));
        GameRegistry.register(blockCapsuleMarkerItemBlock.setRegistryName(CAPSULE_MARKER_REGISTERY_NAME));
        GameRegistry.registerTileEntity(TileEntityCapture.class, CAPSULE_MARKER_TE_REGISTERY_NAME);

        // testing blocks
        blockCaptureCrasher = new BlockCaptureCrasher(CAPTURE_CRASHER_REGISTERY_NAME, Material.ROCK);
        blockCaptureCrasher.setCreativeTab(Main.tabCapsule);
        ItemBlock blockCaptureCrasherItemBlock = new ItemBlock(blockCaptureCrasher);
        GameRegistry.register(blockCaptureCrasher.setRegistryName(CAPTURE_CRASHER_REGISTERY_NAME));
        GameRegistry.register(blockCaptureCrasherItemBlock.setRegistryName(CAPTURE_CRASHER_REGISTERY_NAME));
    }

    public static void registerRecipes() {
        ItemStack capsulemarker = new ItemStack(CapsuleBlocksRegistrer.blockCapsuleMarker, 1, CapsuleItem.STATE_EMPTY);

        // base recipes
        GameRegistry.addRecipe(capsulemarker, new Object[]{"#G#", "#C#", "#T#", '#', Blocks.COBBLESTONE, 'T', Blocks.TORCH, 'C', Items.COMPASS, 'G', Blocks.GLASS_PANE});

    }
}
