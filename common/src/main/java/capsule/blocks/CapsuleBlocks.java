package capsule.blocks;

import capsule.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

public class CapsuleBlocks {

    public static final Supplier<BlockCapsuleMarker> CAPSULE_MARKER = Services.PLATFORM.register(BuiltInRegistries.BLOCK, "capsulemarker", BlockCapsuleMarker::new);
    public static final Supplier<BlockItem> CAPSULE_MARKER_ITEM = Services.PLATFORM.register(BuiltInRegistries.ITEM, "capsulemarker", () -> new BlockItem(CAPSULE_MARKER.get(), new Item.Properties()));
    public static final Supplier<BlockEntityType<BlockEntityCapture>> MARKER_TE = Services.PLATFORM.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "capsulemarker_te", () -> BlockEntityType.Builder.of(BlockEntityCapture::new, CAPSULE_MARKER.get()).build(null));

    public static void init() {
    }
}
