package capsule.blocks;

import capsule.CapsuleMod;
import com.mojang.datafixers.types.Type;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.RegistryEvent;

import java.util.Map;

public class CapsuleBlocks {

    private final static ResourceLocation CAPSULE_MARKER_REGISTERY_NAME = new ResourceLocation(CapsuleMod.MODID, "capsulemarker");
    public final static ResourceLocation CAPSULE_MARKER_TE_REGISTERY_NAME = new ResourceLocation(CapsuleMod.MODID, "capsulemarker_te");

    public static BlockCapsuleMarker CAPSULE_MARKER;
    public static BlockEntityType<BlockEntityCapture> MARKER_TE;

    // testing blocks
    public static BlockCaptureCrasher blockCaptureCrasher;
    public final static String CAPTURE_CRASHER_REGISTERY_NAME = "capturecrasher";

    public static BlockDeployCrasher blockDeployCrasher;
    public final static String DEPLOY_CRASHER_REGISTERY_NAME = "deploycrasher";

    private static <T extends BlockEntity> BlockEntityType<T> buildBlockEntity(BlockEntityType.BlockEntitySupplier<T> supplier, ResourceLocation name, Block... blocks) {
        Type<?> type = Util.fetchChoiceType(References.BLOCK_ENTITY, name.toString());
        BlockEntityType<T> te = BlockEntityType.Builder.of(supplier, blocks).build(type);
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

    public static void registerBlockEntities(final RegistryEvent.Register<BlockEntityType<?>> event) {
        MARKER_TE = buildBlockEntity(BlockEntityCapture::new, CapsuleBlocks.CAPSULE_MARKER_TE_REGISTERY_NAME, CapsuleBlocks.CAPSULE_MARKER);
        event.getRegistry().register(MARKER_TE);
    }

    @OnlyIn(Dist.CLIENT)
    public static void registerBlockEntitiesRenderer(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(MARKER_TE, CaptureBER::new);
    }
}
