package capsule.blocks;

import capsule.client.CapsulePreviewHandler;
import capsule.helpers.Spacial;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public class CaptureBER implements BlockEntityRenderer<BlockEntityCapture> {

    double time = 0;

    public CaptureBER(BlockEntityRendererProvider.Context ctx) {
        time = Math.random() * 10000;
    }


    @Override
    public boolean shouldRenderOffScreen(BlockEntityCapture te) {
        return true;
    }

    @Override
    public void render(BlockEntityCapture blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLightIn, int combinedOverlayIn) {
        time += partialTicks;
        int size = blockEntity.getSize();
        if (size == 0)
            return;
        int extendSize = (size - 1) / 2;
        int color = blockEntity.getColor();
        BlockPos offset = Spacial.getAnchor(BlockPos.ZERO, blockEntity.getBlockState(), size);
        AABB boundingBox = Spacial.getBB(offset.getX(), offset.getY(), offset.getZ(), size, extendSize);
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.lines());
        CapsulePreviewHandler.renderRecallBox(poseStack, color, boundingBox, buffer, time);
    }

    /**
     * @return an appropriately size AABB for the BlockEntity. Overrides IBlockEntityRendererExtension#getRenderBoundingBox
     * on NeoForge, which culls block entity renderers with it.
     */
    public AABB getRenderBoundingBox(BlockEntityCapture blockEntity) {
        return blockEntity.getBoundingBox();
    }
}
