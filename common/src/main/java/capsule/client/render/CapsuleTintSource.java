package capsule.client.render;

import capsule.CapsuleMod;
import capsule.items.CapsuleItem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Colors the layer (tint index) of a capsule item model: CapsuleItem#getColorFromItemstack with that layer as the
 * render pass. Registered as capsule:layer by each loader, used by the tints lists of assets/capsule/items/capsule.json.
 */
public record CapsuleTintSource(int layer) implements ItemTintSource {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(CapsuleMod.MODID, "layer");
    public static final MapCodec<CapsuleTintSource> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("layer").forGetter(CapsuleTintSource::layer)
    ).apply(i, CapsuleTintSource::new));

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        return CapsuleItem.getColorFromItemstack(stack, layer);
    }

    @Override
    public MapCodec<CapsuleTintSource> type() {
        return MAP_CODEC;
    }
}
