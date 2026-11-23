package capsule.client.render;

import capsule.platform.Services;
import capsule.structure.CapsuleTemplate;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
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
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CapsuleTemplateRenderer {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final float PREVIEW_ALPHA = 0.5f;
    public FakeWorld templateWorld = null;
    private boolean isWorldDirty = true;
    private StructurePlaceSettings lastPlacementSettings;
    // ambient occlusion and face culling, as 1.21.1's tesselateWithAO with checkSides
    private final ModelBlockRenderer blockRenderer = new ModelBlockRenderer(true, true, Minecraft.getInstance().getBlockColors());
    private final PoseStack.Pose quadPose = new PoseStack().last();

    /**
     * Submits the blocks of the template world as translucent ghost blocks, in the space of poseStack, in two passes
     * (see CustomRenderType): the depth pass with the vertex colors as they are, then the color pass with their alpha
     * scaled by alpha. Both passes tessellate the template when the frame draws them.
     */
    public void submitTemplate(SubmitNodeCollector collector, PoseStack poseStack, float alpha) {
        FakeWorld world = templateWorld;
        if (world == null) return;
        collector.submitCustomGeometry(poseStack, CustomRenderType.GHOST_DEPTH, (pose, consumer) -> tesselate(world, pose, consumer));
        collector.submitCustomGeometry(poseStack, CustomRenderType.GHOST,
                (pose, consumer) -> tesselate(world, pose, new TranslucentVertexConsumer(consumer, alpha)));
    }

    private void tesselate(FakeWorld world, PoseStack.Pose pose, VertexConsumer consumer) {
        ModelManager models = Minecraft.getInstance().getModelManager();
        // block quads come with their offset in the template; the pose keeps the camera translation and the glitch
        BlockQuadOutput output = (x, y, z, quad, instance) -> {
            quadPose.set(pose);
            quadPose.translate(x, y, z);
            consumer.putBakedQuad(quadPose, quad, instance);
        };
        for (Map.Entry<BlockPos, BlockState> entry : world.entrySet()) {
            BlockPos targetPos = entry.getKey();
            BlockState state = entry.getValue();
            try {
                if (state.getRenderShape() == RenderShape.MODEL) {
                    BlockStateModel model = models.getBlockStateModelSet().get(state);
                    blockRenderer.tesselateBlock(output, targetPos.getX(), targetPos.getY(), targetPos.getZ(),
                            world, targetPos, state, model, Mth.getSeed(targetPos));
                } else {
                    FluidState fluidState = state.getFluidState();
                    if (!fluidState.isEmpty()) {
                        renderFluid(pose, targetPos, world, consumer, fluidState, models.getFluidStateModelSet().get(fluidState));
                    }
                }
            } catch (Exception e) {
                LOGGER.trace("Caught exception whilst rendering {}.", state, e);
            }
        }
    }

    /**
     * The top face of the block, with the still texture and the tint of the fluid model (the same on both loaders since
     * 26.1: NeoForge and Fabric register their fluids' models in the vanilla FluidStateModelSet).
     */
    @SuppressWarnings("deprecation") // NeoForge deprecates tintSource() for its FluidTintSource, which is a BlockTintSource
    private static void renderFluid(PoseStack.Pose pose, BlockPos pos, BlockAndTintGetter world, VertexConsumer buffer, FluidState fluidState, FluidModel fluidModel) {
        TextureAtlasSprite sprite = fluidModel.stillMaterial().sprite();
        float minU = sprite.getU0();
        float maxU = Math.min(minU + (sprite.getU1() - minU) * 1, sprite.getU1());
        float minV = sprite.getV0();
        float maxV = Math.min(minV + (sprite.getV1() - minV) * 1, sprite.getV1());
        BlockTintSource tintSource = fluidModel.tintSource();
        int waterColor = tintSource == null ? -1 : tintSource.colorInWorld(fluidState.createLegacyBlock(), world, pos);
        float red = (float) (waterColor >> 16 & 255) / 255.0F;
        float green = (float) (waterColor >> 8 & 255) / 255.0F;
        float blue = (float) (waterColor & 255) / 255.0F;

        Matrix4f matrix = pose.pose();
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        vertex(buffer, maxU, minV, red, green, blue, matrix, x, y + 1, z);
        vertex(buffer, minU, minV, red, green, blue, matrix, x, y + 1, z + 1);
        vertex(buffer, minU, maxV, red, green, blue, matrix, x + 1, y + 1, z + 1);
        vertex(buffer, maxU, maxV, red, green, blue, matrix, x + 1, y + 1, z);
    }

    private static void vertex(VertexConsumer buffer, float maxU, float minV, float red, float green,
                               float blue, Matrix4f matrix, int x, int y, int z) {
        buffer.addVertex(matrix, x, y, z).setColor(red, green, blue, 1.0F).setUv(maxU, minV).setLight(256).setNormal(0.0F, 1.0F, 0.0F);
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
        lastPlacementSettings = placementSettings;
        if (template.palettes.isEmpty()) {
            return false;
        } else {
            templateWorld = new FakeWorld(world);

            List<StructureTemplate.StructureBlockInfo> list = CapsuleTemplate.Palette.getRandomPalette(placementSettings, template.palettes, destPos).blocks();
            if (!list.isEmpty() && template.size.getX() >= 1 && template.size.getY() >= 1 && template.size.getZ() >= 1) {
                BoundingBox mutableboundingbox = placementSettings.getBoundingBox();
                List<BlockPos> list1 = Lists.newArrayListWithCapacity(0);
                List<Pair<BlockPos, CompoundTag>> list2 = Lists.newArrayListWithCapacity(list.size());
                int i = Integer.MAX_VALUE;
                int j = Integer.MAX_VALUE;
                int k = Integer.MAX_VALUE;
                int l = Integer.MIN_VALUE;
                int i1 = Integer.MIN_VALUE;
                int j1 = Integer.MIN_VALUE;

                for (StructureTemplate.StructureBlockInfo template$blockinfo : CapsuleTemplate.processBlockInfos(template, templateWorld, offPos, placementSettings, list)) {
                    BlockPos blockpos = template$blockinfo.pos();
                    if (mutableboundingbox != null && !mutableboundingbox.isInside(blockpos)) continue;
                    try {
                        FluidState fluidstate = null;
                        BlockState blockstate = Services.PLATFORM.rotate(template$blockinfo.state().mirror(placementSettings.getMirror()), templateWorld, blockpos, placementSettings.getRotation());
                        if (template$blockinfo.nbt() != null) {
                            BlockEntity BlockEntity = templateWorld.getBlockEntity(blockpos);
                            if (BlockEntity instanceof Clearable clearable) clearable.clearContent();
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
                    } catch (RuntimeException e) {
                        logSkippedBlock(template$blockinfo.state(), e);
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

                        try {
                            StructureTemplate.updateShapeAtEdge(templateWorld, placeFlag, voxelshapepart, l1, i2, j2);
                        } catch (RuntimeException e) {
                            LOGGER.debug("Preview shapes at the edge of the template not updated", e);
                        }
                    }

                    for (Pair<BlockPos, CompoundTag> pair : list2) {
                        BlockPos blockpos4 = pair.getFirst();
                        if (!placementSettings.getKnownShape()) {
                            BlockState blockstate1 = templateWorld.getBlockState(blockpos4);
                            try {
                                BlockState blockstate3 = Block.updateFromNeighbourShapes(blockstate1, templateWorld, blockpos4);
                                if (blockstate1 != blockstate3) {
                                    templateWorld.setBlock(blockpos4, blockstate3, placeFlag & -2 | 16);
                                }

                                templateWorld.updateNeighborsAt(blockpos4, blockstate3.getBlock());
                            } catch (RuntimeException e) {
                                logSkippedBlock(blockstate1, e);
                            }
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

    /**
     * Modded blocks may expect a Level where the preview only offers a LevelAccessor (FakeWorld).
     */
    private static void logSkippedBlock(BlockState state, RuntimeException e) {
        LOGGER.debug("Preview of {} is incomplete: {}", state, e.toString());
    }

    public void setWorldDirty() {
        isWorldDirty = true;
    }
}
