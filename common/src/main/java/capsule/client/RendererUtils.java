package capsule.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;

public class RendererUtils {
    /**
     * The outline of box, in the space of poseStack, with the vanilla lines render types (depth tested, view offset z
     * layering) as 1.21.1's LevelRenderer#renderLineBox with RenderType.lines() had.
     * <p>
     * Since 26.3 an outline with alpha below 255 goes to the translucent phase, which Improved Transparency draws in
     * order independent transparency stages: only a render type with OIT pipelines can be drawn there (the client
     * crashes otherwise), and RenderTypes.lines() has none. Translucent boxes take
     * RenderTypes.linesTranslucentNoDepthWrite(), which has them; opaque boxes keep RenderTypes.lines().
     *
     * @param argb      color with alpha
     * @param lineWidth 1.21.1's RenderType.lines() used the window-scaled width: Window#getAppropriateLineWidth
     */
    public static void submitLineBox(OrderedSubmitNodeCollector collector, PoseStack poseStack, AABB box, int argb, float lineWidth) {
        RenderType type = ARGB.alpha(argb) == 255 ? RenderTypes.lines() : RenderTypes.linesTranslucentNoDepthWrite();
        collector.submitShapeOutline(poseStack, Shapes.create(box), type, argb, lineWidth, false);
    }

    public static void drawCube(PoseStack.Pose pose, final float minX, final float minY, final float minZ,
                                final float maxX, final float maxY, final float maxZ,
                                final VertexConsumer buffer, int r, int g, int b, int a) {
        drawPlaneNegX(pose, minX, minY, maxY, minZ, maxZ, buffer, r, g, b, a);
        drawPlanePosX(pose, maxX, minY, maxY, minZ, maxZ, buffer, r, g, b, a);
        drawPlaneNegY(pose, minY, minX, maxX, minZ, maxZ, buffer, r, g, b, a);
        drawPlanePosY(pose, maxY, minX, maxX, minZ, maxZ, buffer, r, g, b, a);
        drawPlaneNegZ(pose, minZ, minX, maxX, minY, maxY, buffer, r, g, b, a);
        drawPlanePosZ(pose, maxZ, minX, maxX, minY, maxY, buffer, r, g, b, a);
    }

    /**
     * Draws a quad with four explicit vertices and a normal vector.
     * All six drawPlane* methods delegate to this single implementation.
     */
    private static void drawQuad(PoseStack.Pose pose,
                                 float x0, float y0, float z0,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float x3, float y3, float z3,
                                 float nx, float ny, float nz,
                                 final VertexConsumer buffer, int r, int g, int b, int a) {
        buffer.addVertex(pose, x0, y0, z0).setColor(r, g, b, a).setNormal(pose, nx, ny, nz);
        buffer.addVertex(pose, x1, y1, z1).setColor(r, g, b, a).setNormal(pose, nx, ny, nz);
        buffer.addVertex(pose, x2, y2, z2).setColor(r, g, b, a).setNormal(pose, nx, ny, nz);
        buffer.addVertex(pose, x3, y3, z3).setColor(r, g, b, a).setNormal(pose, nx, ny, nz);
    }

    public static void drawPlaneNegX(PoseStack.Pose pose, final float x, final float minY, final float maxY,
                                     final float minZ, final float maxZ, final VertexConsumer buffer,
                                     int r, int g, int b, int a) {
        drawQuad(pose, x, minY, minZ, x, minY, maxZ, x, maxY, maxZ, x, maxY, minZ,
                -1, 0, 0, buffer, r, g, b, a);
    }

    public static void drawPlanePosX(PoseStack.Pose pose, final float x, final float minY, final float maxY,
                                     final float minZ, final float maxZ, final VertexConsumer buffer,
                                     int r, int g, int b, int a) {
        drawQuad(pose, x, minY, minZ, x, maxY, minZ, x, maxY, maxZ, x, minY, maxZ,
                1, 0, 0, buffer, r, g, b, a);
    }

    public static void drawPlaneNegY(PoseStack.Pose pose, final float y, final float minX, final float maxX,
                                     final float minZ, final float maxZ, final VertexConsumer buffer,
                                     int r, int g, int b, int a) {
        drawQuad(pose, minX, y, minZ, maxX, y, minZ, maxX, y, maxZ, minX, y, maxZ,
                0, -1, 0, buffer, r, g, b, a);
    }

    public static void drawPlanePosY(PoseStack.Pose pose, final float y, final float minX, final float maxX,
                                     final float minZ, final float maxZ, final VertexConsumer buffer,
                                     int r, int g, int b, int a) {
        drawQuad(pose, minX, y, minZ, minX, y, maxZ, maxX, y, maxZ, maxX, y, minZ,
                0, 1, 0, buffer, r, g, b, a);
    }

    public static void drawPlaneNegZ(PoseStack.Pose pose, final float z, final float minX, final float maxX,
                                     final float minY, final float maxY, final VertexConsumer buffer,
                                     int r, int g, int b, int a) {
        drawQuad(pose, minX, minY, z, minX, maxY, z, maxX, maxY, z, maxX, minY, z,
                0, 0, -1, buffer, r, g, b, a);
    }

    public static void drawPlanePosZ(PoseStack.Pose pose, final float z, final float minX, final float maxX,
                                     final float minY, final float maxY, final VertexConsumer buffer,
                                     int r, int g, int b, int a) {
        drawQuad(pose, minX, minY, z, maxX, minY, z, maxX, maxY, z, minX, maxY, z,
                0, 0, 1, buffer, r, g, b, a);
    }

    public static void drawCapsuleCube(PoseStack.Pose pose, AABB boundingBox, VertexConsumer bufferBuilder, int r, int g, int b, int a) {
        drawPlaneNegX(pose, (float) boundingBox.minX, (float) boundingBox.minY, (float) boundingBox.maxY, (float) boundingBox.minZ, (float) boundingBox.maxZ, bufferBuilder, r, g, b, a);
        drawPlanePosX(pose, (float) boundingBox.maxX, (float) boundingBox.minY, (float) boundingBox.maxY, (float) boundingBox.minZ, (float) boundingBox.maxZ, bufferBuilder, r, g, b, a);
        drawPlaneNegY(pose, (float) boundingBox.minY, (float) boundingBox.minX, (float) boundingBox.maxX, (float) boundingBox.minZ, (float) boundingBox.maxZ, bufferBuilder, r, g, b, a);
        drawPlanePosY(pose, (float) boundingBox.maxY, (float) boundingBox.minX, (float) boundingBox.maxX, (float) boundingBox.minZ, (float) boundingBox.maxZ, bufferBuilder, r, g, b, a);
        drawPlaneNegZ(pose, (float) boundingBox.minZ, (float) boundingBox.minX, (float) boundingBox.maxX, (float) boundingBox.minY, (float) boundingBox.maxY, bufferBuilder, r, g, b, a);
        drawPlanePosZ(pose, (float) boundingBox.maxZ, (float) boundingBox.minX, (float) boundingBox.maxX, (float) boundingBox.minY, (float) boundingBox.maxY, bufferBuilder, r, g, b, a);
    }

}
