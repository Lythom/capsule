package capsule.blocks;

import capsule.CapsuleMod;
import capsule.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.Set;
import java.util.function.Supplier;

public class CapsuleBlocks {

    public static final Supplier<BlockCapsuleMarker> CAPSULE_MARKER = Services.PLATFORM.register(BuiltInRegistries.BLOCK, "capsulemarker",
            () -> new BlockCapsuleMarker(BlockBehaviour.Properties.of().setId(CapsuleMod.blockKey("capsulemarker"))));
    public static final Supplier<BlockItem> CAPSULE_MARKER_ITEM = Services.PLATFORM.register(BuiltInRegistries.ITEM, "capsulemarker",
            () -> new BlockItem(CAPSULE_MARKER.get(), new Item.Properties().useBlockDescriptionPrefix().setId(CapsuleMod.itemKey("capsulemarker"))));
    public static final Supplier<BlockEntityType<BlockEntityCapture>> MARKER_TE = Services.PLATFORM.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "capsulemarker_te",
            () -> new BlockEntityType<>(BlockEntityCapture::new, Set.of(CAPSULE_MARKER.get())));

    public static void init() {
    }
}
