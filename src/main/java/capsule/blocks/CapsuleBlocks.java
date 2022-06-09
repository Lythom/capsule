package capsule.blocks;

import capsule.CapsuleMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class CapsuleBlocks {

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, CapsuleMod.MODID);
    private static final DeferredRegister<Item> BLOCKITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, CapsuleMod.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, CapsuleMod.MODID);

    public static final RegistryObject<Block> CAPSULE_MARKER = BLOCKS.register("capsulemarker", BlockCapsuleMarker::new);
    public static final RegistryObject<BlockItem> CAPSULE_MARKER_ITEM = BLOCKITEMS.register("capsulemarker", () -> new BlockItem(CAPSULE_MARKER.get(), new Item.Properties().tab(CapsuleMod.tabCapsule)));
    public static final RegistryObject<BlockEntityType<BlockEntityCapture>> MARKER_TE = BLOCK_ENTITIES.register("capsulemarker_te", () -> BlockEntityType.Builder.of(BlockEntityCapture::new, CAPSULE_MARKER.get()).build(null));

    public static void registerBlocks(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        BLOCKITEMS.register(modEventBus);
    }

    @OnlyIn(Dist.CLIENT)
    public static void registerBlockEntitiesRenderer(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(MARKER_TE.get(), CaptureBER::new);
    }
}
