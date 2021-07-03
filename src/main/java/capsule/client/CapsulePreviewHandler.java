package capsule.client;

import capsule.Config;
import capsule.blocks.CaptureTESR;
import capsule.blocks.TileEntityCapture;
import capsule.helpers.Spacial;
import capsule.items.CapsuleItem;
import capsule.structure.CapsuleTemplate;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

import static capsule.client.RendererUtils.*;
import static capsule.structure.CapsuleTemplate.recenterRotation;

public class CapsulePreviewHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final Map<String, List<AxisAlignedBB>> currentPreview = new HashMap<>();
    public static final Map<String, CapsuleTemplate> currentFullPreview = new HashMap<>();

    private int lastSize = 0;
    private int lastColor = 0;

    private static int uncompletePreviewsCount = 0;
    private static int completePreviewsCount = 0;
    private static String uncompletePreviewsCountStructure = null;

    static double time = 0;
    static FakeWorld fakeWorld = new FakeWorld();
    public static final double NS_TO_MS = 0.000001d;

    public CapsulePreviewHandler() {
    }


    /**
     * Render recall preview when deployed capsule in hand
     */
    @SubscribeEvent
    public void onWorldRenderLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        time += 1;

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
    public void onLivingUpdateEvent(PlayerTickEvent event) {

        // do something to player every update tick:
        if (event.player instanceof EntityPlayerSP && event.phase.equals(Phase.START)) {
            EntityPlayerSP player = (EntityPlayerSP) event.player;
            tryPreviewCapture(player, player.getHeldItemMainhand());
        }
    }

    /**
     * try to spot a templateDownload command for client
     */
    @SubscribeEvent
    public void OnClientChatEvent(ClientChatEvent event) {
        if ("/capsule downloadTemplate".equals(event.getMessage())) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.player != null) {
                ItemStack heldItem = mc.player.getHeldItemMainhand();
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
                        NBTTagCompound compoundnbt = template.writeToNBT(new NBTTagCompound());
                        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
                        Path filePath = path.resolve(df.format(new Date()) + "-" + structureName + ".nbt");
                        CompressedStreamTools.writeCompressed(compoundnbt, new DataOutputStream(new FileOutputStream(filePath.toFile())));
                        mc.player.sendMessage(new TextComponentString("→ <minecraftInstance>/" + filePath.toString().replace("\\", "/")));
                    } catch (Throwable var21) {
                        LOGGER.error(var21);
                        mc.player.sendMessage(new TextComponentTranslation("capsule.error.cantDownload"));
                    }
                } else {
                    mc.player.sendMessage(new TextComponentTranslation("capsule.error.cantDownload"));
                }
            }
        }
    }


    private boolean tryPreviewCapture(EntityPlayerSP player, ItemStack heldItem) {
        // an item is in hand
        if (!heldItem.isEmpty()) {
            Item heldItemItem = heldItem.getItem();
            // it's an empty capsule : show capture zones
            if (heldItemItem instanceof CapsuleItem && (heldItem.getItemDamage() == CapsuleItem.STATE_EMPTY || heldItem.getItemDamage() == CapsuleItem.STATE_EMPTY_ACTIVATED)) {
                CapsuleItem capsule = (CapsuleItem) heldItem.getItem();
                //noinspection ConstantConditions
                if (heldItem.hasTagCompound() && heldItem.getTagCompound().hasKey("size")) {
                    setCaptureTESizeColor(heldItem.getTagCompound().getInteger("size"), CapsuleItem.getBaseColor(heldItem), player.getEntityWorld());
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
    private void tryPreviewDeploy(EntityPlayerSP thePlayer, float partialTicks, ItemStack heldItemMainhand) {

        if (heldItemMainhand.getItem() instanceof CapsuleItem
                && heldItemMainhand.hasTagCompound()
                && (heldItemMainhand.getItemDamage() == CapsuleItem.STATE_ACTIVATED
                || heldItemMainhand.getItemDamage() == CapsuleItem.STATE_ONE_USE_ACTIVATED
                || heldItemMainhand.getItemDamage() == CapsuleItem.STATE_BLUEPRINT
                || CapsuleItem.getSize(heldItemMainhand) == 1 && heldItemMainhand.getItemDamage() != CapsuleItem.STATE_DEPLOYED)
        ) {
            int size = CapsuleItem.getSize(heldItemMainhand);
            RayTraceResult rtc = Spacial.clientRayTracePreview(thePlayer, partialTicks, size);
            if (rtc != null && rtc.typeOfHit == RayTraceResult.Type.BLOCK) {
                int extendSize = (size - 1) / 2;
                BlockPos destOriginPos = rtc.getBlockPos().add(rtc.sideHit.getDirectionVec()).add(-extendSize, 0.01, -extendSize);
                String structureName = heldItemMainhand.getTagCompound().getString("structureName");

                if (!structureName.equals(uncompletePreviewsCountStructure)) {
                    uncompletePreviewsCountStructure = structureName;
                    uncompletePreviewsCount = 0;
                    completePreviewsCount = 0;
                }

                AxisAlignedBB errorBoundingBox = new AxisAlignedBB(
                        0,
                        +0.01,
                        0,
                        1.01,
                        1.01,
                        1.01);

                synchronized (CapsulePreviewHandler.currentPreview) {
                    synchronized (CapsulePreviewHandler.currentFullPreview) {
                        boolean haveFullPreview = CapsulePreviewHandler.currentFullPreview.containsKey(structureName);
                        boolean isRenderComplete = false;
                        if (haveFullPreview && completePreviewsCount + uncompletePreviewsCount > 60) {
                            // switch off full preview if it cannot render properly over 60 frames
                            haveFullPreview = completePreviewsCount > uncompletePreviewsCount;
                        }
                        if (haveFullPreview) {
                            isRenderComplete = DisplayFullPreview(destOriginPos, structureName, extendSize, heldItemMainhand, thePlayer.world);
                            if (isRenderComplete) {
                                completePreviewsCount++;
                            } else {
                                uncompletePreviewsCount++;
                            }
                        }

                        if (CapsulePreviewHandler.currentPreview.containsKey(structureName) || size == 1) {
                            DisplayWireframePreview(thePlayer, heldItemMainhand, size, rtc, extendSize, destOriginPos, structureName, errorBoundingBox, haveFullPreview && isRenderComplete);
                        }
                    }
                }
            }
        }
    }

    private static boolean DisplayFullPreview(BlockPos destOriginPos, String structureName, int extendSize, ItemStack heldItemMainhand, IBlockAccess world) {
        boolean isRenderComplete = true;
        RenderManager info = Minecraft.getMinecraft().getRenderManager();
        TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
        CapsuleTemplate template = CapsulePreviewHandler.currentFullPreview.get(structureName);
        PlacementSettings placement = CapsuleItem.getPlacement(heldItemMainhand);

        BlockRendererDispatcher blockrendererdispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();

        float glitchIntensity = (float) (Math.cos(time * 0.1f) * Math.cos(time * 0.14f) * Math.cos(time * 0.12f)) - 0.3f;
        glitchIntensity = (float) Math.min(0.05, Math.max(0, glitchIntensity));
        float glitchIntensity2 = ((float) (Math.cos(time * 0.12f) * Math.cos(time * 0.15f) * Math.cos(time * 0.14f))) * glitchIntensity;
        float glitchValue = (float) Math.min(0.12, Math.max(0, Math.tan(time * 0.5)));
        float glitchValuey = (float) Math.min(0.32, Math.max(0, Math.tan(time * 0.2)));
        float glitchValuez = (float) Math.min(0.12, Math.max(0, Math.tan(time * 0.8)));

        long start = System.nanoTime();

        GlStateManager.pushMatrix();
        GlStateManager.translate(
                glitchIntensity2 * glitchValue,
                glitchIntensity * glitchValuey,
                glitchIntensity2 * glitchValuez);
        GlStateManager.scale(1 + glitchIntensity2 * glitchValuez, 1 + glitchIntensity * glitchValuey, 1);

        fakeWorld.clear();
        for (Template.BlockInfo blockInfo : template.blocks) {
            BlockPos blockpos = CapsuleTemplate.transformedBlockPos(placement, blockInfo.pos)
                    .add(recenterRotation(extendSize, placement))
                    .add(destOriginPos);
            IBlockState state = blockInfo.blockState;
            fakeWorld.add(blockpos, blockInfo);
            // .mirror(placement.getMirror()).rotate(placement.getRotation());

            if (state != Blocks.AIR.getDefaultState() && state.getRenderType() != EnumBlockRenderType.INVISIBLE) {
                GlStateManager.pushMatrix();

                GlStateManager.translate(
                        -info.viewerPosX,
                        -info.viewerPosY,
                        -info.viewerPosZ
                );

                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                GlStateManager.depthMask(false);
                RenderHelper.disableStandardItemLighting();
                GlStateManager.enableLighting();
                textureManager.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
                if (Minecraft.isAmbientOcclusionEnabled()) {
                    GlStateManager.shadeModel(GL11.GL_SMOOTH);
                } else {
                    GlStateManager.shadeModel(GL11.GL_FLAT);
                }

                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder bufferBuilder = tessellator.getBuffer();
                bufferBuilder.begin(7, DefaultVertexFormats.BLOCK);

                blockrendererdispatcher.renderBlock(state, blockpos, fakeWorld, bufferBuilder);

                GlStateManager.disableLighting();
                RenderHelper.enableStandardItemLighting();
                GlStateManager.depthMask(true);

                tessellator.draw();
                GlStateManager.popMatrix();
            }
            net.minecraftforge.client.ForgeHooksClient.setRenderLayer(null);

            // limit to 8ms render during profiling
            if (completePreviewsCount + uncompletePreviewsCount <= 60) {
                long elapsedTime = System.nanoTime() - start;
                if (elapsedTime * NS_TO_MS > 8) {
                    isRenderComplete = false;
                    break;
                }
            }
        }
        GlStateManager.popMatrix();
        return isRenderComplete;
    }

    private void DisplayWireframePreview(EntityPlayerSP thePlayer, ItemStack heldItemMainhand, int size, RayTraceResult rtc, int extendSize, BlockPos destOriginPos, String structureName, AxisAlignedBB errorBoundingBox, boolean haveFullPreview) {
        List<AxisAlignedBB> blockspos = new ArrayList<>();
        if (size > 1) {
            blockspos = CapsulePreviewHandler.currentPreview.get(structureName);
        } else if (heldItemMainhand.getItemDamage() == CapsuleItem.STATE_EMPTY) {
            // (1/2) hack this renderer for specific case : capture of a 1-sized empty capsule
            BlockPos pos = rtc.getBlockPos().subtract(destOriginPos);
            blockspos.add(new AxisAlignedBB(pos, pos));
        }
        if (blockspos.isEmpty()) {
            BlockPos pos = new BlockPos(extendSize, 0, extendSize);
            blockspos.add(new AxisAlignedBB(pos, pos));
        }

        doPositionPrologue();
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
            if (heldItemMainhand.getItemDamage() == CapsuleItem.STATE_EMPTY) {
                // (2/2) hack this renderer for specific case : capture of a 1-sized empty capsule
                GL11.glLineWidth(5.0F);
                color = CapsuleItem.getBaseColor(heldItemMainhand);
            } else {
                for (double j = dest.minZ; j < dest.maxZ; ++j) {
                    for (double k = dest.minY; k < dest.maxY; ++k) {
                        for (double l = dest.minX; l < dest.maxX; ++l) {
                            BlockPos pos = new BlockPos(l, k, j);
                            if (!Config.overridableBlocks.contains(thePlayer.getEntityWorld().getBlockState(pos).getBlock())) {
                                GL11.glLineWidth(5.0F);
                                bufferBuilder.begin(2, DefaultVertexFormats.POSITION);
                                setColor(0xaa0000, 50);
                                drawCapsuleCube(errorBoundingBox.offset(pos), bufferBuilder);
                                tessellator.draw();
                            }
                        }
                    }
                }
            }

            if (!haveFullPreview) {
                GL11.glLineWidth(1.0F);
                bufferBuilder.begin(2, DefaultVertexFormats.POSITION);
                setColor(color, 50);
                drawCapsuleCube(dest, bufferBuilder);
                tessellator.draw();
            }
        }

        setColor(0xFFFFFF, 255);
        doWireEpilogue();
        doPositionEpilogue();
    }

    private void tryPreviewRecall(ItemStack heldItem) {
        // an item is in hand
        if (heldItem != null) {
            Item heldItemItem = heldItem.getItem();
            // it's an empty capsule : show capture zones
            //noinspection ConstantConditions
            if (heldItemItem instanceof CapsuleItem
                    && (heldItem.getItemDamage() == CapsuleItem.STATE_DEPLOYED || heldItem.getItemDamage() == CapsuleItem.STATE_BLUEPRINT)
                    && heldItem.hasTagCompound()
                    && heldItem.getTagCompound().hasKey("spawnPosition")) {
                previewRecall(heldItem);
            }
        }
    }

    private void tryPreviewLinkedInventory(EntityPlayerSP player, ItemStack heldItem) {
        if (heldItem != null) {
            Item heldItemItem = heldItem.getItem();
            if (heldItemItem instanceof CapsuleItem
                    && CapsuleItem.isBlueprint(heldItem)
                    && CapsuleItem.hasSourceInventory(heldItem)) {
                BlockPos location = CapsuleItem.getSourceInventoryLocation(heldItem);
                Integer dimension = CapsuleItem.getSourceInventoryDimension(heldItem);
                if (location != null
                        && dimension != null
                        && dimension.equals(player.dimension)
                        && location.distanceSqToCenter(player.posX, player.posY, player.posZ) < 60 * 60) {
                    previewLinkedInventory(location, heldItem);
                }
            }
        }
    }

    private void previewLinkedInventory(BlockPos location, ItemStack capsule) {

        float shrink = 0.05f;
        AxisAlignedBB boundingBox = new AxisAlignedBB(
                +shrink + location.getX(),
                +shrink + location.getY(),
                +shrink + location.getZ(),
                1 - shrink + location.getX(),
                1 - shrink + location.getY(),
                1 - shrink + location.getZ());

        doPositionPrologue();
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

    private void previewRecall(ItemStack capsule) {
        if (capsule.getTagCompound() == null) return;
        NBTTagCompound linkPos = capsule.getTagCompound().getCompoundTag("spawnPosition");

        int size = CapsuleItem.getSize(capsule);
        int extendSize = (size - 1) / 2;
        int color = CapsuleItem.getBaseColor(capsule);

        CaptureTESR.drawCaptureZone(
                linkPos.getInteger("x") + extendSize,
                linkPos.getInteger("y") - 1,
                linkPos.getInteger("z") + extendSize, size,
                extendSize, color);
    }

    private void setCaptureTESizeColor(int size, int color, World worldIn) {
        if (size == lastSize && color == lastColor) return;

        // change MinecraftNBT of all existing TileEntityCapture in the world to make them display the preview zone
        // remember it's client side only
        for (TileEntityCapture te : TileEntityCapture.instances) {
            if (te.getWorld() == worldIn) {
                TileEntityCapture tec = te;
                tec.getTileData().setInteger("size", size);
                tec.getTileData().setInteger("color", color);
                worldIn.markBlockRangeForRenderUpdate(
                        te.getPos().add(-size / 2, -size / 2, -size / 2),
                        te.getPos().add(size / 2, size / 2, size / 2)
                );
            }
        }
        lastSize = size;
        lastColor = color;
    }


}

class FakeWorld implements IBlockAccess {
    private HashMap<BlockPos, Template.BlockInfo> blocks = new HashMap<BlockPos, Template.BlockInfo>();

    public FakeWorld() {
    }

    @Nullable
    @Override
    public TileEntity getTileEntity(BlockPos pos) {
        return null;
    }

    @Override
    public int getCombinedLight(BlockPos pos, int lightValue) {
        return 15;
    }

    @Override
    public IBlockState getBlockState(BlockPos pos) {
        return blocks.containsKey(pos) ? blocks.get(pos).blockState : Blocks.AIR.getDefaultState();
    }

    @Override
    public boolean isAirBlock(BlockPos pos) {
        return !blocks.containsKey(pos);
    }

    @Override
    public Biome getBiome(BlockPos pos) {
        return net.minecraft.init.Biomes.PLAINS;
    }

    @Override
    public int getStrongPower(BlockPos pos, EnumFacing direction) {
        return 0;
    }

    @Override
    public WorldType getWorldType() {
        return WorldType.DEFAULT;
    }

    @Override
    public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean _default) {
        return blocks.containsKey(pos) && blocks.get(pos).blockState.isSideSolid(this, pos, side);
    }

    public void clear() {
        blocks.clear();
    }

    public void add(BlockPos blockpos, Template.BlockInfo state) {
        blocks.put(blockpos, state);
    }
}