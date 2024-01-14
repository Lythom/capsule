package capsule.client.render;

import capsule.client.render.vbo.MultiVBORenderer;
import capsule.structure.CapsuleTemplate;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.client.model.data.ModelData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CapsuleTemplateRenderer {
    private static final Logger LOGGER = LogManager.getLogger();
    public MultiVBORenderer renderBuffer;
    public FakeWorld templateWorld = null;
    private boolean isWorldDirty = true;
    private StructurePlaceSettings lastPlacementSettings;
    private ModelBlockRenderer blockRenderer = Minecraft.getInstance().getBlockRenderer().getModelRenderer();
    private LiquidBlockRenderer liquidBlockRenderer = new LiquidBlockRenderer();
    private final RandomSource random = RandomSource.create();

    public void renderTemplate(PoseStack poseStack, Player player, BlockPos destPos) {
        if (player == null)
            return;

        final Minecraft minecraft = Minecraft.getInstance();

        final Vec3 cameraView = minecraft.gameRenderer.getMainCamera().getPosition();
        poseStack.pushPose();
        poseStack.translate(destPos.getX() - cameraView.x, destPos.getY() - cameraView.y, destPos.getZ() - cameraView.z);

        renderTemplate(poseStack, cameraView, player);

        poseStack.popPose();
    }

//    public void renderTemplate(PoseStack poseStack, Vec3 cameraView, Player player) {
//        if (renderBuffer != null) {
//            renderBuffer.render(poseStack.last().pose()); //Actually draw whats in the buffer
//            return;
//        }
//
//        Minecraft minecraft = Minecraft.getInstance();
//        renderBuffer = MultiVBORenderer.of((buffer) -> {
//            VertexConsumer builder = buffer.getBuffer(CustomRenderType.VISUAL_BLOCK);
//            MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
//            var dispatcher = minecraft.getBlockRenderer();
//
//            PoseStack stack = new PoseStack(); //Create a new matrix stack for use in the buffer building process
//            stack.pushPose(); //Save position
//
//            for (Map.Entry<BlockPos, BlockState> entry : templateWorld.entrySet()) {
//                BlockPos targetPos = entry.getKey();
//                BlockState state = entry.getValue();
//
//                stack.pushPose(); //Save position again
//                stack.translate(targetPos.getX(), targetPos.getY(), targetPos.getZ());
//
//                BakedModel ibakedmodel = dispatcher.getBlockModel(state);
//                try {
//                    if (state.getRenderShape() == RenderShape.MODEL || state.getRenderShape() == RenderShape.ENTITYBLOCK_ANIMATED) {
//                        blockRenderer.tesselateWithAO(templateWorld, ibakedmodel, state, targetPos, poseStack, builder, true, new Random(Mth.getSeed(targetPos)), Mth.getSeed(targetPos), OverlayTexture.NO_OVERLAY, net.minecraftforge.client.model.data.EmptyModelData.INSTANCE);
//                    } else {
//                        FluidState ifluidstate = state.getFluidState();
//                        if (!ifluidstate.isEmpty()) {
//                            liquidBlockRenderer.tesselate(templateWorld, targetPos, builder, state, ifluidstate);
//                        }
//                    }
//                } catch (Exception e) {
//                    LOGGER.trace("Caught exception whilst rendering {}.", state, e);
//                }
//                stack.popPose(); // Load the position we saved earlier
//            }
//            stack.popPose(); //Load after loop
//        });
//
//        // renderBuffer.sort((float) (-destPos.getX() + cameraView.x()), (float) (-destPos.getY() + cameraView.y()), (float) (-destPos.getZ() + cameraView.z()));
//        renderBuffer.render(poseStack.last().pose()); //Actually draw whats in the buffer
//    }

    public void renderTemplate(PoseStack poseStack, Vec3 cameraView, Player player) {
        Minecraft minecraft = Minecraft.getInstance();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        VertexConsumer bufferSolid = bufferSource.getBuffer(CustomRenderType.VISUAL_BLOCK);
//        VertexConsumer bufferSolid = bufferSource.getBuffer(RenderType.cutoutMipped());

        // PoseStack stack = new PoseStack(); //Create a new matrix stack for use in the bufferSolid building process
        //stack.pushPose(); //Save position

        for (Map.Entry<BlockPos, BlockState> entry : templateWorld.entrySet()) {
            BlockPos targetPos = entry.getKey();
            BlockState state = entry.getValue();
            BakedModel ibakedmodel = minecraft.getBlockRenderer().getBlockModel(state);

            poseStack.pushPose(); //Save position again
            poseStack.translate(targetPos.getX(), targetPos.getY(), targetPos.getZ()); // handled by renderBatched
            try {
                if (state.getRenderShape() == RenderShape.MODEL || state.getRenderShape() == RenderShape.ENTITYBLOCK_ANIMATED) {
                    random.setSeed(Mth.getSeed(targetPos));
                    blockRenderer.tesselateWithAO(templateWorld, ibakedmodel, state, targetPos, poseStack, bufferSolid, true, random, Mth.getSeed(targetPos), OverlayTexture.NO_OVERLAY, ModelData.EMPTY, RenderType.LINES);
                } else {
                    FluidState ifluidstate = state.getFluidState();
                    if (!ifluidstate.isEmpty()) {
//                        liquidBlockRenderer.tesselate(templateWorld, targetPos, bufferSolid, state, ifluidstate);
                        renderFluid(poseStack, targetPos, templateWorld, bufferSolid, ifluidstate);
                    }
                }
            } catch (Exception e) {
                LOGGER.trace("Caught exception whilst rendering {}.", state, e);
            }
            poseStack.popPose(); // Load the position we saved earlier

        }
        //stack.popPose(); //Load after loop
    }

    private static void renderFluid(PoseStack matrixStack, BlockPos destOriginPos, BlockAndTintGetter world, VertexConsumer buffer, FluidState ifluidstate) {
        IClientFluidTypeExtensions fluidTypeExtensions = IClientFluidTypeExtensions.of(ifluidstate);
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(fluidTypeExtensions.getStillTexture());
        float minU = sprite.getU0();
        float maxU = Math.min(minU + (sprite.getU1() - minU) * 1, sprite.getU1());
        float minV = sprite.getV0();
        float maxV = Math.min(minV + (sprite.getV1() - minV) * 1, sprite.getV1());
        int waterColor =  fluidTypeExtensions.getTintColor(ifluidstate, world, destOriginPos);
        float red = (float) (waterColor >> 16 & 255) / 255.0F;
        float green = (float) (waterColor >> 8 & 255) / 255.0F;
        float blue = (float) (waterColor & 255) / 255.0F;

        Matrix4f matrix = matrixStack.last().pose();

        vertex(buffer, maxU, minV, red, green, blue, matrix, 0, 1, 0);
        vertex(buffer, minU, minV, red, green, blue, matrix, 0, 1, 1);
        vertex(buffer, minU, maxV, red, green, blue, matrix, 1, 1, 1);
        vertex(buffer, maxU, maxV, red, green, blue, matrix, 1, 1, 0);
    }

    private static void vertex(VertexConsumer buffer, float maxU, float minV, float red, float green,
                               float blue, Matrix4f matrix, int x, int y, int z) {
        buffer.vertex(matrix, x, y, z).color(red, green, blue, 1.0F).uv(maxU, minV).uv2(256).normal(0.0F, 1.0F, 0.0F).endVertex();
    }

    public static void renderModelBrightnessColorQuads(PoseStack.Pose matrixEntry, VertexConsumer builder,
                                                       float red, float green, float blue, float alpha, List<BakedQuad> listQuads, int combinedLightsIn,
                                                       int combinedOverlayIn) {
        for (BakedQuad bakedquad : listQuads) {
            float f;
            float f1;
            float f2;

            if (bakedquad.isTinted()) {
                f = red;
                f1 = green;
                f2 = blue;
            } else {
                f = 1f;
                f1 = 1f;
                f2 = 1f;
            }

            builder.putBulkData(matrixEntry, bakedquad, f, f1, f2, alpha, combinedLightsIn, combinedOverlayIn, false);
        }
    }

    public boolean changeTemplateIfDirty(CapsuleTemplate template, Level world, BlockPos destPos, BlockPos
            offPos, StructurePlaceSettings placementSettings, int placeFlag) {
        if (lastPlacementSettings == null ||
                (placementSettings != null && (
                        placementSettings.getRotation() != lastPlacementSettings.getRotation()
                                || placementSettings.getMirror() != lastPlacementSettings.getMirror()))
        ) {
            isWorldDirty = true;
        }
        if (!isWorldDirty) return true;
        if (renderBuffer != null) renderBuffer.close();
        renderBuffer = null;
        lastPlacementSettings = placementSettings;
        if (template.palettes.isEmpty()) {
            return false;
        } else {
            templateWorld = new FakeWorld(world);

            List<StructureTemplate.StructureBlockInfo> list = CapsuleTemplate.Palette.getRandomPalette(placementSettings, template.palettes, destPos).blocks();
            if (!list.isEmpty() && template.size.getX() >= 1 && template.size.getY() >= 1 && template.size.getZ() >= 1) {
                BoundingBox mutableboundingbox = placementSettings.getBoundingBox();
                List<BlockPos> list1 = Lists.newArrayListWithCapacity(placementSettings.shouldKeepLiquids() ? list.size() : 0);
                List<Pair<BlockPos, CompoundTag>> list2 = Lists.newArrayListWithCapacity(list.size());
                int i = Integer.MAX_VALUE;
                int j = Integer.MAX_VALUE;
                int k = Integer.MAX_VALUE;
                int l = Integer.MIN_VALUE;
                int i1 = Integer.MIN_VALUE;
                int j1 = Integer.MIN_VALUE;

                for (StructureTemplate.StructureBlockInfo template$blockinfo : CapsuleTemplate.processBlockInfos(template, templateWorld, offPos, placementSettings, list)) {
                    BlockPos blockpos = template$blockinfo.pos();
                    if (mutableboundingbox == null || mutableboundingbox.isInside(blockpos)) {
                        FluidState fluidstate = placementSettings.shouldKeepLiquids() ? templateWorld.getFluidState(blockpos) : null;
                        BlockState blockstate = template$blockinfo.state().mirror(placementSettings.getMirror()).rotate(templateWorld, blockpos, placementSettings.getRotation());
                        if (template$blockinfo.nbt() != null) {
                            BlockEntity BlockEntity = templateWorld.getBlockEntity(blockpos);
                            Clearable.tryClear(BlockEntity);
                            templateWorld.setBlock(blockpos, Blocks.BARRIER.defaultBlockState(), 20);
                        }

                        if (templateWorld.setBlock(blockpos, blockstate, placeFlag)) {
                            i = Math.min(i, blockpos.getX());
                            j = Math.min(j, blockpos.getY());
                            k = Math.min(k, blockpos.getZ());
                            l = Math.max(l, blockpos.getX());
                            i1 = Math.max(i1, blockpos.getY());
                            j1 = Math.max(j1, blockpos.getZ());
                            list2.add(Pair.of(blockpos, template$blockinfo.nbt()));

                            if (fluidstate != null && blockstate.getBlock() instanceof LiquidBlockContainer) {
                                ((LiquidBlockContainer) blockstate.getBlock()).placeLiquid(templateWorld, blockpos, blockstate, fluidstate);
                                if (!fluidstate.isSource()) {
                                    list1.add(blockpos);
                                }
                            }
                        }
                    }
                }


                boolean flag = true;
                Direction[] adirection = new Direction[]{Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

                while (flag && !list1.isEmpty()) {
                    flag = false;
                    Iterator<BlockPos> iterator = list1.iterator();

                    while (iterator.hasNext()) {
                        BlockPos blockpos2 = iterator.next();
                        BlockPos blockpos3 = blockpos2;
                        FluidState fluidstate2 = templateWorld.getFluidState(blockpos2);

                        for (int k1 = 0; k1 < adirection.length && !fluidstate2.isSource(); ++k1) {
                            BlockPos blockpos1 = blockpos3.relative(adirection[k1]);
                            FluidState fluidstate1 = templateWorld.getFluidState(blockpos1);
                            if (fluidstate1.getHeight(templateWorld, blockpos1) > fluidstate2.getHeight(templateWorld, blockpos3) || fluidstate1.isSource() && !fluidstate2.isSource()) {
                                fluidstate2 = fluidstate1;
                                blockpos3 = blockpos1;
                            }
                        }

                        if (fluidstate2.isSource()) {
                            BlockState blockstate2 = templateWorld.getBlockState(blockpos2);
                            Block block = blockstate2.getBlock();
                            if (block instanceof LiquidBlockContainer) {
                                ((LiquidBlockContainer) block).placeLiquid(templateWorld, blockpos2, blockstate2, fluidstate2);
                                flag = true;
                                iterator.remove();
                            }
                        }
                    }
                }

                if (i <= l) {
                    if (!placementSettings.getKnownShape()) {
                        DiscreteVoxelShape voxelshapepart = new BitSetDiscreteVoxelShape(l - i + 1, i1 - j + 1, j1 - k + 1);
                        int l1 = i;
                        int i2 = j;
                        int j2 = k;

                        for (Pair<BlockPos, CompoundTag> pair1 : list2) {
                            BlockPos blockpos5 = pair1.getFirst();
                            voxelshapepart.fill(blockpos5.getX() - l1, blockpos5.getY() - i2, blockpos5.getZ() - j2);
                        }

                        StructureTemplate.updateShapeAtEdge(templateWorld, placeFlag, voxelshapepart, l1, i2, j2);
                    }

                    for (Pair<BlockPos, CompoundTag> pair : list2) {
                        BlockPos blockpos4 = pair.getFirst();
                        if (!placementSettings.getKnownShape()) {
                            BlockState blockstate1 = templateWorld.getBlockState(blockpos4);
                            BlockState blockstate3 = Block.updateFromNeighbourShapes(blockstate1, templateWorld, blockpos4);
                            if (blockstate1 != blockstate3) {
                                templateWorld.setBlock(blockpos4, blockstate3, placeFlag & -2 | 16);
                            }

                            templateWorld.blockUpdated(blockpos4, blockstate3.getBlock());
                        }
                    }
                }
                isWorldDirty = false;
                return true;
            } else {
                isWorldDirty = false;
                return false;
            }
        }
    }

    public void setWorldDirty() {
        isWorldDirty = true;
    }
}
