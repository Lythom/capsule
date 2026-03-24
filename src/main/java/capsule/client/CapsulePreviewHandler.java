package capsule.client;

import capsule.CapsuleMod;
import capsule.blocks.BlockCapsuleMarker;
import capsule.blocks.BlockEntityCapture;
import capsule.client.render.CapsuleTemplateRenderer;
import capsule.helpers.NBTHelper;
import capsule.helpers.Spacial;
import capsule.items.CapsuleItem;
import capsule.structure.CapsuleTemplate;
import capsule.tags.CapsuleTags;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import joptsimple.internal.Strings;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static capsule.client.RendererUtils.*;
import static capsule.items.CapsuleItem.CapsuleState.ACTIVATED;
import static capsule.items.CapsuleItem.CapsuleState.BLUEPRINT;
import static capsule.items.CapsuleItem.CapsuleState.DEPLOYED;
import static capsule.items.CapsuleItem.CapsuleState.EMPTY;
import static capsule.items.CapsuleItem.CapsuleState.EMPTY_ACTIVATED;
import static capsule.items.CapsuleItem.CapsuleState.ONE_USE_ACTIVATED;
import static capsule.structure.CapsuleTemplate.recenterRotation;

@EventBusSubscriber(modid = CapsuleMod.MODID, value = Dist.CLIENT)
public class CapsulePreviewHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final Map<String, List<AABB>> currentPreview = new HashMap<>();
    public static final Map<String, CapsuleTemplate> currentFullPreview = new HashMap<>();
    public static final Map<String, CapsuleTemplateRenderer> cachedFullPreview = new HashMap<>();
    public static final double NS_TO_MS = 0.000001d;
    private static int lastSize = 0;
    private static int lastColor = 0;

    static double time = 0;
    public static SectionRenderDispatcher dispatcher;

    public CapsulePreviewHandler() {
    }

    /**
     * Render recall preview when deployed capsule in hand
     */
    @SubscribeEvent
    public static void onLevelRenderLast(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            Minecraft mc = Minecraft.getInstance();
            time = (time + 1) % 100000;
            if (mc.player != null) {
                dispatcher = event.getLevelRenderer().getSectionRenderDispatcher();
                tryPreviewRecall(mc.player.getMainHandItem(), event.getPoseStack());
                tryPreviewDeploy(mc.player, event.getPartialTick().getGameTimeDeltaPartialTick(true), mc.player.getMainHandItem(), event.getPoseStack());
                tryPreviewLinkedInventory(mc.player, mc.player.getMainHandItem(), event.getPoseStack());
            }
        }
    }

    /**
     * try to spot a templateDownload command for client
     */
    @SubscribeEvent
    public static void OnClientChatEvent(ClientChatReceivedEvent event) {
        if ("Getting template of currently held capsule…".equals(event.getMessage().getString())) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                ItemStack heldItem = mc.player.getMainHandItem();
                String structureName = CapsuleItem.getStructureName(heldItem);
                if (heldItem.getItem() instanceof CapsuleItem && structureName != null) {
                    CapsuleTemplate template = currentFullPreview.get(structureName);
                    Path path = new File("capsule_exports").toPath();
                    try {
                        Files.createDirectories(Files.exists(path) ? path.toRealPath() : path);
                    } catch (IOException var19) {
                        LOGGER.error("Failed to create parent directory: {}", path);
                    }
                    try {
                        CompoundTag compoundnbt = template.save(new CompoundTag());
                        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
                        Path filePath = path.resolve(df.format(new Date()) + "-" + structureName + ".nbt");
                        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(filePath.toFile()))) {
                            NbtIo.writeCompressed(compoundnbt, dos);
                        }
                        mc.player.sendSystemMessage(Component.literal("→ <minecraftInstance>/" + filePath.toString().replace("\\", "/")));
                    } catch (Throwable var21) {
                        LOGGER.error(var21);
                        mc.player.sendSystemMessage(Component.translatable("capsule.error.cantDownload"));
                    }
                } else {
                    mc.player.sendSystemMessage(Component.translatable("capsule.error.cantDownload"));
                }
            }
        }


    }

    /**
     * set captureBlock data (clientside only ) when capsule is in hand.
     */
    @SubscribeEvent
    public static void onLivingUpdateEvent(PlayerTickEvent.Pre event) {
        // do something to player every update tick:
        if (event.getEntity() instanceof LocalPlayer) {
            LocalPlayer player = (LocalPlayer) event.getEntity();
            tryPreviewCapture(player, player.getMainHandItem());
        }
    }

    private static boolean tryPreviewCapture(LocalPlayer player, ItemStack heldItem) {
        // an item is in hand
        if (!heldItem.isEmpty()) {
            Item heldItemItem = heldItem.getItem();
            // it's an empty capsule : show capture zones
            if (heldItemItem instanceof CapsuleItem && (CapsuleItem.hasState(heldItem, EMPTY) || CapsuleItem.hasState(heldItem, EMPTY_ACTIVATED))) {
                CompoundTag tag = NBTHelper.getTag(heldItem);
                if (tag != null && tag.contains("size")) {
                    setCaptureTESizeColor(tag.getInt("size"), CapsuleItem.getBaseColor(heldItem), player.getCommandSenderWorld());
                    return true;
                }

            } else {
                setCaptureTESizeColor(0, 0, player.getCommandSenderWorld());
            }
        } else {
            setCaptureTESizeColor(0, 0, player.getCommandSenderWorld());
        }

        return false;
    }


    @SuppressWarnings("ConstantConditions")
    private static void tryPreviewDeploy(LocalPlayer thePlayer, float partialTicks, ItemStack heldItemMainhand, PoseStack poseStack) {
        if (heldItemMainhand.getItem() instanceof CapsuleItem
                && NBTHelper.hasTag(heldItemMainhand)
                && isDeployable(heldItemMainhand)
        ) {
            CompoundTag tag = NBTHelper.getTag(heldItemMainhand);
            if (tag == null || Strings.isNullOrEmpty(tag.getString("structureName"))) return;

            int size = CapsuleItem.getSize(heldItemMainhand);
            BlockHitResult rtc = Spacial.clientRayTracePreview(thePlayer, partialTicks, size);
            if (rtc != null && rtc.getType() == HitResult.Type.BLOCK) {
                int extendSize = (size - 1) / 2;
                BlockPos destOriginPos = rtc.getBlockPos().offset(rtc.getDirection().getNormal()).offset(-extendSize, (int) (0.01 + CapsuleItem.getYOffset(heldItemMainhand)), -extendSize);
                String structureName = tag.getString("structureName");

                AABB errorBoundingBox = new AABB(
                        0,
                        +0.01,
                        0,
                        1.01,
                        1.01,
                        1.01);

                synchronized (CapsulePreviewHandler.currentPreview) {
                    synchronized (CapsulePreviewHandler.currentFullPreview) {
                        boolean haveFullPreview = CapsulePreviewHandler.currentFullPreview.containsKey(structureName);
                        if (haveFullPreview) {
                            if (CapsulePreviewHandler.cachedFullPreview.containsKey(structureName)) {
                                DisplayFullPreview(thePlayer, heldItemMainhand, poseStack, extendSize, destOriginPos, structureName);

                            }
                        }
                        if (CapsulePreviewHandler.currentPreview.containsKey(structureName) || size == 1) {
                            DisplayWireframePreview(thePlayer, heldItemMainhand, size, rtc, extendSize, destOriginPos, structureName, errorBoundingBox, haveFullPreview, poseStack);
                        }
                    }
                }
            }
        }

    }

    private static void DisplayFullPreview(LocalPlayer thePlayer, ItemStack heldItemMainhand, PoseStack poseStack, int extendSize, BlockPos destOriginPos, String structureName) {
        CapsuleTemplate template = CapsulePreviewHandler.currentFullPreview.get(structureName);
        CapsuleTemplateRenderer renderer = CapsulePreviewHandler.cachedFullPreview.get(structureName);
        StructurePlaceSettings placement = CapsuleItem.getPlacement(heldItemMainhand);
        renderer.changeTemplateIfDirty(
                template,
                thePlayer.getCommandSenderWorld(),
                destOriginPos,
                recenterRotation(extendSize, placement),
                placement,
                2
        );

        float glitchIntensity = (float) (Math.abs(Math.cos(time * 0.1f)) * Math.abs(Math.cos(time * 0.14f)) * Math.abs(Math.cos(time * 0.12f))) - 0.3f;
        glitchIntensity = (float) Math.min(0.05, Math.max(0, glitchIntensity));
        float glitchIntensity2 = ((float) (Math.cos(time * 0.12f) * Math.cos(time * 0.15f) * Math.cos(time * 0.14f))) * glitchIntensity;
        float glitchValue = (float) Math.min(0.12, Math.max(0, Math.tan(time * 0.5)));
        float glitchValuey = (float) Math.min(0.32, Math.max(0, Math.tan(time * 0.2)));
        float glitchValuez = (float) Math.min(0.12, Math.max(0, Math.tan(time * 0.8)));

        poseStack.pushPose();
        poseStack.translate(
                glitchIntensity2 * glitchValue,
                glitchIntensity * glitchValuey,
                glitchIntensity2 * glitchValuez);
        poseStack.scale(1 + glitchIntensity2 * glitchValuez, 1 + glitchIntensity * glitchValuey, 1);

        renderer.renderTemplate(poseStack, thePlayer, destOriginPos);

        poseStack.popPose();
    }

    private static void DisplayWireframePreview(LocalPlayer thePlayer, ItemStack heldItemMainhand, int size, BlockHitResult rtc, int extendSize, BlockPos destOriginPos, String structureName, AABB errorBoundingBox, boolean haveFullPreview, PoseStack poseStack) {
        List<AABB> blockspos = new ArrayList<>();
        if (size > 1) {
            blockspos = CapsulePreviewHandler.currentPreview.get(structureName);
        } else if (CapsuleItem.hasState(heldItemMainhand, EMPTY)) {
            // (1/2) hack this renderer for specific case : capture of a 1-sized empty capsule
            BlockPos pos = rtc.getBlockPos().subtract(destOriginPos);
            blockspos.add(new AABB(pos));
        }
        if (blockspos.isEmpty()) {
            BlockPos pos = new BlockPos(extendSize, 0, extendSize);
            blockspos.add(new AABB(pos));
        }

        doPositionPrologue(Minecraft.getInstance().getEntityRenderDispatcher().camera, poseStack);

        StructurePlaceSettings placement = CapsuleItem.getPlacement(heldItemMainhand);

        for (AABB bb : blockspos) {
            BlockPos recenter = recenterRotation(extendSize, placement);
            AABB dest = CapsuleTemplate.transformedAxisAlignedBB(placement, bb)
                    .move(destOriginPos.getX(), destOriginPos.getY() + 0.01, destOriginPos.getZ())
                    .move(recenter.getX(), recenter.getY(), recenter.getZ())
                    .expandTowards(1, 1, 1);

            int color = 0xDDDDDD;
            if (CapsuleItem.hasState(heldItemMainhand, EMPTY)) {
                // (2/2) hack this renderer for specific case : capture of a 1-sized empty capsule
                RenderSystem.lineWidth(haveFullPreview ? 2.0F : 5.0F);
                color = CapsuleItem.getBaseColor(heldItemMainhand);
            } else {
                RenderSystem.disableDepthTest();
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                for (double j = dest.minZ; j < dest.maxZ; ++j) {
                    for (double k = dest.minY; k < dest.maxY; ++k) {
                        for (double l = dest.minX; l < dest.maxX; ++l) {
                            BlockPos pos = BlockPos.containing(l, k, j);
                            if (!thePlayer.getCommandSenderWorld().getBlockState(pos).is(CapsuleTags.overridable)) {
                                MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
                                VertexConsumer buffer = bufferSource.getBuffer(RenderType.lines());
                                LevelRenderer.renderLineBox(poseStack, buffer, errorBoundingBox.move(pos), 0.8f, 0, 0, 1);
                                LevelRenderer.renderLineBox(poseStack, buffer, errorBoundingBox.inflate(0.005).move(pos), 0.8f, 0, 0, 1);
                                LevelRenderer.renderLineBox(poseStack, buffer, errorBoundingBox.deflate(0.005).move(pos), 0.8f, 0, 0, 1);
                                bufferSource.endBatch(RenderType.lines());
                            }
                        }
                    }
                }
                RenderSystem.enableDepthTest();
            }

            if (!haveFullPreview) {
                RenderSystem.lineWidth(1);
                RenderSystem.enableBlend();
                final int r = (color >> 16) & 0xFF;
                final int g = (color >> 8) & 0xFF;
                final int b = color & 0xFF;
                MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
                VertexConsumer buffer = bufferSource.getBuffer(RenderType.lines());
                LevelRenderer.renderLineBox(poseStack, buffer, dest, r / 255f, g / 255f, b / 255f, 1);
                bufferSource.endBatch(RenderType.lines());
            }
        }

//        doWireEpilogue();
        doPositionEpilogue(poseStack);
    }

    private static boolean isDeployable(ItemStack heldItemMainhand) {
        return CapsuleItem.hasState(heldItemMainhand, ACTIVATED)
                || CapsuleItem.hasState(heldItemMainhand, ONE_USE_ACTIVATED)
                || CapsuleItem.hasState(heldItemMainhand, BLUEPRINT)
                || CapsuleItem.getSize(heldItemMainhand) == 1 && !CapsuleItem.hasState(heldItemMainhand, DEPLOYED);
    }

    private static void tryPreviewRecall(ItemStack heldItem, PoseStack poseStack) {
        // an item is in hand
        if (heldItem != null) {
            Item heldItemItem = heldItem.getItem();
            // it's an empty capsule : show capture zones
            if (heldItemItem instanceof CapsuleItem
                    && (CapsuleItem.hasState(heldItem, DEPLOYED) || CapsuleItem.hasState(heldItem, BLUEPRINT))
                    && NBTHelper.hasTag(heldItem)) {
                CompoundTag tag = NBTHelper.getTag(heldItem);
                if (tag != null && tag.contains("spawnPosition")) {
                    previewRecall(heldItem, poseStack);
                }
            }
        }
    }

    private static void tryPreviewLinkedInventory(LocalPlayer player, ItemStack heldItem, PoseStack poseStack) {
        if (heldItem != null) {
            Item heldItemItem = heldItem.getItem();
            if (heldItemItem instanceof CapsuleItem
                    && CapsuleItem.isBlueprint(heldItem)
                    && CapsuleItem.hasSourceInventory(heldItem)) {
                BlockPos location = CapsuleItem.getSourceInventoryLocation(heldItem);
                ResourceKey<Level> dimension = CapsuleItem.getSourceInventoryDimension(heldItem);
                if (location != null
                        && dimension != null
                        && dimension.equals(player.getCommandSenderWorld().dimension())
                        && location.distToCenterSqr(player.getX(), player.getY(), player.getZ()) < 60 * 60) {
                    previewLinkedInventory(location, poseStack);
                }
            }
        }
    }

    private static void previewLinkedInventory(BlockPos location, PoseStack poseStack) {
        Camera cam = Minecraft.getInstance().getEntityRenderDispatcher().camera;
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.debugQuads());
        poseStack.pushPose();
        poseStack.translate(-cam.getPosition().x, -cam.getPosition().y, -cam.getPosition().z);
        drawCube(poseStack, location, 0.01f, buffer, 91, 156, 255, 80);
        bufferSource.endBatch();

        poseStack.popPose();
    }

    private static void previewRecall(ItemStack capsule, PoseStack poseStack) {
        CompoundTag tag = NBTHelper.getTag(capsule);
        if (tag == null) return;
        CompoundTag linkPos = tag.getCompound("spawnPosition");

        int size = CapsuleItem.getSize(capsule);
        int extendSize = (size - 1) / 2;
        int color = CapsuleItem.getBaseColor(capsule);

        Camera cam = Minecraft.getInstance().getEntityRenderDispatcher().camera;
        AABB boundingBox = Spacial.getBB(linkPos.getInt("x") + extendSize, linkPos.getInt("y") - 1, linkPos.getInt("z") + extendSize, size, extendSize);
        MultiBufferSource.BufferSource impl = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer ivertexbuilder = impl.getBuffer(RenderType.lines());
        poseStack.pushPose();
        poseStack.translate(-cam.getPosition().x, -cam.getPosition().y, -cam.getPosition().z);

        renderRecallBox(poseStack, color, boundingBox, ivertexbuilder, time);
        impl.endBatch();

        poseStack.popPose();
    }

    private static void setCaptureTESizeColor(int size, int color, Level worldIn) {
        if (size == lastSize && color == lastColor) return;

        // change MinecraftNBT of all existing BlockEntityCapture in the world to make them display the preview zone
        // remember it's client side only
        for (BlockEntityCapture te : new ArrayList<>(BlockEntityCapture.instances)) {
            if (te.getLevel() == worldIn) {
                CompoundTag teData = te.getPersistentData();
                if (teData.getInt("size") != size || teData.getInt("color") != color) {
                    te.getPersistentData().putInt("size", size);
                    te.getPersistentData().putInt("color", color);
                    if (te.getBlockState().hasProperty(BlockCapsuleMarker.TRIGGERED)) {
                        worldIn.setBlock(te.getBlockPos(), te.getBlockState().setValue(BlockCapsuleMarker.TRIGGERED, size <= 0), 2);
                    }
                }
            }
        }
        lastSize = size;
        lastColor = color;
    }

    public static void renderRecallBox(PoseStack poseStackIn, int color, AABB boundingBox, VertexConsumer ivertexbuilder, double time) {
        final float af = 200 / 255f;
        final float rf = ((color >> 16) & 0xFF) / 255f;
        final float gf = ((color >> 8) & 0xFF) / 255f;
        final float bf = (color & 0xFF) / 255f;
        LevelRenderer.renderLineBox(poseStackIn, ivertexbuilder, boundingBox.minX, boundingBox.minY, boundingBox.minZ, boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ, rf, gf, bf, af, rf, gf, bf);
        for (int i = 0; i < 5; i++) {
            double i1 = getMovingEffectPos(boundingBox, i, time, 0.0015f);
            double i2 = getMovingEffectPos(boundingBox, i, time, 0.0017f);
            double lowI = Math.min(i1, i2);
            double highI = Math.max(i1, i2);
            LevelRenderer.renderLineBox(poseStackIn, ivertexbuilder,
                    boundingBox.minX, lowI, boundingBox.minZ,
                    boundingBox.maxX, highI, boundingBox.maxZ,
                    rf, gf, bf, 0.01f + i * 0.05f, rf, gf, bf);
        }
    }

    private static double getMovingEffectPos(AABB boundingBox, int t, double incTime, float offset) {
        return boundingBox.minY + (Math.cos(t * incTime * offset) * 0.5 + 0.5) * (boundingBox.maxY - boundingBox.minY);
    }
}
