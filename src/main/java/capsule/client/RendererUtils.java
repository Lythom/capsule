package capsule.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class RendererUtils {
    public static void doPositionPrologue(Camera camera, PoseStack poseStack) {
        poseStack.pushPose();
        poseStack.translate(-camera.getPosition().x, -camera.getPosition().y, -camera.getPosition().z);
    }

    public static void doPositionEpilogue(PoseStack poseStack) {
        poseStack.popPose();
    }

    public static void doOverlayPrologue() {
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
    }

    public static void doOverlayEpilogue() {
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public static void doWirePrologue() {
        RenderSystem.disableCull();
        RenderSystem.lineWidth(3.0F);
    }

    public static void doWireEpilogue() {
        RenderSystem.lineWidth(1.0F);
        RenderSystem.enableCull();

    }

    public static void drawCube(PoseStack poseStack, final BlockPos pos, final float sizeOffset, final VertexConsumer buffer, int r, int g, int b, int a) {
        drawCube(poseStack, pos.getX() - sizeOffset, pos.getY() - sizeOffset, pos.getZ() - sizeOffset,
                pos.getX() + 1 + sizeOffset, pos.getY() + 1 + sizeOffset, pos.getZ() + 1 + sizeOffset,
                buffer, r, g, b, a);
    }

    public static void drawCube(PoseStack poseStack, final float minX, final float minY, final float minZ,
                                final float maxX, final float maxY, final float maxZ,
                                final VertexConsumer buffer, int r, int g, int b, int a) {
        drawPlaneNegX(poseStack, minX, minY, maxY, minZ, maxZ, buffer, r, g, b, a);
        drawPlanePosX(poseStack, maxX, minY, maxY, minZ, maxZ, buffer, r, g, b, a);
        drawPlaneNegY(poseStack, minY, minX, maxX, minZ, maxZ, buffer, r, g, b, a);
        drawPlanePosY(poseStack, maxY, minX, maxX, minZ, maxZ, buffer, r, g, b, a);
        drawPlaneNegZ(poseStack, minZ, minX, maxX, minY, maxY, buffer, r, g, b, a);
        drawPlanePosZ(poseStack, maxZ, minX, maxX, minY, maxY, buffer, r, g, b, a);
    }

    public static void drawPlaneNegX(PoseStack poseStack, final float x, final float minY, final float maxY,
                                     final float minZ, final float maxZ, final VertexConsumer buffer,
                                     int r, int g, int b, int a) {
        Matrix4f matrix4f = poseStack.last().pose();
        Matrix3f matrix3f = poseStack.last().normal();
        buffer.addVertex(matrix4f, x, minY, minZ).setColor(r, g, b, a).setNormal(matrix3f, -1, 0, 0);
        buffer.addVertex(matrix4f, x, minY, maxZ).setColor(r, g, b, a).setNormal(matrix3f, -1, 0, 0);
        buffer.addVertex(matrix4f, x, maxY, maxZ).setColor(r, g, b, a).setNormal(matrix3f, -1, 0, 0);
        buffer.addVertex(matrix4f, x, maxY, minZ).setColor(r, g, b, a).setNormal(matrix3f, -1, 0, 0);
    }

    public static void drawPlanePosX(PoseStack poseStack, final float x, final float minY, final float maxY,
                                     final float minZ, final float maxZ, final VertexConsumer buffer,
                                     int r, int g, int b, int a) {
        Matrix4f matrix4f = poseStack.last().pose();
        Matrix3f matrix3f = poseStack.last().normal();
        buffer.addVertex(matrix4f, x, minY, minZ).setColor(r, g, b, a).setNormal(matrix3f, 1, 0, 0);
        buffer.addVertex(matrix4f, x, maxY, minZ).setColor(r, g, b, a).setNormal(matrix3f, 1, 0, 0);
        buffer.addVertex(matrix4f, x, maxY, maxZ).setColor(r, g, b, a).setNormal(matrix3f, 1, 0, 0);
        buffer.addVertex(matrix4f, x, minY, maxZ).setColor(r, g, b, a).setNormal(matrix3f, 1, 0, 0);
    }

    public static void drawPlaneNegY(PoseStack poseStack, final float y, final float minX, final float maxX,
                                     final float minZ, final float maxZ, final VertexConsumer buffer,
                                     int r, int g, int b, int a) {
        Matrix4f matrix4f = poseStack.last().pose();
        Matrix3f matrix3f = poseStack.last().normal();
        buffer.addVertex(matrix4f, minX, y, minZ).setColor(r, g, b, a).setNormal(matrix3f, 0, -1, 0);
        buffer.addVertex(matrix4f, maxX, y, minZ).setColor(r, g, b, a).setNormal(matrix3f, 0, -1, 0);
        buffer.addVertex(matrix4f, maxX, y, maxZ).setColor(r, g, b, a).setNormal(matrix3f, 0, -1, 0);
        buffer.addVertex(matrix4f, minX, y, maxZ).setColor(r, g, b, a).setNormal(matrix3f, 0, -1, 0);
    }

    public static void drawPlanePosY(PoseStack poseStack, final float y, final float minX, final float maxX,
                                     final float minZ, final float maxZ, final VertexConsumer buffer,
                                     int r, int g, int b, int a) {
        Matrix4f matrix4f = poseStack.last().pose();
        Matrix3f matrix3f = poseStack.last().normal();
        buffer.addVertex(matrix4f, minX, y, minZ).setColor(r, g, b, a).setNormal(matrix3f, 0, 1, 0);
        buffer.addVertex(matrix4f, minX, y, maxZ).setColor(r, g, b, a).setNormal(matrix3f, 0, 1, 0);
        buffer.addVertex(matrix4f, maxX, y, maxZ).setColor(r, g, b, a).setNormal(matrix3f, 0, 1, 0);
        buffer.addVertex(matrix4f, maxX, y, minZ).setColor(r, g, b, a).setNormal(matrix3f, 0, 1, 0);
    }

    public static void drawPlaneNegZ(PoseStack poseStack, final float z, final float minX, final float maxX,
                                     final float minY, final float maxY, final VertexConsumer buffer,
                                     int r, int g, int b, int a) {
        Matrix4f matrix4f = poseStack.last().pose();
        Matrix3f matrix3f = poseStack.last().normal();
        buffer.addVertex(matrix4f, minX, minY, z).setColor(r, g, b, a).setNormal(matrix3f, 0, 0, -1);
        buffer.addVertex(matrix4f, minX, maxY, z).setColor(r, g, b, a).setNormal(matrix3f, 0, 0, -1);
        buffer.addVertex(matrix4f, maxX, maxY, z).setColor(r, g, b, a).setNormal(matrix3f, 0, 0, -1);
        buffer.addVertex(matrix4f, maxX, minY, z).setColor(r, g, b, a).setNormal(matrix3f, 0, 0, -1);
    }

    public static void drawPlanePosZ(PoseStack poseStack, final float z, final float minX, final float maxX,
                                     final float minY, final float maxY, final VertexConsumer buffer,
                                     int r, int g, int b, int a) {
        Matrix4f matrix4f = poseStack.last().pose();
        Matrix3f matrix3f = poseStack.last().normal();
        buffer.addVertex(matrix4f, minX, minY, z).setColor(r, g, b, a).setNormal(matrix3f, 0, 0, 1);
        buffer.addVertex(matrix4f, maxX, minY, z).setColor(r, g, b, a).setNormal(matrix3f, 0, 0, 1);
        buffer.addVertex(matrix4f, maxX, maxY, z).setColor(r, g, b, a).setNormal(matrix3f, 0, 0, 1);
        buffer.addVertex(matrix4f, minX, maxY, z).setColor(r, g, b, a).setNormal(matrix3f, 0, 0, 1);
    }

    public static void drawCapsuleCube(PoseStack poseStack, AABB boundingBox, VertexConsumer bufferBuilder, int r, int g, int b, int a) {
        drawPlaneNegX(poseStack, (float) boundingBox.minX, (float) boundingBox.minY, (float) boundingBox.maxY, (float) boundingBox.minZ, (float) boundingBox.maxZ, bufferBuilder, r, g, b, a);
        drawPlanePosX(poseStack, (float) boundingBox.maxX, (float) boundingBox.minY, (float) boundingBox.maxY, (float) boundingBox.minZ, (float) boundingBox.maxZ, bufferBuilder, r, g, b, a);
        drawPlaneNegY(poseStack, (float) boundingBox.minY, (float) boundingBox.minX, (float) boundingBox.maxX, (float) boundingBox.minZ, (float) boundingBox.maxZ, bufferBuilder, r, g, b, a);
        drawPlanePosY(poseStack, (float) boundingBox.maxY, (float) boundingBox.minX, (float) boundingBox.maxX, (float) boundingBox.minZ, (float) boundingBox.maxZ, bufferBuilder, r, g, b, a);
        drawPlaneNegZ(poseStack, (float) boundingBox.minZ, (float) boundingBox.minX, (float) boundingBox.maxX, (float) boundingBox.minY, (float) boundingBox.maxY, bufferBuilder, r, g, b, a);
        drawPlanePosZ(poseStack, (float) boundingBox.maxZ, (float) boundingBox.minX, (float) boundingBox.maxX, (float) boundingBox.minY, (float) boundingBox.maxY, bufferBuilder, r, g, b, a);
    }

    public static float[] prevColor = new float[4];

    public static void pushColor(final int rgb, final int alpha) {
        prevColor = RenderSystem.getShaderColor();
        final int r = (rgb >> 16) & 0xFF;
        final int g = (rgb >> 8) & 0xFF;
        final int b = rgb & 0xFF;

        final float af = alpha / 255f;
        final float rf = r / 255f;
        final float gf = g / 255f;
        final float bf = b / 255f;
        RenderSystem.setShaderColor(rf, gf, bf, af);
    }

    public static void popColor() {
        if (prevColor != null && prevColor.length == 4)
            RenderSystem.setShaderColor(prevColor[0], prevColor[1], prevColor[2], prevColor[3]);
    }
}
