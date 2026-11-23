package capsule.client.render;

import capsule.CapsuleMod;
import capsule.items.CapsuleItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The capsule state (CapsuleItem.CapsuleState#getValue, 0 to 7) as a numeric item model property, read by the
 * range_dispatch of assets/capsule/items/capsule.json. Registered as capsule:state by each loader.
 */
public record CapsuleStateProperty() implements RangeSelectItemModelProperty {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(CapsuleMod.MODID, "state");
    public static final MapCodec<CapsuleStateProperty> MAP_CODEC = MapCodec.unit(new CapsuleStateProperty());

    @Override
    public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        return CapsuleItem.getState(stack).getValue();
    }

    @Override
    public MapCodec<CapsuleStateProperty> type() {
        return MAP_CODEC;
    }
}
