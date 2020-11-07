package capsule.client;

import capsule.CapsuleMod;
import capsule.Config;
import capsule.blocks.CaptureTESR;
import capsule.blocks.TileEntityCapture;
import capsule.helpers.Spacial;
import capsule.items.CapsuleItem;
import capsule.structure.CapsuleTemplate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static capsule.client.RendererUtils.*;
import static capsule.structure.CapsuleTemplate.recenterRotation;

@Mod.EventBusSubscriber(modid = CapsuleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CapsulePreviewHandler {
    public static final Map<String, List<AxisAlignedBB>> currentPreview = new HashMap<>();
    private static int lastSize = 0;
    private static int lastColor = 0;

    public CapsulePreviewHandler() {
    }

    /**
     * Render recall preview when deployed capsule in hand
     */
    @SubscribeEvent
    public static void onWorldRenderLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player != null) {
            tryPreviewRecall(mc.player.getHeldItemMainhand());
            tryPreviewDeploy(mc.player, event.getPartialTicks(), mc.player.getHeldItemMainhand());
            tryPreviewLinkedInventory(mc.player, mc.player.getHeldItemMainhand());
        }
    }

    /**
     * set captureBlock data (clientside only ) when capsule is in hand.
     */
    @SubscribeEvent
    public static void onLivingUpdateEvent(TickEvent.PlayerTickEvent event) {
        // do something to player every update tick:
        if (event.player instanceof ClientPlayerEntity && event.phase.equals(TickEvent.Phase.START)) {
            ClientPlayerEntity player = (ClientPlayerEntity) event.player;
            tryPreviewCapture(player, player.getHeldItemMainhand());
        }
    }

    private static boolean tryPreviewCapture(ClientPlayerEntity player, ItemStack heldItem) {
        // an item is in hand
        if (!heldItem.isEmpty()) {
            Item heldItemItem = heldItem.getItem();
            // it's an empty capsule : show capture zones
            if (heldItemItem instanceof CapsuleItem && (heldItem.getDamage() == CapsuleItem.STATE_EMPTY || heldItem.getDamage() == CapsuleItem.STATE_EMPTY_ACTIVATED)) {
                //noinspection ConstantConditions
                if (heldItem.hasTag() && heldItem.getTag().contains("size")) {
                    setCaptureTESizeColor(heldItem.getTag().getInt("size"), CapsuleItem.getBaseColor(heldItem), player.getEntityWorld());
                    return true;
                }

            } else {
                setCaptureTESizeColor(0, 0, player.getEntityWorld());
            }
        } else {
            setCaptureTESizeColor(0, 0, player.getEntityWorld());
        }

        return false;
    }


    @SuppressWarnings("ConstantConditions")
    private static void tryPreviewDeploy(ClientPlayerEntity thePlayer, float partialTicks, ItemStack heldItemMainhand) {


        if (heldItemMainhand.getItem() instanceof CapsuleItem
                && heldItemMainhand.hasTag()
                && (heldItemMainhand.getDamage() == CapsuleItem.STATE_ACTIVATED
                || heldItemMainhand.getDamage() == CapsuleItem.STATE_ONE_USE_ACTIVATED
                || heldItemMainhand.getDamage() == CapsuleItem.STATE_BLUEPRINT
                || CapsuleItem.getSize(heldItemMainhand) == 1 && heldItemMainhand.getDamage() != CapsuleItem.STATE_DEPLOYED)
        ) {
            int size = CapsuleItem.getSize(heldItemMainhand);
            BlockRayTraceResult rtc = Spacial.clientRayTracePreview(thePlayer, partialTicks, size);
            if (rtc != null && rtc.getType() == RayTraceResult.Type.BLOCK) {
                int extendSize = (size - 1) / 2;
                BlockPos destOriginPos = rtc.getPos().add(rtc.getFace().getDirectionVec()).add(-extendSize, 0.01, -extendSize);
                String structureName = heldItemMainhand.getTag().getString("structureName");

                AxisAlignedBB errorBoundingBox = new AxisAlignedBB(
                        0,
                        +0.01,
                        0,
                        1.01,
                        1.01,
                        1.01);

                synchronized (CapsulePreviewHandler.currentPreview) {
                    if (CapsulePreviewHandler.currentPreview.containsKey(structureName) || size == 1) {

                        List<AxisAlignedBB> blockspos = new ArrayList<>();
                        if (size > 1) {
                            blockspos = CapsulePreviewHandler.currentPreview.get(structureName);
                        } else if (heldItemMainhand.getDamage() == CapsuleItem.STATE_EMPTY) {
                            // (1/2) hack this renderer for specific case : capture of a 1-sized empty capsule
                            BlockPos pos = rtc.getPos().subtract(destOriginPos);
                            blockspos.add(new AxisAlignedBB(pos, pos));
                        }
                        if (blockspos.isEmpty()) {
                            BlockPos pos = new BlockPos(extendSize, 0, extendSize);
                            blockspos.add(new AxisAlignedBB(pos, pos));
                        }

                        doPositionPrologue(Minecraft.getInstance().getRenderManager().info);
                        doWirePrologue();
                        Tessellator tessellator = Tessellator.getInstance();
                        BufferBuilder bufferBuilder = tessellator.getBuffer();

                        PlacementSettings placement = CapsuleItem.getPlacement(heldItemMainhand);

                        for (AxisAlignedBB bb : blockspos) {
                            BlockPos recenter = recenterRotation(extendSize, placement);
                            AxisAlignedBB dest = CapsuleTemplate.transformedAxisAlignedBB(placement, bb)
                                    .offset(destOriginPos.getX(), destOriginPos.getY() + 0.01, destOriginPos.getZ())
                                    .offset(recenter.getX(), recenter.getY(), recenter.getZ())
                                    .expand(1, 1, 1);

                            int color = 0xDDDDDD;
                            if (heldItemMainhand.getDamage() == CapsuleItem.STATE_EMPTY) {
                                // (2/2) hack this renderer for specific case : capture of a 1-sized empty capsule
                                GlStateManager.lineWidth(5.0F);
                                color = CapsuleItem.getBaseColor(heldItemMainhand);
                            } else {
                                for (double j = dest.minZ; j < dest.maxZ; ++j) {
                                    for (double k = dest.minY; k < dest.maxY; ++k) {
                                        for (double l = dest.minX; l < dest.maxX; ++l) {
                                            BlockPos pos = new BlockPos(l, k, j);
                                            if (!Config.overridableBlocks.contains(thePlayer.getEntityWorld().getBlockState(pos).getBlock())) {
                                                GlStateManager.lineWidth(5.0F);
                                                bufferBuilder.begin(2, DefaultVertexFormats.POSITION);
                                                setColor(0xaa0000, 50);
                                                drawCapsuleCube(errorBoundingBox.offset(pos), bufferBuilder);
                                                tessellator.draw();
                                            }
                                        }
                                    }
                                }
                            }

                            GlStateManager.lineWidth(1.0F);
                            bufferBuilder.begin(2, DefaultVertexFormats.POSITION);
                            setColor(color, 50);
                            drawCapsuleCube(dest, bufferBuilder);
                            tessellator.draw();
                        }

                        setColor(0xFFFFFF, 255);
                        doWireEpilogue();
                        doPositionEpilogue();
                    }
                }
            }
        }

    }

    private static void tryPreviewRecall(ItemStack heldItem) {
        // an item is in hand
        if (heldItem != null) {
            Item heldItemItem = heldItem.getItem();
            // it's an empty capsule : show capture zones
            //noinspection ConstantConditions
            if (heldItemItem instanceof CapsuleItem
                    && (heldItem.getDamage() == CapsuleItem.STATE_DEPLOYED || heldItem.getDamage() == CapsuleItem.STATE_BLUEPRINT)
                    && heldItem.hasTag()
                    && heldItem.getTag().contains("spawnPosition")) {
                previewRecall(heldItem);
            }
        }
    }

    private static void tryPreviewLinkedInventory(ClientPlayerEntity player, ItemStack heldItem) {
        if (heldItem != null) {
            Item heldItemItem = heldItem.getItem();
            if (heldItemItem instanceof CapsuleItem
                    && CapsuleItem.isBlueprint(heldItem)
                    && CapsuleItem.hasSourceInventory(heldItem)) {
                BlockPos location = CapsuleItem.getSourceInventoryLocation(heldItem);
                Integer dimension = CapsuleItem.getSourceInventoryDimension(heldItem);
                if (location != null
                        && dimension != null
                        && dimension.equals(player.dimension.getId())
                        && location.distanceSq(player.getPosX(), player.getPosY(), player.getPosZ(), true) < 60 * 60) {
                    previewLinkedInventory(location, heldItem);
                }
            }
        }
    }

    private static void previewLinkedInventory(BlockPos location, ItemStack capsule) {
        doPositionPrologue(Minecraft.getInstance().getRenderManager().info);
        doOverlayPrologue();

        setColor(0x5B9CFF, 80);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        bufferBuilder.begin(7, DefaultVertexFormats.POSITION);
        drawCube(location, 0, bufferBuilder);
        tessellator.draw();
        setColor(0xFFFFFF, 255);

        doOverlayEpilogue();
        doPositionEpilogue();
    }

    private static void previewRecall(ItemStack capsule) {
        if (capsule.getTag() == null) return;
        CompoundNBT linkPos = capsule.getTag().getCompound("spawnPosition");

        int size = CapsuleItem.getSize(capsule);
        int extendSize = (size - 1) / 2;
        int color = CapsuleItem.getBaseColor(capsule);

        CaptureTESR.drawCaptureZone(
                linkPos.getInt("x") + extendSize,
                linkPos.getInt("y") - 1,
                linkPos.getInt("z") + extendSize, size,
                extendSize, color, Minecraft.getInstance().getRenderManager().info);
    }

    private static void setCaptureTESizeColor(int size, int color, World worldIn) {
        if (size == lastSize && color == lastColor) return;

        // change MinecraftNBT of all existing TileEntityCapture in the world to make them display the preview zone
        // remember it's client side only
        for (TileEntityCapture te : TileEntityCapture.instances) {
            if (te.getWorld() == worldIn) {
                TileEntityCapture tec = te;
                tec.getTileData().putInt("size", size);
                tec.getTileData().putInt("color", color);
                // FIXME: check if the capture block still displays properly
//                worldIn.markBlockRangeForRenderUpdate(
//                        te.getPos().add(-size / 2, -size / 2, -size / 2),
//                        te.getPos().add(size / 2, size / 2, size / 2)
//                );
            }
        }
        lastSize = size;
        lastColor = color;
    }


}
