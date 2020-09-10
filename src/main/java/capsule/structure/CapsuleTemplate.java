package capsule.structure;

import capsule.Config;
import com.google.common.collect.Lists;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.item.PaintingEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Mirror;
import net.minecraft.util.ObjectIntIdentityMap;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.template.BlockRotationProcessor;
import net.minecraft.world.gen.structure.template.ITemplateProcessor;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Copy of mc original net.minecraft.world.gen.structure.template.Template class, but having fields public to allow external manipulation.
 * Also include some specificities to capsules
 *
 * @author Lythom
 */
public class CapsuleTemplate {
    protected static final Logger LOGGER = LogManager.getLogger(CapsuleTemplate.class);

    public final List<Template.BlockInfo> blocks = Lists.<Template.BlockInfo>newArrayList();
    public final List<Template.EntityInfo> entities = Lists.<Template.EntityInfo>newArrayList();
    public Map<BlockPos, Block> occupiedPositions = null;

    /**
     * size of the structure
     */
    public BlockPos size = BlockPos.ORIGIN;
    /**
     * The author of this template.
     */
    public String author = "?";

    public BlockPos getSize() {
        return this.size;
    }

    public void setAuthor(String authorIn) {
        this.author = authorIn;
    }

    public String getAuthor() {
        return this.author;
    }

    public static BlockPos transformedBlockPos(PlacementSettings placementIn, BlockPos pos) {
        return transformedBlockPos(pos, placementIn.getMirror(), placementIn.getRotation());
    }

    public static AxisAlignedBB transformedAxisAlignedBB(PlacementSettings placementIn, AxisAlignedBB bb) {
        return new AxisAlignedBB(
                transformedBlockPos(new BlockPos(bb.minX, bb.minY, bb.minZ), placementIn.getMirror(), placementIn.getRotation()),
                transformedBlockPos(new BlockPos(bb.maxX, bb.maxY, bb.maxZ), placementIn.getMirror(), placementIn.getRotation())
        );
    }

    private void addEntitiesToWorld(World worldIn, BlockPos pos, Mirror mirrorIn, Rotation rotationIn, @Nullable StructureBoundingBox aabb, List<Entity> spawnedEntities) {
        for (Template.EntityInfo template$entityinfo : this.entities) {
            BlockPos recenterOffset = recenterRotation((size.getX() - 1) / 2, mirrorIn, rotationIn);
            BlockPos blockpos = transformedBlockPos(template$entityinfo.blockPos, mirrorIn, rotationIn).add(pos).add(recenterOffset);

            if (aabb == null || aabb.isVecInside(blockpos)) {
                CompoundNBT nbttagcompound = template$entityinfo.entityData;
                Vec3d vec3d = transformedVec3d(template$entityinfo.pos, mirrorIn, rotationIn);
                Vec3d vec3d1 = vec3d
                        .addVector((double) pos.getX(), (double) pos.getY(), (double) pos.getZ())
                        .addVector((double) recenterOffset.getX(), (double) recenterOffset.getY(), (double) recenterOffset.getZ());
                ListNBT nbttaglist = new ListNBT();
                nbttaglist.appendTag(new DoubleNBT(vec3d1.x));
                nbttaglist.appendTag(new DoubleNBT(vec3d1.y));
                nbttaglist.appendTag(new DoubleNBT(vec3d1.z));
                nbttagcompound.setTag("Pos", nbttaglist);
                nbttagcompound.setUniqueId("UUID", UUID.randomUUID());
                Entity entity;

                try {
                    entity = EntityList.createEntityFromNBT(nbttagcompound, worldIn);
                } catch (Exception var15) {
                    entity = null;
                }

                if (entity != null) {
                    float f = entity.getMirroredYaw(mirrorIn);
                    f = f + (entity.rotationYaw - entity.getRotatedYaw(rotationIn));
                    entity.setLocationAndAngles(vec3d1.x, vec3d1.y, vec3d1.z, f, entity.rotationPitch);
                    worldIn.spawnEntity(entity);
                    if (spawnedEntities != null) spawnedEntities.add(entity);
                }
            }
        }
    }

    private static BlockPos transformedBlockPos(BlockPos pos, Mirror mirrorIn, Rotation rotationIn) {
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        boolean flag = true;

        switch (mirrorIn) {
            case LEFT_RIGHT:
                k = -k;
                break;
            case FRONT_BACK:
                i = -i;
                break;
            default:
                flag = false;
        }

        switch (rotationIn) {
            case COUNTERCLOCKWISE_90:
                return new BlockPos(k, j, -i);
            case CLOCKWISE_90:
                return new BlockPos(-k, j, i);
            case CLOCKWISE_180:
                return new BlockPos(-i, j, -k);
            default:
                return flag ? new BlockPos(i, j, k) : pos;
        }
    }

    private static Vec3d transformedVec3d(Vec3d vec, Mirror mirrorIn, Rotation rotationIn) {
        double d0 = vec.x;
        double d1 = vec.y;
        double d2 = vec.z;
        boolean flag = true;

        switch (mirrorIn) {
            case LEFT_RIGHT:
                d2 = 1.0D - d2;
                break;
            case FRONT_BACK:
                d0 = 1.0D - d0;
                break;
            default:
                flag = false;
        }

        switch (rotationIn) {
            case COUNTERCLOCKWISE_90:
                return new Vec3d(d2, d1, 1.0D - d0);
            case CLOCKWISE_90:
                return new Vec3d(1.0D - d2, d1, d0);
            case CLOCKWISE_180:
                return new Vec3d(1.0D - d0, d1, 1.0D - d2);
            default:
                return flag ? new Vec3d(d0, d1, d2) : vec;
        }
    }

    public CompoundNBT writeToNBT(CompoundNBT nbt) {
        CapsuleTemplate.BasicPalette template$basicpalette = new CapsuleTemplate.BasicPalette();
        ListNBT nbttaglist = new ListNBT();

        for (Template.BlockInfo template$blockinfo : this.blocks) {
            CompoundNBT nbttagcompound = new CompoundNBT();
            nbttagcompound.setTag("pos", this.writeInts(template$blockinfo.pos.getX(), template$blockinfo.pos.getY(), template$blockinfo.pos.getZ()));
            nbttagcompound.putInt("state", template$basicpalette.idFor(template$blockinfo.blockState));

            if (template$blockinfo.tileentityData != null) {
                nbttagcompound.setTag("nbt", template$blockinfo.tileentityData);
            }

            nbttaglist.appendTag(nbttagcompound);
        }

        ListNBT nbttaglist1 = new ListNBT();

        for (Template.EntityInfo template$entityinfo : this.entities) {
            CompoundNBT nbttagcompound1 = new CompoundNBT();
            nbttagcompound1.setTag("pos", this.writeDoubles(template$entityinfo.pos.x, template$entityinfo.pos.y, template$entityinfo.pos.z));
            nbttagcompound1.setTag("blockPos", this.writeInts(template$entityinfo.blockPos.getX(), template$entityinfo.blockPos.getY(), template$entityinfo.blockPos.getZ()));

            if (template$entityinfo.entityData != null) {
                nbttagcompound1.setTag("nbt", template$entityinfo.entityData);
            }

            nbttaglist1.appendTag(nbttagcompound1);
        }

        ListNBT nbttaglist2 = new ListNBT();

        for (BlockState iblockstate : template$basicpalette) {
            nbttaglist2.appendTag(NBTUtil.writeBlockState(new CompoundNBT(), iblockstate));
        }

        net.minecraftforge.fml.common.FMLCommonHandler.instance().getDataFixer().writeVersionData(nbt); //Moved up for MC updating reasons.
        nbt.setTag("palette", nbttaglist2);
        nbt.setTag("blocks", nbttaglist);
        nbt.setTag("entities", nbttaglist1);
        nbt.setTag("size", this.writeInts(this.size.getX(), this.size.getY(), this.size.getZ()));
        nbt.putString("author", this.author);
        nbt.putInt("DataVersion", 1343);

        // CAPSULE save already occupied positions when deployed
        ListNBT occupiedSpawnPositionstaglist = new ListNBT();
        if (this.occupiedPositions != null) {
            for (Map.Entry<BlockPos, Block> entry : occupiedPositions.entrySet()) {
                CompoundNBT nbtEntry = new CompoundNBT();
                nbtEntry.setLong("pos", entry.getKey().toLong());
                nbtEntry.putInt("blockId", Block.getIdFromBlock(entry.getValue()));
                occupiedSpawnPositionstaglist.appendTag(nbtEntry);
            }
            nbt.setTag("capsule_occupiedSources", occupiedSpawnPositionstaglist);
        }
        return nbt;
    }

    public void read(CompoundNBT compound) {
        this.blocks.clear();
        this.entities.clear();
        ListNBT nbttaglist = compound.getTagList("size", 3);
        this.size = new BlockPos(nbttaglist.getIntAt(0), nbttaglist.getIntAt(1), nbttaglist.getIntAt(2));
        this.author = compound.getString("author");
        CapsuleTemplate.BasicPalette template$basicpalette = new CapsuleTemplate.BasicPalette();
        ListNBT nbttaglist1 = compound.getTagList("palette", 10);

        for (int i = 0; i < nbttaglist1.size(); ++i) {
            template$basicpalette.addMapping(NBTUtil.readBlockState(nbttaglist1.getCompound(i)), i);
        }

        ListNBT nbttaglist3 = compound.getTagList("blocks", 10);

        for (int j = 0; j < nbttaglist3.size(); ++j) {
            CompoundNBT nbttagcompound = nbttaglist3.getCompound(j);
            ListNBT nbttaglist2 = nbttagcompound.getTagList("pos", 3);
            BlockPos blockpos = new BlockPos(nbttaglist2.getIntAt(0), nbttaglist2.getIntAt(1), nbttaglist2.getIntAt(2));
            BlockState iblockstate = template$basicpalette.stateFor(nbttagcompound.getInt("state"));
            if (iblockstate != null && iblockstate.getMaterial() != Material.AIR) {
                CompoundNBT nbttagcompound1;

                if (nbttagcompound.contains("nbt")) {
                    nbttagcompound1 = nbttagcompound.getCompound("nbt");
                } else {
                    nbttagcompound1 = null;
                }

                this.blocks.add(new Template.BlockInfo(blockpos, iblockstate, nbttagcompound1));
            }
        }

        ListNBT nbttaglist4 = compound.getTagList("entities", 10);

        for (int k = 0; k < nbttaglist4.size(); ++k) {
            CompoundNBT nbttagcompound3 = nbttaglist4.getCompound(k);
            ListNBT nbttaglist5 = nbttagcompound3.getTagList("pos", 6);
            Vec3d vec3d = new Vec3d(nbttaglist5.getDoubleAt(0), nbttaglist5.getDoubleAt(1), nbttaglist5.getDoubleAt(2));
            ListNBT nbttaglist6 = nbttagcompound3.getTagList("blockPos", 3);
            BlockPos blockpos1 = new BlockPos(nbttaglist6.getIntAt(0), nbttaglist6.getIntAt(1), nbttaglist6.getIntAt(2));

            if (nbttagcompound3.contains("nbt")) {
                CompoundNBT nbttagcompound2 = nbttagcompound3.getCompound("nbt");
                this.entities.add(new Template.EntityInfo(vec3d, blockpos1, nbttagcompound2));
            }
        }

        // CAPSULE read already occupied positions when deployed
        if (compound.contains("capsule_occupiedSources")) {
            Map<BlockPos, Block> occupiedSources = new HashMap<>();
            ListNBT list = compound.getTagList("capsule_occupiedSources", 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundNBT entry = list.getCompound(i);
                occupiedSources.put(BlockPos.fromLong(entry.getLong("pos")), Block.getBlockById(entry.getInt("blockId")));
            }
            this.occupiedPositions = occupiedSources;
        }
    }

    private ListNBT writeInts(int... values) {
        ListNBT nbttaglist = new ListNBT();

        for (int i : values) {
            nbttaglist.appendTag(new IntNBT(i));
        }

        return nbttaglist;
    }

    private ListNBT writeDoubles(double... values) {
        ListNBT nbttaglist = new ListNBT();

        for (double d0 : values) {
            nbttaglist.appendTag(new DoubleNBT(d0));
        }

        return nbttaglist;
    }

    static class BasicPalette implements Iterable<BlockState> {
        public static final BlockState DEFAULT_BLOCK_STATE = Blocks.AIR.getDefaultState();
        final ObjectIntIdentityMap<BlockState> ids;
        private int lastId;

        private BasicPalette() {
            this.ids = new ObjectIntIdentityMap<>(16);
        }

        public int idFor(BlockState state) {
            int i = this.ids.get(state);

            if (i == -1) {
                i = this.lastId++;
                this.ids.put(state, i);
            }

            return i;
        }

        @Nullable
        public BlockState stateFor(int id) {
            BlockState iblockstate = this.ids.getByValue(id);
            return iblockstate == null ? DEFAULT_BLOCK_STATE : iblockstate;
        }

        public Iterator<BlockState> iterator() {
            return this.ids.iterator();
        }

        public void addMapping(BlockState p_189956_1_, int p_189956_2_) {
            this.ids.put(p_189956_1_, p_189956_2_);
        }
    }


    // CAPSULE additions

    public void removeOccupiedPositions() {
        this.occupiedPositions = null;
    }

    public void saveOccupiedPositions(Map<BlockPos, Block> occupiedPositions) {
        this.occupiedPositions = occupiedPositions;
    }

    /**
     * takes blocks from the world and puts the data them into this template
     */
    public List<BlockPos> snapshotBlocksFromWorld(World worldIn, BlockPos startPos, BlockPos endPos,
                                                  Map<BlockPos, Block> occupiedPositionsToIgnore, List<Block> excluded, List<Entity> outCapturedEntities) {

        List<BlockPos> transferedBlocks = new ArrayList<>();

        if (endPos.getX() >= 1 && endPos.getY() >= 1 && endPos.getZ() >= 1) {
            BlockPos blockpos = startPos.add(endPos).add(-1, -1, -1);
            List<Template.BlockInfo> list = Lists.newArrayList();
            List<Template.BlockInfo> list1 = Lists.newArrayList();
            List<Template.BlockInfo> list2 = Lists.newArrayList();
            BlockPos blockpos1 = new BlockPos(Math.min(startPos.getX(), blockpos.getX()), Math.min(startPos.getY(), blockpos.getY()),
                    Math.min(startPos.getZ(), blockpos.getZ()));
            BlockPos blockpos2 = new BlockPos(Math.max(startPos.getX(), blockpos.getX()), Math.max(startPos.getY(), blockpos.getY()),
                    Math.max(startPos.getZ(), blockpos.getZ()));
            this.size = endPos;

            for (BlockPos.MutableBlockPos blockpos$mutableblockpos : BlockPos.getAllInBoxMutable(blockpos1, blockpos2)) {
                BlockPos blockpos3 = blockpos$mutableblockpos.subtract(blockpos1);
                BlockState iblockstate = worldIn.getBlockState(blockpos$mutableblockpos);
                Block iblock = iblockstate.getBlock();

                if (!excluded.contains(iblock) // excluded blocks are not captured at all
                        && (occupiedPositionsToIgnore == null // exclude sourceBlock that were already presents. Capture only if it was changed.
                        || !(occupiedPositionsToIgnore.keySet().contains(blockpos$mutableblockpos)
                        && occupiedPositionsToIgnore.get(blockpos$mutableblockpos).equals(iblock)))) {
                    TileEntity tileentity = worldIn.getTileEntity(blockpos$mutableblockpos);

                    if (tileentity != null) {
                        CompoundNBT nbttagcompound = tileentity.writeToNBT(new CompoundNBT());
                        nbttagcompound.remove("x");
                        nbttagcompound.remove("y");
                        nbttagcompound.remove("z");
                        list1.add(new Template.BlockInfo(blockpos3, iblockstate, nbttagcompound));
                    } else if (!iblockstate.isFullBlock() && !iblockstate.isFullCube()) {
                        list2.add(new Template.BlockInfo(blockpos3, iblockstate, null));
                    } else {
                        list.add(new Template.BlockInfo(blockpos3, iblockstate, null));
                    }
                    // save a copy
                    transferedBlocks.add(new BlockPos(blockpos$mutableblockpos.getX(), blockpos$mutableblockpos.getY(), blockpos$mutableblockpos.getZ()));
                }
            }

            blocks.clear();
            blocks.addAll(list);
            blocks.addAll(list1);
            blocks.addAll(list2);

            List<Entity> capturedEntities = this.snapshotNonLivingEntitiesFromWorld(worldIn, blockpos1, blockpos2.add(1, 1, 1));
            if (outCapturedEntities != null && capturedEntities != null) {
                outCapturedEntities.addAll(capturedEntities);
            }

            return transferedBlocks;
        }

        return transferedBlocks;
    }

    /**
     * takes blocks from the world and puts the data them into this template
     */
    public List<Entity> snapshotNonLivingEntitiesFromWorld(World worldIn, BlockPos startPos, BlockPos endPos) {

        // rewritten vanilla code from CapsuleTemplate.takeEntitiesFromWorld
        List<Entity> list = worldIn.getEntitiesWithinAABB(
                Entity.class,
                new AxisAlignedBB(startPos, endPos),
                entity -> !(entity instanceof ItemEntity) && (!(entity instanceof LivingEntity) || (entity instanceof ArmorStandEntity))
        );
        entities.clear();

        for (Entity entity : list) {
            Vec3d vec3d = new Vec3d(entity.posX - (double) startPos.getX(), entity.posY - (double) startPos.getY(), entity.posZ - (double) startPos.getZ());
            CompoundNBT nbttagcompound = new CompoundNBT();
            entity.writeToNBTOptional(nbttagcompound);
            BlockPos blockpos;

            if (entity instanceof PaintingEntity) {
                blockpos = ((PaintingEntity) entity).getHangingPosition().subtract(startPos);
            } else {
                blockpos = new BlockPos(vec3d);
            }

            entities.add(new Template.EntityInfo(vec3d, blockpos, nbttagcompound));
        }

        return list;
    }

    /**
     * Tweaked version of "addBlocksToWorld" for capsule
     */
    public void spawnBlocksAndEntities(World worldIn, BlockPos pos, PlacementSettings placementIn, Map<BlockPos, Block> occupiedPositions, List<Block> overridableBlocks, List<BlockPos> outSpawnedBlocks, List<Entity> outSpawnedEntities) {
        ITemplateProcessor templateProcessor = new BlockRotationProcessor(pos, placementIn);

        if (size == null) return;

        StructureBoundingBox structureboundingbox = placementIn.getBoundingBox();
        if (!this.blocks.isEmpty() && this.size.getX() >= 1 && this.size.getY() >= 1 && this.size.getZ() >= 1) {
            Block block = placementIn.getReplacedBlock();

            for (Template.BlockInfo template$blockinfo : this.blocks) {
                BlockPos blockpos = transformedBlockPos(placementIn, template$blockinfo.pos).add(pos).add(recenterRotation((size.getX() - 1) / 2, placementIn));
                Template.BlockInfo template$blockinfo1 = templateProcessor.processBlock(worldIn, blockpos, template$blockinfo);

                if (template$blockinfo1 != null) {
                    Block block1 = template$blockinfo1.blockState.getBlock();

                    if ((block == null || block != block1)
                            && (!placementIn.getIgnoreStructureBlock() || block1 != Blocks.STRUCTURE_BLOCK)
                            && (structureboundingbox == null || structureboundingbox.isVecInside(blockpos))
                            // CAPSULE add a condition to prevent replacement of existing content by the capsule content if the world content is not overridable
                            && (!occupiedPositions.containsKey(blockpos) || overridableBlocks.contains(occupiedPositions.get(blockpos)))
                    ) {
                        // CAPSULE capsule addition to allow a rollback in case of error while deploying
                        if (outSpawnedBlocks != null) outSpawnedBlocks.add(blockpos);

                        BlockState iblockstate = template$blockinfo1.blockState.withMirror(placementIn.getMirror());
                        BlockState iblockstate1 = iblockstate.withRotation(placementIn.getRotation());

                        if (template$blockinfo1.tileentityData != null) {
                            TileEntity tileentity = worldIn.getTileEntity(blockpos);

                            if (tileentity != null) {
                                if (tileentity instanceof IInventory) {
                                    ((IInventory) tileentity).clear();
                                }

                                worldIn.setBlockState(blockpos, Blocks.BARRIER.getDefaultState(), 4);
                            }
                        }

                        if (worldIn.setBlockState(blockpos, iblockstate1, 2) && template$blockinfo1.tileentityData != null) {
                            TileEntity tileentity2 = worldIn.getTileEntity(blockpos);

                            if (tileentity2 != null) {
                                template$blockinfo1.tileentityData.putInt("x", blockpos.getX());
                                template$blockinfo1.tileentityData.putInt("y", blockpos.getY());
                                template$blockinfo1.tileentityData.putInt("z", blockpos.getZ());
                                tileentity2.readFromNBT(template$blockinfo1.tileentityData);
                                tileentity2.mirror(placementIn.getMirror());
                                tileentity2.rotate(placementIn.getRotation());
                            }
                        }
                    }
                }
            }

            for (Template.BlockInfo template$blockinfo2 : this.blocks) {
                if (block == null || block != template$blockinfo2.blockState.getBlock()) {
                    BlockPos blockpos1 = transformedBlockPos(placementIn, template$blockinfo2.pos).add(pos).add(recenterRotation((size.getX() - 1) / 2, placementIn));

                    if (structureboundingbox == null || structureboundingbox.isVecInside(blockpos1)) {
                        worldIn.notifyNeighborsRespectDebug(blockpos1, template$blockinfo2.blockState.getBlock(), false);

                        if (template$blockinfo2.tileentityData != null) {
                            TileEntity tileentity1 = worldIn.getTileEntity(blockpos1);

                            if (tileentity1 != null) {
                                tileentity1.markDirty();
                            }
                        }
                    }
                }
            }
        }
        if (!placementIn.getIgnoreEntities()) {
            this.addEntitiesToWorld(worldIn, pos, placementIn.getMirror(), placementIn.getRotation(), structureboundingbox, outSpawnedEntities);
        }
    }


    public void removeBlocks(List<BlockPos> couldNotBeRemoved, BlockPos startPos) {
        for (BlockPos blockPos : couldNotBeRemoved) {
            this.blocks.removeIf(blockInfo -> blockPos.subtract(startPos).equals(blockInfo.pos));
        }
    }

    /**
     * list positions of futur deployment
     */
    public List<BlockPos> calculateDeployPositions(World world, BlockPos blockPos, PlacementSettings placementSettings) {

        ArrayList<BlockPos> out = new ArrayList<>();
        ITemplateProcessor blockRotationProcessor = new BlockRotationProcessor(blockPos, placementSettings);
        if (size == null) return out;

        if (!this.blocks.isEmpty() && this.size.getX() >= 1 && this.size.getY() >= 1 && this.size.getZ() >= 1) {
            Block block = placementSettings.getReplacedBlock();
            StructureBoundingBox structureboundingbox = placementSettings.getBoundingBox();

            for (Template.BlockInfo template$blockinfo : this.blocks) {
                BlockPos blockpos = transformedBlockPos(placementSettings, template$blockinfo.pos).add(blockPos).add(recenterRotation((size.getX() - 1) / 2, placementSettings));
                Template.BlockInfo template$blockinfo1 = blockRotationProcessor.processBlock(world, blockpos, template$blockinfo);

                if (template$blockinfo1 != null) {
                    Block block1 = template$blockinfo1.blockState.getBlock();

                    if (
                            (block == null || block != block1) &&
                                    (!placementSettings.getIgnoreStructureBlock() || block1 != Blocks.STRUCTURE_BLOCK) &&
                                    (structureboundingbox == null || structureboundingbox.isVecInside(blockpos))
                    ) {
                        out.add(blockpos);
                    }
                }
            }
        }
        return out;
    }

    public static BlockPos recenterRotation(int extendSize, PlacementSettings placement) {
        return CapsuleTemplate.transformedBlockPos(placement, new BlockPos(-extendSize, 0, -extendSize)).add(new BlockPos(extendSize, 0, extendSize));
    }

    public static BlockPos recenterRotation(int extendSize, Mirror m, Rotation r) {
        return CapsuleTemplate.transformedBlockPos(new BlockPos(-extendSize, 0, -extendSize), m, r).add(new BlockPos(extendSize, 0, extendSize));
    }

    public boolean canRotate() {
        try {
            for (Template.BlockInfo block : blocks) {
                if (block.tileentityData != null && !Config.blueprintWhitelist.keySet().contains(block.blockState.getBlock().getRegistryName().toString())) {
                    return false;
                }
            }
        } catch (NullPointerException e) {
            return false;
        }
        return true;
    }

    // SCHEMATIC STUFF BELOW

    // inspired by https://github.com/maruohon/worldprimer/blob/master/src/main/java/fi/dy/masa/worldprimer/util/Schematic.java
    // Also for schematic V2 See https://github.com/EngineHub/WorldEdit/blob/master/worldedit-core/src/main/java/com/sk89q/worldedit/extent/clipboard/io/SpongeSchematicReader.java for version 2
    public boolean readSchematic(CompoundNBT nbt) {

        if (!nbt.contains("Blocks", Constants.NBT.TAG_BYTE_ARRAY) ||
                !nbt.contains("Data", Constants.NBT.TAG_BYTE_ARRAY)) {
            LOGGER.error("Schematic: Missing block data in the schematic");
            return false;
        }

        this.blocks.clear();
        this.entities.clear();

        int width = nbt.getShort("Width");
        int height = nbt.getShort("Height");
        int length = nbt.getShort("Length");
        byte[] blockIdsByte = nbt.getByteArray("Blocks");
        byte[] metaArr = nbt.getByteArray("Data");
        final int numBlocks = blockIdsByte.length;
        this.author = "?";

        if (numBlocks != (width * height * length)) {
            LOGGER.error("Schematic: Mismatched block array size compared to the width/height/length, blocks: {}, W x H x L: {} x {} x {}",
                    numBlocks, width, height, length);
            return false;
        }

        if (numBlocks != metaArr.length) {
            LOGGER.error("Schematic: Mismatched block ID and metadata array sizes, blocks: {}, meta: {}", numBlocks, metaArr.length);
            return false;
        }
        Block[] palette = this.readSchematicPalette(nbt);
        if (palette == null || palette.length == 0) {
            LOGGER.error("Schematic: Failed to read the block palette");
            return false;
        }

        // get blocks informations
        BlockState[] blocksById = getSchematicBlocks(nbt, blockIdsByte, metaArr, numBlocks, palette);
        if (blocksById == null) return false;

        // get tile entities
        Map<BlockPos, CompoundNBT> tiles = getSchematicTiles(nbt);

        // get entities
        this.entities.clear();
        ListNBT tagList = nbt.getTagList("Entities", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < tagList.size(); ++i) {
            CompoundNBT entityNBT = tagList.getCompound(i);
            ListNBT posList = entityNBT.getTagList("Pos", Constants.NBT.TAG_DOUBLE);
            Vec3d vec3d = new Vec3d(posList.getDoubleAt(0), posList.getDoubleAt(1), posList.getDoubleAt(2));
            this.entities.add(new Template.EntityInfo(vec3d, new BlockPos(vec3d), entityNBT));
        }

        // calculate block template informations
        int index = 0;
        int sizeX = 1;
        int sizeY = 1;
        int sizeZ = 1;
        for (int y = 0; y < height; ++y) {
            for (int z = 0; z < length; ++z) {
                for (int x = 0; x < width; ++x, index++) {
                    BlockState state = blocksById[index];
                    if (state.getBlock() != Blocks.AIR) {
                        BlockPos pos = new BlockPos(x, y, z);
                        CompoundNBT teNBT = tiles.get(pos);
                        this.blocks.add(new Template.BlockInfo(pos, state, teNBT));
                        if (pos.getX() > sizeX) sizeX = pos.getX();
                        if (pos.getY() > sizeY) sizeY = pos.getY();
                        if (pos.getZ() > sizeZ) sizeZ = pos.getZ();
                    }
                }
            }
        }
        int size = Math.max(sizeX, Math.max(sizeY, sizeZ));
        if (size % 2 == 0) size++;
        this.size = new BlockPos(size, size, size);

        return true;
    }

    private Map<BlockPos, CompoundNBT> getSchematicTiles(CompoundNBT nbt) {
        Map<BlockPos, CompoundNBT> tiles = new HashMap<>();
        ListNBT tagList = nbt.getTagList("TileEntities", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < tagList.size(); ++i) {
            CompoundNBT tag = tagList.getCompound(i);
            BlockPos pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
            tiles.put(pos, tag);
        }
        return tiles;
    }

    @Nullable
    private BlockState[] getSchematicBlocks(CompoundNBT nbt, byte[] blockIdsByte, byte[] metaArr, int numBlocks, Block[] palette) {
        BlockState[] blocksById = new BlockState[numBlocks];
        if (nbt.contains("AddBlocks", Constants.NBT.TAG_BYTE_ARRAY)) {
            byte[] add = nbt.getByteArray("AddBlocks");
            final int expectedAddLength = (int) Math.ceil((double) blockIdsByte.length / 2D);

            if (add.length != expectedAddLength) {
                LOGGER.error("Schematic: Add array size mismatch, blocks: {}, add: {}, expected add: {}",
                        numBlocks, add.length, expectedAddLength);
                return null;
            }

            final int loopMax;

            // Even number of blocks, we can handle two position (meaning one full add byte) at a time
            if ((numBlocks % 2) == 0) {
                loopMax = numBlocks - 1;
            } else {
                loopMax = numBlocks - 2;
            }

            Block block;
            int byteId;
            int bi, ai;

            // Handle two positions per iteration, ie. one full byte of the add array
            for (bi = 0, ai = 0; bi < loopMax; bi += 2, ai++) {
                final int addValue = ((int) add[ai]) & 0xFF;

                byteId = ((int) blockIdsByte[bi]) & 0xFF;
                block = palette[(addValue & 0xF0) << 4 | byteId];
                blocksById[bi] = block.getStateFromMeta(metaArr[bi]);

                byteId = ((int) blockIdsByte[bi + 1]) & 0xFF;
                block = palette[(addValue & 0x0F) << 8 | byteId];
                blocksById[bi + 1] = block.getStateFromMeta(metaArr[bi + 1]);
            }

            // Odd number of blocks, handle the last position
            if ((numBlocks % 2) != 0) {
                final int addValue = ((int) add[ai]) & 0xFF;
                byteId = ((int) blockIdsByte[bi]) & 0xFF;
                block = palette[(addValue & 0xF0) << 4 | byteId];
                blocksById[bi] = block.getStateFromMeta(metaArr[bi]);
            }
        }
        // Old Schematica format
        else if (nbt.contains("Add", Constants.NBT.TAG_BYTE_ARRAY)) {
            LOGGER.error("Schematic: Old Schematica format detected, not implemented");
            return null;
        }
        // V2 Schematica format
        else if (nbt.contains("Version", Constants.NBT.TAG_INT)) {
            LOGGER.error("Schematic: Newer Schematica format {} detected, not implemented", nbt.getInt("Version"));
            return null;
        }
        // No palette, use the registry IDs directly
        else {
            for (int i = 0; i < numBlocks; i++) {
                Block block = palette[((int) blockIdsByte[i]) & 0xFF];
                blocksById[i] = block.getStateFromMeta(metaArr[i]);
            }
        }
        return blocksById;
    }

    @Nullable
    private Block[] readSchematicPalette(CompoundNBT nbt) {
        final Block air = Blocks.AIR;
        Block[] palette = new Block[4096];
        Arrays.fill(palette, air);

        // Schematica palette
        if (nbt.contains("SchematicaMapping", Constants.NBT.TAG_COMPOUND)) {
            CompoundNBT tag = nbt.getCompound("SchematicaMapping");
            Set<String> keys = tag.getKeySet();

            for (String key : keys) {
                int id = tag.getShort(key);

                if (id >= palette.length) {
                    LOGGER.error("Schematic: Invalid ID '{}' in SchematicaMapping for block '{}', max = 4095", id, key);
                    return null;
                }

                Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(key));

                if (block != null) {
                    palette[id] = block;
                } else {
                    LOGGER.error("Schematic: Missing/non-existing block '{}' in SchematicaMapping", key);
                }
            }
        }
        // MCEdit2 palette
        else if (nbt.contains("BlockIDs", Constants.NBT.TAG_COMPOUND)) {
            CompoundNBT tag = nbt.getCompound("BlockIDs");
            Set<String> keys = tag.getKeySet();

            for (String idStr : keys) {
                String key = tag.getString(idStr);
                int id;

                try {
                    id = Integer.parseInt(idStr);
                } catch (NumberFormatException e) {
                    LOGGER.error("Schematic: Invalid ID '{}' (not a number) in MCEdit2 palette for block '{}'", idStr, key);
                    continue;
                }

                if (id >= palette.length) {
                    LOGGER.error("Schematic: Invalid ID '{}' in MCEdit2 palette for block '{}', max = 4095", id, key);
                    return null;
                }

                Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(key));

                if (block != null) {
                    palette[id] = block;
                } else {
                    LOGGER.error("Schematic: Missing/non-existing block '{}' in MCEdit2 palette", key);
                }
            }
        }
        // No palette, use the current registry IDs directly
        else {
            for (Block block : ForgeRegistries.BLOCKS.getValuesCollection()) {
                if (block != null) {
                    int id = Block.getIdFromBlock(block);

                    if (id >= 0 && id < palette.length) {
                        palette[id] = block;
                    } else {
                        LOGGER.error("Schematic: Invalid ID {} for block '{}' from the registry", id, block.getRegistryName());
                    }
                }
            }
        }

        return palette;
    }

}