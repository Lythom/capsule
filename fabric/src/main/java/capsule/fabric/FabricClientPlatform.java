package capsule.fabric;

import capsule.platform.ClientPlatform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class FabricClientPlatform implements ClientPlatform {

    @Override
    public void tesselateWithAO(ModelBlockRenderer renderer, BlockAndTintGetter level, BakedModel model, BlockState state, BlockPos pos,
                                PoseStack poseStack, VertexConsumer consumer, RandomSource random, long seed, int packedOverlay) {
        renderer.tesselateWithAO(level, model, state, pos, poseStack, consumer, true, random, seed, packedOverlay);
    }

    @Override
    public TextureAtlasSprite getFluidStillSprite(FluidState fluid, BlockAndTintGetter level, BlockPos pos) {
        FluidRenderHandler handler = FluidRenderHandlerRegistry.INSTANCE.get(fluid.getType());
        return handler != null
                ? handler.getFluidSprites(level, pos, fluid)[0]
                : Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(fluid.createLegacyBlock());
    }

    @Override
    public int getFluidTint(FluidState fluid, BlockAndTintGetter level, BlockPos pos) {
        FluidRenderHandler handler = FluidRenderHandlerRegistry.INSTANCE.get(fluid.getType());
        return handler != null ? handler.getFluidColor(level, pos, fluid) : -1;
    }
}
