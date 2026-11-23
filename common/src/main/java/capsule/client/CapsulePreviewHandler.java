package capsule.client;

import capsule.blocks.BlockCapsuleMarker;
import capsule.blocks.BlockEntityCapture;
import capsule.client.render.CapsuleTemplateRenderer;
import capsule.helpers.NBTHelper;
import capsule.helpers.Spacial;
import capsule.items.CapsuleItem;
import capsule.structure.CapsuleTemplate;
import capsule.tags.CapsuleTags;
import com.mojang.blaze3d.vertex.PoseStack;
import joptsimple.internal.Strings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
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

public class CapsulePreviewHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final Map<String, List<AABB>> currentPreview = new HashMap<>();
    public static final Map<String, CapsuleTemplate> currentFullPreview = new HashMap<>();
    public static final Map<String, CapsuleTemplateRenderer> cachedFullPreview = new HashMap<>();
    public static final double NS_TO_MS = 0.000001d;
    private static int lastSize = 0;
    private static int lastColor = 0;
    private static ItemStack lastMainHandItem = ItemStack.EMPTY;

    static double time = 0;
    /**
     * The red of the blocks in the way of a deploy (1.21.1: 0.8, 0, 0, 1).
     */
    private static final int ERROR_COLOR = ARGB.colorFromFloat(1, 0.8f, 0, 0);

    public CapsulePreviewHandler() {
    }

    /**
     * Something to draw this frame, in coordinates relative to the camera (the pose stack of the submit hooks has its
     * origin at the camera). The pose stack is pushed and popped around each submit.
     */
    @FunctionalInterface
    public interface PreviewSubmit {
        void submit(SubmitNodeCollector collector, PoseStack poseStack);
    }

    /**
     * What the previews draw this frame. Gathered during the level extraction, then submitted in the same frame: the
     * loaders call extractFrame from their end of level extraction hook and submitFrame from their custom geometry
     * submit hook, both on the render thread.
     */
    private static final List<PreviewSubmit> frame = new ArrayList<>();

    /**
     * Gathers the recall, deploy and linked inventory previews of the capsule in hand, and the capture animations.
     */
    public static void extractFrame(Vec3 camera, float partialTick) {
        frame.clear();
        Minecraft mc = Minecraft.getInstance();
        time = (time + 1) % 100000;
        if (mc.player != null) {
            float lineWidth = mc.getWindow().getAppropriateLineWidth();
            tryPreviewRecall(mc.player.getMainHandItem(), camera, lineWidth);
            tryPreviewDeploy(mc.player, partialTick, mc.player.getMainHandItem(), camera, lineWidth);
            tryPreviewLinkedInventory(mc.player, mc.player.getMainHandItem(), camera);
            CaptureAnimation.extract(frame, camera, partialTick, lineWidth);
        }
    }

    public static void submitFrame(SubmitNodeCollector collector, PoseStack poseStack) {
        for (PreviewSubmit submit : frame) {
            poseStack.pushPose();
            submit.submit(collector, poseStack);
            poseStack.popPose();
        }
    }

    /**
     * try to spot a templateDownload command for client
     */
    public static void onChatMessage(Component message) {
        if ("Getting template of currently held capsule…".equals(message.getString())) {
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
    public static void onLocalPlayerTick(LocalPlayer player) {
        CaptureAnimation.tick();
        ItemStack mainHandItem = player.getMainHandItem();
        tryPreviewCapture(player, mainHandItem);
        if (!ItemStack.matches(lastMainHandItem, mainHandItem)) {
            lastMainHandItem = mainHandItem.copy();
            CapsuleItem.onMainHandChanged(mainHandItem);
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
                    setCaptureTESizeColor(tag.getIntOr("size", 0), CapsuleItem.getBaseColor(heldItem), player.level());
                    return true;
                }

            } else {
                setCaptureTESizeColor(0, 0, player.level());
            }
        } else {
            setCaptureTESizeColor(0, 0, player.level());
        }

        return false;
    }


    @SuppressWarnings("ConstantConditions")
    private static void tryPreviewDeploy(LocalPlayer thePlayer, float partialTicks, ItemStack heldItemMainhand, Vec3 camera, float lineWidth) {
        if (heldItemMainhand.getItem() instanceof CapsuleItem
                && NBTHelper.hasTag(heldItemMainhand)
                && isDeployable(heldItemMainhand)
        ) {
            CompoundTag tag = NBTHelper.getTag(heldItemMainhand);
            if (tag == null || Strings.isNullOrEmpty(tag.getStringOr("structureName", ""))) return;

            int size = CapsuleItem.getSize(heldItemMainhand);
            BlockHitResult rtc = Spacial.clientRayTracePreview(thePlayer, partialTicks, size);
            if (rtc != null && rtc.getType() == HitResult.Type.BLOCK) {
                int extendSize = (size - 1) / 2;
                BlockPos destOriginPos = Spacial.getDeployPosition(thePlayer.level(), rtc).offset(-extendSize, (int) (0.01 + CapsuleItem.getYOffset(heldItemMainhand)), -extendSize);
                String structureName = tag.getStringOr("structureName", "");

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
                                DisplayFullPreview(thePlayer, heldItemMainhand, camera, extendSize, destOriginPos, structureName);

                            }
                        }
                        if (CapsulePreviewHandler.currentPreview.containsKey(structureName) || size == 1) {
                            DisplayWireframePreview(thePlayer, heldItemMainhand, size, rtc, extendSize, destOriginPos, structureName, errorBoundingBox, haveFullPreview, camera, lineWidth);
                        }
                    }
                }
            }
        }

    }

    private static void DisplayFullPreview(LocalPlayer thePlayer, ItemStack heldItemMainhand, Vec3 camera, int extendSize, BlockPos destOriginPos, String structureName) {
        CapsuleTemplate template = CapsulePreviewHandler.currentFullPreview.get(structureName);
        CapsuleTemplateRenderer renderer = CapsulePreviewHandler.cachedFullPreview.get(structureName);
        StructurePlaceSettings placement = CapsuleItem.getPlacement(heldItemMainhand);
        try {
            renderer.changeTemplateIfDirty(
                    template,
                    thePlayer.level(),
                    destOriginPos,
                    recenterRotation(extendSize, placement),
                    placement,
                    2
            );
        } catch (RuntimeException e) {
            // the wireframe preview is displayed instead
            LOGGER.warn("Full preview of {} disabled: {}", structureName, e.toString());
            CapsulePreviewHandler.currentFullPreview.remove(structureName);
            CapsulePreviewHandler.cachedFullPreview.remove(structureName);
            return;
        }

        float glitchIntensity = (float) (Math.abs(Math.cos(time * 0.1f)) * Math.abs(Math.cos(time * 0.14f)) * Math.abs(Math.cos(time * 0.12f))) - 0.3f;
        glitchIntensity = (float) Math.min(0.05, Math.max(0, glitchIntensity));
        float glitchIntensity2 = ((float) (Math.cos(time * 0.12f) * Math.cos(time * 0.15f) * Math.cos(time * 0.14f))) * glitchIntensity;
        float glitchValue = (float) Math.min(0.12, Math.max(0, Math.tan(time * 0.5)));
        float glitchValuey = (float) Math.min(0.32, Math.max(0, Math.tan(time * 0.2)));
        float glitchValuez = (float) Math.min(0.12, Math.max(0, Math.tan(time * 0.8)));

        // the glitch moves and stretches the preview around the camera, then the template is placed at its destination
        final float glitchIntensityFinal = glitchIntensity;
        frame.add((collector, poseStack) -> {
            poseStack.translate(
                    glitchIntensity2 * glitchValue,
                    glitchIntensityFinal * glitchValuey,
                    glitchIntensity2 * glitchValuez);
            poseStack.scale(1 + glitchIntensity2 * glitchValuez, 1 + glitchIntensityFinal * glitchValuey, 1);
            poseStack.translate(destOriginPos.getX() - camera.x, destOriginPos.getY() - camera.y, destOriginPos.getZ() - camera.z);
            renderer.submitTemplate(collector, poseStack, CapsuleTemplateRenderer.PREVIEW_ALPHA);
        });
    }

    /**
     * Outlines the blocks of the template (or the capsule cube) at the deploy position, and outlines in red every block
     * that is in the way. In 1.21.1 these lines were drawn with RenderType.lines(), whose state set the depth test
     * (less or equal), the translucent blending and the window-scaled line width itself, overriding the RenderSystem
     * depth test and line width calls around them: RenderTypes.lines() and the window's line width give the same.
     */
    private static void DisplayWireframePreview(LocalPlayer thePlayer, ItemStack heldItemMainhand, int size, BlockHitResult rtc, int extendSize, BlockPos destOriginPos, String structureName, AABB errorBoundingBox, boolean haveFullPreview, Vec3 camera, float lineWidth) {
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

        StructurePlaceSettings placement = CapsuleItem.getPlacement(heldItemMainhand);
        List<AABB> errorBoxes = new ArrayList<>();
        List<AABB> destBoxes = new ArrayList<>();
        int color = 0xDDDDDD;

        for (AABB bb : blockspos) {
            BlockPos recenter = recenterRotation(extendSize, placement);
            AABB dest = CapsuleTemplate.transformedAxisAlignedBB(placement, bb)
                    .move(destOriginPos.getX(), destOriginPos.getY() + 0.01, destOriginPos.getZ())
                    .move(recenter.getX(), recenter.getY(), recenter.getZ())
                    .expandTowards(1, 1, 1);

            if (CapsuleItem.hasState(heldItemMainhand, EMPTY)) {
                // (2/2) hack this renderer for specific case : capture of a 1-sized empty capsule
                color = CapsuleItem.getBaseColor(heldItemMainhand);
            } else {
                for (double j = dest.minZ; j < dest.maxZ; ++j) {
                    for (double k = dest.minY; k < dest.maxY; ++k) {
                        for (double l = dest.minX; l < dest.maxX; ++l) {
                            BlockPos pos = BlockPos.containing(l, k, j);
                            if (!thePlayer.level().getBlockState(pos).is(CapsuleTags.overridable)) {
                                AABB error = errorBoundingBox.move(pos).move(camera.reverse());
                                errorBoxes.add(error);
                                errorBoxes.add(error.inflate(0.005));
                                errorBoxes.add(error.deflate(0.005));
                            }
                        }
                    }
                }
            }

            if (!haveFullPreview) {
                destBoxes.add(dest.move(camera.reverse()));
            }
        }

        final int destColor = ARGB.opaque(color);
        frame.add((collector, poseStack) -> {
            for (AABB box : errorBoxes) submitLineBox(collector, poseStack, box, ERROR_COLOR, lineWidth);
            for (AABB box : destBoxes) submitLineBox(collector, poseStack, box, destColor, lineWidth);
        });
    }

    private static boolean isDeployable(ItemStack heldItemMainhand) {
        return CapsuleItem.hasState(heldItemMainhand, ACTIVATED)
                || CapsuleItem.hasState(heldItemMainhand, ONE_USE_ACTIVATED)
                || CapsuleItem.hasState(heldItemMainhand, BLUEPRINT)
                || CapsuleItem.getSize(heldItemMainhand) == 1 && !CapsuleItem.hasState(heldItemMainhand, DEPLOYED);
    }

    private static void tryPreviewRecall(ItemStack heldItem, Vec3 camera, float lineWidth) {
        // an item is in hand
        if (heldItem != null) {
            Item heldItemItem = heldItem.getItem();
            // it's an empty capsule : show capture zones
            if (heldItemItem instanceof CapsuleItem
                    && (CapsuleItem.hasState(heldItem, DEPLOYED) || CapsuleItem.hasState(heldItem, BLUEPRINT))
                    && NBTHelper.hasTag(heldItem)) {
                CompoundTag tag = NBTHelper.getTag(heldItem);
                if (tag != null && tag.contains("spawnPosition")) {
                    previewRecall(heldItem, camera, lineWidth);
                }
            }
        }
    }

    private static void tryPreviewLinkedInventory(LocalPlayer player, ItemStack heldItem, Vec3 camera) {
        if (heldItem != null) {
            Item heldItemItem = heldItem.getItem();
            if (heldItemItem instanceof CapsuleItem
                    && CapsuleItem.isBlueprint(heldItem)
                    && CapsuleItem.hasSourceInventory(heldItem)) {
                BlockPos location = CapsuleItem.getSourceInventoryLocation(heldItem);
                ResourceKey<Level> dimension = CapsuleItem.getSourceInventoryDimension(heldItem);
                if (location != null
                        && dimension != null
                        && dimension.equals(player.level().dimension())
                        && location.distToCenterSqr(player.getX(), player.getY(), player.getZ()) < 60 * 60) {
                    previewLinkedInventory(location, camera);
                }
            }
        }
    }

    private static void previewLinkedInventory(BlockPos location, Vec3 camera) {
        float x = (float) (location.getX() - camera.x);
        float y = (float) (location.getY() - camera.y);
        float z = (float) (location.getZ() - camera.z);
        float offset = 0.01f;
        frame.add((collector, poseStack) -> collector.submitCustomGeometry(poseStack, RenderTypes.debugQuads(),
                (pose, buffer) -> drawCube(pose, x - offset, y - offset, z - offset, x + 1 + offset, y + 1 + offset, z + 1 + offset,
                        buffer, 91, 156, 255, 80)));
    }

    private static void previewRecall(ItemStack capsule, Vec3 camera, float lineWidth) {
        CompoundTag tag = NBTHelper.getTag(capsule);
        if (tag == null) return;
        CompoundTag linkPos = tag.getCompoundOrEmpty("spawnPosition");

        int size = CapsuleItem.getSize(capsule);
        int extendSize = (size - 1) / 2;
        int color = CapsuleItem.getBaseColor(capsule);

        AABB boundingBox = Spacial.getBB(linkPos.getIntOr("x", 0) + extendSize, linkPos.getIntOr("y", 0) - 1, linkPos.getIntOr("z", 0) + extendSize, size, extendSize)
                .move(camera.reverse());
        double boxTime = time;
        frame.add((collector, poseStack) -> renderRecallBox(collector, poseStack, color, boundingBox, boxTime, lineWidth));
    }

    private static void setCaptureTESizeColor(int size, int color, Level worldIn) {
        if (size == lastSize && color == lastColor) return;

        // change MinecraftNBT of all existing BlockEntityCapture in the world to make them display the preview zone
        // remember it's client side only
        for (BlockEntityCapture te : new ArrayList<>(BlockEntityCapture.instances)) {
            if (te.getLevel() == worldIn) {
                if (te.getSize() != size || te.getColor() != color) {
                    te.setSizeAndColor(size, color);
                    if (te.getBlockState().hasProperty(BlockCapsuleMarker.TRIGGERED)) {
                        worldIn.setBlock(te.getBlockPos(), te.getBlockState().setValue(BlockCapsuleMarker.TRIGGERED, size > 0), 2);
                    }
                }
            }
        }
        lastSize = size;
        lastColor = color;
    }

    /**
     * The box of a deployed capsule or of a capture base, and five horizontal slices moving up and down inside it.
     *
     * @param boundingBox in the space of poseStack
     */
    public static void renderRecallBox(OrderedSubmitNodeCollector collector, PoseStack poseStack, int color, AABB boundingBox, double time, float lineWidth) {
        final float rf = ((color >> 16) & 0xFF) / 255f;
        final float gf = ((color >> 8) & 0xFF) / 255f;
        final float bf = (color & 0xFF) / 255f;
        submitLineBox(collector, poseStack, boundingBox, ARGB.colorFromFloat(200 / 255f, rf, gf, bf), lineWidth);
        for (int i = 0; i < 5; i++) {
            double i1 = getMovingEffectPos(boundingBox, i, time, 0.0015f);
            double i2 = getMovingEffectPos(boundingBox, i, time, 0.0017f);
            double lowI = Math.min(i1, i2);
            double highI = Math.max(i1, i2);
            AABB slice = new AABB(boundingBox.minX, lowI, boundingBox.minZ, boundingBox.maxX, highI, boundingBox.maxZ);
            submitLineBox(collector, poseStack, slice, ARGB.colorFromFloat(0.01f + i * 0.05f, rf, gf, bf), lineWidth);
        }
    }

    private static double getMovingEffectPos(AABB boundingBox, int t, double incTime, float offset) {
        return boundingBox.minY + (Math.cos(t * incTime * offset) * 0.5 + 0.5) * (boundingBox.maxY - boundingBox.minY);
    }
}
