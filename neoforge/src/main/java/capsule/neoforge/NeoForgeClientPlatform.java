package capsule.neoforge;

import capsule.platform.ClientPlatform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.data.ModelData;

public class NeoForgeClientPlatform implements ClientPlatform {

    @Override
    public void tesselateWithAO(ModelBlockRenderer renderer, BlockAndTintGetter level, BakedModel model, BlockState state, BlockPos pos,
                                PoseStack poseStack, VertexConsumer consumer, RandomSource random, long seed, int packedOverlay) {
        renderer.tesselateWithAO(level, model, state, pos, poseStack, consumer, true, random, seed, packedOverlay, ModelData.EMPTY, null);
    }

    @Override
    public TextureAtlasSprite getFluidStillSprite(FluidState fluid, BlockAndTintGetter level, BlockPos pos) {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(IClientFluidTypeExtensions.of(fluid).getStillTexture());
    }

    @Override
    public int getFluidTint(FluidState fluid, BlockAndTintGetter level, BlockPos pos) {
        return IClientFluidTypeExtensions.of(fluid).getTintColor(fluid, level, pos);
    }
}
