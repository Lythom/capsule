package capsule.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public interface ClientPlatform {

    /**
     * ModelBlockRenderer#tesselateWithAO of every render type of the model, with the loader's empty model data.
     */
    void tesselateWithAO(ModelBlockRenderer renderer, BlockAndTintGetter level, BakedModel model, BlockState state, BlockPos pos,
                         PoseStack poseStack, VertexConsumer consumer, RandomSource random, long seed, int packedOverlay);

    TextureAtlasSprite getFluidStillSprite(FluidState fluid, BlockAndTintGetter level, BlockPos pos);

    int getFluidTint(FluidState fluid, BlockAndTintGetter level, BlockPos pos);
}
