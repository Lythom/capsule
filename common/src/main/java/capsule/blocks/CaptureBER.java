package capsule.blocks;

import capsule.client.CapsulePreviewHandler;
import capsule.helpers.Spacial;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The capture zone of a capture base while an empty capsule is in hand: the recall box of CapsulePreviewHandler around
 * the cube the capsule would capture.
 */
public class CaptureBER implements BlockEntityRenderer<BlockEntityCapture, CaptureBER.State> {

    public static class State extends BlockEntityRenderState {
        /**
         * Null when nothing is drawn (capture size 0). In the block's space, as the submit pose stack is.
         */
        @Nullable AABB box;
        int color;
        double time;
        float lineWidth;
    }

    double time = 0;

    public CaptureBER(BlockEntityRendererProvider.Context ctx) {
        time = Math.random() * 10000;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BlockEntityCapture blockEntity, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        // advanced once per frame and per capture base, as 1.21.1's render did
        time += partialTicks;
        int size = blockEntity.getSize();
        if (size == 0) {
            state.box = null;
            return;
        }
        int extendSize = (size - 1) / 2;
        BlockPos offset = Spacial.getAnchor(BlockPos.ZERO, blockEntity.getBlockState(), size);
        state.box = Spacial.getBB(offset.getX(), offset.getY(), offset.getZ(), size, extendSize);
        state.color = blockEntity.getColor();
        state.time = time;
        state.lineWidth = Minecraft.getInstance().getWindow().getAppropriateLineWidth();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.box == null) return;
        CapsulePreviewHandler.renderRecallBox(collector, poseStack, state.color, state.box, state.time, state.lineWidth);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    /**
     * @return an appropriately size AABB for the BlockEntity. Overrides IBlockEntityRendererExtension#getRenderBoundingBox
     * on NeoForge, which culls block entity renderers with it.
     */
    public AABB getRenderBoundingBox(BlockEntityCapture blockEntity) {
        return blockEntity.getBoundingBox();
    }
}
