package capsule.structure;

import capsule.Config;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ILiquidContainer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.item.PaintingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.IFluidState;
import net.minecraft.inventory.IClearable;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.shapes.BitSetVoxelShapePart;
import net.minecraft.util.math.shapes.VoxelShapePart;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Copy of mc original net.minecraft.world.gen.feature.template class, but having fields public to allow external manipulation.
 * Also include some specificities to capsules
 *
 * @author Lythom
 */
public class CapsuleTemplate {
    protected static final Logger LOGGER = LogManager.getLogger(CapsuleTemplate.class);

    public final List<List<Template.BlockInfo>> blocks = Lists.newArrayList();

    public final List<Template.BlockInfo> getBlocks() {
        if (blocks.size() <= 0) return Lists.newArrayList();
        return blocks.get(0);
    }

    public final List<Template.EntityInfo> entities = Lists.newArrayList();
    public Map<BlockPos, Block> occupiedPositions = null;

    public BlockPos size = BlockPos.ZERO;
    private String author = "?";

    public BlockPos getSize() {
        return this.size;
    }

    public void setAuthor(String authorIn) {
        this.author = authorIn;
    }

    public String getAuthor() {
        return this.author;
    }

    /**
     * takes blocks from the world and puts the data them into this template
     */
    public void takeBlocksFromWorld(World worldIn, BlockPos startPos, BlockPos size, boolean takeEntities, @Nullable Block toIgnore) {
        if (size.getX() >= 1 && size.getY() >= 1 && size.getZ() >= 1) {
            BlockPos blockpos = startPos.add(size).add(-1, -1, -1);
            List<Template.BlockInfo> list = Lists.newArrayList();
            List<Template.BlockInfo> list1 = Lists.newArrayList();
            List<Template.BlockInfo> list2 = Lists.newArrayList();
            BlockPos blockpos1 = new BlockPos(Math.min(startPos.getX(), blockpos.getX()), Math.min(startPos.getY(), blockpos.getY()), Math.min(startPos.getZ(), blockpos.getZ()));
            BlockPos blockpos2 = new BlockPos(Math.max(startPos.getX(), blockpos.getX()), Math.max(startPos.getY(), blockpos.getY()), Math.max(startPos.getZ(), blockpos.getZ()));
            this.size = size;

            for (BlockPos blockpos3 : BlockPos.getAllInBoxMutable(blockpos1, blockpos2)) {
                BlockPos blockpos4 = blockpos3.subtract(blockpos1);
                BlockState blockstate = worldIn.getBlockState(blockpos3);
                if (toIgnore == null || toIgnore != blockstate.getBlock()) {
                    TileEntity tileentity = worldIn.getTileEntity(blockpos3);
                    if (tileentity != null) {
                        CompoundNBT compoundnbt = tileentity.write(new CompoundNBT());
                        compoundnbt.remove("x");
                        compoundnbt.remove("y");
                        compoundnbt.remove("z");
                        list1.add(new Template.BlockInfo(blockpos4, blockstate, compoundnbt));
                    } else if (!blockstate.isOpaqueCube(worldIn, blockpos3) && !blockstate.isCollisionShapeOpaque(worldIn, blockpos3)) {
                        list2.add(new Template.BlockInfo(blockpos4, blockstate, null));
                    } else {
                        list.add(new Template.BlockInfo(blockpos4, blockstate, null));
                    }
                }
            }

            List<Template.BlockInfo> list3 = Lists.newArrayList();
            list3.addAll(list);
            list3.addAll(list1);
            list3.addAll(list2);
            this.blocks.clear();
            this.blocks.add(list3);
            if (takeEntities) {
                this.takeEntitiesFromWorld(worldIn, blockpos1, blockpos2.add(1, 1, 1));
            } else {
                this.entities.clear();
            }

        }
    }

    /**
     * takes blocks from the world and puts the data them into this template
     */
    private void takeEntitiesFromWorld(World worldIn, BlockPos startPos, BlockPos endPos) {
        List<Entity> list = worldIn.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(startPos, endPos), (p_201048_0_) -> {
            return !(p_201048_0_ instanceof PlayerEntity);
        });
        this.entities.clear();

        for (Entity entity : list) {
            Vec3d vec3d = new Vec3d(entity.getPosX() - (double) startPos.getX(), entity.getPosY() - (double) startPos.getY(), entity.getPosZ() - (double) startPos.getZ());
            CompoundNBT compoundnbt = new CompoundNBT();
            entity.writeUnlessPassenger(compoundnbt);
            BlockPos blockpos;
            if (entity instanceof PaintingEntity) {
                blockpos = ((PaintingEntity) entity).getHangingPosition().subtract(startPos);
            } else {
                blockpos = new BlockPos(vec3d);
            }

            this.entities.add(new Template.EntityInfo(vec3d, blockpos, compoundnbt));
        }

    }

    public List<Template.BlockInfo> func_215381_a(BlockPos p_215381_1_, PlacementSettings p_215381_2_, Block p_215381_3_) {
        return this.func_215386_a(p_215381_1_, p_215381_2_, p_215381_3_, true);
    }

    public List<Template.BlockInfo> func_215386_a(BlockPos p_215386_1_, PlacementSettings p_215386_2_, Block p_215386_3_, boolean p_215386_4_) {
        List<Template.BlockInfo> list = Lists.newArrayList();
        MutableBoundingBox mutableboundingbox = p_215386_2_.getBoundingBox();

        for (Template.BlockInfo template$blockinfo : p_215386_2_.func_227459_a_(this.blocks, p_215386_1_)) {
            BlockPos blockpos = p_215386_4_ ? transformedBlockPos(p_215386_2_, template$blockinfo.pos).add(p_215386_1_) : template$blockinfo.pos;
            if (mutableboundingbox == null || mutableboundingbox.isVecInside(blockpos)) {
                BlockState blockstate = template$blockinfo.state;
                if (blockstate.getBlock() == p_215386_3_) {
                    list.add(new Template.BlockInfo(blockpos, blockstate.rotate(p_215386_2_.getRotation()), template$blockinfo.nbt));
                }
            }
        }

        return list;
    }

    public BlockPos calculateConnectedPos(PlacementSettings placementIn, BlockPos p_186262_2_, PlacementSettings p_186262_3_, BlockPos p_186262_4_) {
        BlockPos blockpos = transformedBlockPos(placementIn, p_186262_2_);
        BlockPos blockpos1 = transformedBlockPos(p_186262_3_, p_186262_4_);
        return blockpos.subtract(blockpos1);
    }

    public static BlockPos transformedBlockPos(PlacementSettings placementIn, BlockPos pos) {
        return getTransformedPos(pos, placementIn.getMirror(), placementIn.getRotation(), placementIn.getCenterOffset());
    }

    // FORGE: Add overload accepting Vec3d
    public static Vec3d transformedVec3d(PlacementSettings placementIn, Vec3d pos) {
        return getTransformedPos(pos, placementIn.getMirror(), placementIn.getRotation(), placementIn.getCenterOffset());
    }

    public static void func_222857_a(IWorld worldIn, int p_222857_1_, VoxelShapePart voxelShapePartIn, int xIn, int yIn, int zIn) {
        voxelShapePartIn.forEachFace((p_222856_5_, p_222856_6_, p_222856_7_, p_222856_8_) -> {
            BlockPos blockpos = new BlockPos(xIn + p_222856_6_, yIn + p_222856_7_, zIn + p_222856_8_);
            BlockPos blockpos1 = blockpos.offset(p_222856_5_);
            BlockState blockstate = worldIn.getBlockState(blockpos);
            BlockState blockstate1 = worldIn.getBlockState(blockpos1);
            BlockState blockstate2 = blockstate.updatePostPlacement(p_222856_5_, blockstate1, worldIn, blockpos, blockpos1);
            if (blockstate != blockstate2) {
                worldIn.setBlockState(blockpos, blockstate2, p_222857_1_ & -2 | 16);
            }

            BlockState blockstate3 = blockstate1.updatePostPlacement(p_222856_5_.getOpposite(), blockstate2, worldIn, blockpos1, blockpos);
            if (blockstate1 != blockstate3) {
                worldIn.setBlockState(blockpos1, blockstate3, p_222857_1_ & -2 | 16);
            }

        });
    }

    @Deprecated // FORGE: Add template parameter
    public static List<Template.BlockInfo> processBlockInfos(IWorld worldIn, BlockPos offsetPos, PlacementSettings placementSettingsIn, List<Template.BlockInfo> blockInfos) {
        return processBlockInfos(null, worldIn, offsetPos, placementSettingsIn, blockInfos);
    }

    public static List<Template.BlockInfo> processBlockInfos(@Nullable CapsuleTemplate template, IWorld worldIn, BlockPos offsetPos, PlacementSettings placementSettingsIn, List<Template.BlockInfo> blockInfos) {
        List<Template.BlockInfo> list = Lists.newArrayList();

        for (Template.BlockInfo template$blockinfo : blockInfos) {
            BlockPos blockpos = transformedBlockPos(placementSettingsIn, template$blockinfo.pos).add(offsetPos);
            Template.BlockInfo template$blockinfo1 = new Template.BlockInfo(blockpos, template$blockinfo.state, template$blockinfo.nbt);
            list.add(template$blockinfo1);
        }

        return list;
    }

    // FORGE: Add processing for entities
    public static List<Template.EntityInfo> processEntityInfos(@Nullable CapsuleTemplate template, IWorld worldIn, BlockPos offsetPos, PlacementSettings placementSettingsIn, List<Template.EntityInfo> blockInfos) {
        List<Template.EntityInfo> list = Lists.newArrayList();

        for (Template.EntityInfo entityInfo : blockInfos) {
            Vec3d pos = transformedVec3d(placementSettingsIn, entityInfo.pos).add(new Vec3d(offsetPos));
            BlockPos blockpos = transformedBlockPos(placementSettingsIn, entityInfo.blockPos).add(offsetPos);
            Template.EntityInfo info = new Template.EntityInfo(pos, blockpos, entityInfo.nbt);
            list.add(info);
        }

        return list;
    }

    private void addEntitiesToWorld(IWorld worldIn, BlockPos offsetPos, PlacementSettings placementIn, Mirror mirrorIn, Rotation rotationIn, BlockPos centerOffset, @Nullable MutableBoundingBox boundsIn, List<Entity> spawnedEntities) {
        for (Template.EntityInfo template$entityinfo : processEntityInfos(this, worldIn, offsetPos, placementIn, this.entities)) {
            BlockPos blockpos = getTransformedPos(template$entityinfo.blockPos, mirrorIn, rotationIn, centerOffset).add(offsetPos);
            blockpos = template$entityinfo.blockPos; // FORGE: Position will have already been transformed by processEntityInfos
            if (boundsIn == null || boundsIn.isVecInside(blockpos)) {
                CompoundNBT compoundnbt = template$entityinfo.nbt;
                Vec3d vec3d = getTransformedPos(template$entityinfo.pos, mirrorIn, rotationIn, centerOffset);
                vec3d = vec3d.add(offsetPos.getX(), offsetPos.getY(), offsetPos.getZ());
                Vec3d vec3d1 = template$entityinfo.pos; // FORGE: Position will have already been transformed by processEntityInfos
                ListNBT listnbt = new ListNBT();
                listnbt.add(DoubleNBT.valueOf(vec3d1.x));
                listnbt.add(DoubleNBT.valueOf(vec3d1.y));
                listnbt.add(DoubleNBT.valueOf(vec3d1.z));
                compoundnbt.put("Pos", listnbt);
                compoundnbt.remove("UUIDMost");
                compoundnbt.remove("UUIDLeast");
                loadEntity(worldIn, compoundnbt).ifPresent((p_215383_4_) -> {
                    float f = p_215383_4_.getMirroredYaw(mirrorIn);
                    f = f + (p_215383_4_.rotationYaw - p_215383_4_.getRotatedYaw(rotationIn));
                    p_215383_4_.setLocationAndAngles(vec3d1.x, vec3d1.y, vec3d1.z, f, p_215383_4_.rotationPitch);
                    worldIn.addEntity(p_215383_4_);
                    if (spawnedEntities != null) spawnedEntities.add(p_215383_4_);
                });
            }
        }

    }

    private static Optional<Entity> loadEntity(IWorld worldIn, CompoundNBT nbt) {
        try {
            return EntityType.loadEntityUnchecked(nbt, worldIn.getWorld());
        } catch (Exception var3) {
            return Optional.empty();
        }
    }

    public BlockPos transformedSize(Rotation rotationIn) {
        switch (rotationIn) {
            case COUNTERCLOCKWISE_90:
            case CLOCKWISE_90:
                return new BlockPos(this.size.getZ(), this.size.getY(), this.size.getX());
            default:
                return this.size;
        }
    }

    public static BlockPos getTransformedPos(BlockPos targetPos, Mirror mirrorIn, Rotation rotationIn, BlockPos offset) {
        int i = targetPos.getX();
        int j = targetPos.getY();
        int k = targetPos.getZ();
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

        int l = offset.getX();
        int i1 = offset.getZ();
        switch (rotationIn) {
            case COUNTERCLOCKWISE_90:
                return new BlockPos(l - i1 + k, j, l + i1 - i);
            case CLOCKWISE_90:
                return new BlockPos(l + i1 - k, j, i1 - l + i);
            case CLOCKWISE_180:
                return new BlockPos(l + l - i, j, i1 + i1 - k);
            default:
                return flag ? new BlockPos(i, j, k) : targetPos;
        }
    }

    private static Vec3d getTransformedPos(Vec3d target, Mirror mirrorIn, Rotation rotationIn, BlockPos centerOffset) {
        double d0 = target.x;
        double d1 = target.y;
        double d2 = target.z;
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

        int i = centerOffset.getX();
        int j = centerOffset.getZ();
        switch (rotationIn) {
            case COUNTERCLOCKWISE_90:
                return new Vec3d((double) (i - j) + d2, d1, (double) (i + j + 1) - d0);
            case CLOCKWISE_90:
                return new Vec3d((double) (i + j + 1) - d2, d1, (double) (j - i) + d0);
            case CLOCKWISE_180:
                return new Vec3d((double) (i + i + 1) - d0, d1, (double) (j + j + 1) - d2);
            default:
                return flag ? new Vec3d(d0, d1, d2) : target;
        }
    }

    public BlockPos getZeroPositionWithTransform(BlockPos p_189961_1_, Mirror p_189961_2_, Rotation p_189961_3_) {
        return getZeroPositionWithTransform(p_189961_1_, p_189961_2_, p_189961_3_, this.getSize().getX(), this.getSize().getZ());
    }

    public static BlockPos getZeroPositionWithTransform(BlockPos p_191157_0_, Mirror p_191157_1_, Rotation p_191157_2_, int p_191157_3_, int p_191157_4_) {
        --p_191157_3_;
        --p_191157_4_;
        int i = p_191157_1_ == Mirror.FRONT_BACK ? p_191157_3_ : 0;
        int j = p_191157_1_ == Mirror.LEFT_RIGHT ? p_191157_4_ : 0;
        BlockPos blockpos = p_191157_0_;
        switch (p_191157_2_) {
            case COUNTERCLOCKWISE_90:
                blockpos = p_191157_0_.add(j, 0, p_191157_3_ - i);
                break;
            case CLOCKWISE_90:
                blockpos = p_191157_0_.add(p_191157_4_ - j, 0, i);
                break;
            case CLOCKWISE_180:
                blockpos = p_191157_0_.add(p_191157_3_ - i, 0, p_191157_4_ - j);
                break;
            case NONE:
                blockpos = p_191157_0_.add(i, 0, j);
        }

        return blockpos;
    }

    public MutableBoundingBox getMutableBoundingBox(PlacementSettings p_215388_1_, BlockPos p_215388_2_) {
        Rotation rotation = p_215388_1_.getRotation();
        BlockPos blockpos = p_215388_1_.getCenterOffset();
        BlockPos blockpos1 = this.transformedSize(rotation);
        Mirror mirror = p_215388_1_.getMirror();
        int i = blockpos.getX();
        int j = blockpos.getZ();
        int k = blockpos1.getX() - 1;
        int l = blockpos1.getY() - 1;
        int i1 = blockpos1.getZ() - 1;
        MutableBoundingBox mutableboundingbox = new MutableBoundingBox(0, 0, 0, 0, 0, 0);
        switch (rotation) {
            case COUNTERCLOCKWISE_90:
                mutableboundingbox = new MutableBoundingBox(i - j, 0, i + j - i1, i - j + k, l, i + j);
                break;
            case CLOCKWISE_90:
                mutableboundingbox = new MutableBoundingBox(i + j - k, 0, j - i, i + j, l, j - i + i1);
                break;
            case CLOCKWISE_180:
                mutableboundingbox = new MutableBoundingBox(i + i - k, 0, j + j - i1, i + i, l, j + j);
                break;
            case NONE:
                mutableboundingbox = new MutableBoundingBox(0, 0, 0, k, l, i1);
        }

        switch (mirror) {
            case LEFT_RIGHT:
                this.func_215385_a(rotation, i1, k, mutableboundingbox, Direction.NORTH, Direction.SOUTH);
                break;
            case FRONT_BACK:
                this.func_215385_a(rotation, k, i1, mutableboundingbox, Direction.WEST, Direction.EAST);
            case NONE:
        }

        mutableboundingbox.offset(p_215388_2_.getX(), p_215388_2_.getY(), p_215388_2_.getZ());
        return mutableboundingbox;
    }

    private void func_215385_a(Rotation rotationIn, int offsetFront, int p_215385_3_, MutableBoundingBox p_215385_4_, Direction p_215385_5_, Direction p_215385_6_) {
        BlockPos blockpos = BlockPos.ZERO;
        if (rotationIn != Rotation.CLOCKWISE_90 && rotationIn != Rotation.COUNTERCLOCKWISE_90) {
            if (rotationIn == Rotation.CLOCKWISE_180) {
                blockpos = blockpos.offset(p_215385_6_, offsetFront);
            } else {
                blockpos = blockpos.offset(p_215385_5_, offsetFront);
            }
        } else {
            blockpos = blockpos.offset(rotationIn.rotate(p_215385_5_), p_215385_3_);
        }

        p_215385_4_.offset(blockpos.getX(), 0, blockpos.getZ());
    }

    public CompoundNBT writeToNBT(CompoundNBT nbt) {
        if (this.blocks.isEmpty()) {
            nbt.put("blocks", new ListNBT());
            nbt.put("palette", new ListNBT());
        } else {
            List<CapsuleTemplate.BasicPalette> list = Lists.newArrayList();
            CapsuleTemplate.BasicPalette template$basicpalette = new CapsuleTemplate.BasicPalette();
            list.add(template$basicpalette);

            for (int i = 1; i < this.blocks.size(); ++i) {
                list.add(new CapsuleTemplate.BasicPalette());
            }

            ListNBT listnbt1 = new ListNBT();
            List<Template.BlockInfo> list1 = this.getBlocks();

            for (int j = 0; j < list1.size(); ++j) {
                Template.BlockInfo template$blockinfo = list1.get(j);
                CompoundNBT compoundnbt = new CompoundNBT();
                compoundnbt.put("pos", this.writeInts(template$blockinfo.pos.getX(), template$blockinfo.pos.getY(), template$blockinfo.pos.getZ()));
                int k = template$basicpalette.idFor(template$blockinfo.state);
                compoundnbt.putInt("state", k);
                if (template$blockinfo.nbt != null) {
                    compoundnbt.put("nbt", template$blockinfo.nbt);
                }

                listnbt1.add(compoundnbt);

                for (int l = 1; l < this.blocks.size(); ++l) {
                    CapsuleTemplate.BasicPalette template$basicpalette1 = list.get(l);
                    template$basicpalette1.addMapping((this.blocks.get(l).get(j)).state, k);
                }
            }

            nbt.put("blocks", listnbt1);
            if (list.size() == 1) {
                ListNBT listnbt2 = new ListNBT();

                for (BlockState blockstate : template$basicpalette) {
                    listnbt2.add(NBTUtil.writeBlockState(blockstate));
                }

                nbt.put("palette", listnbt2);
            } else {
                ListNBT listnbt3 = new ListNBT();

                for (CapsuleTemplate.BasicPalette template$basicpalette2 : list) {
                    ListNBT listnbt4 = new ListNBT();

                    for (BlockState blockstate1 : template$basicpalette2) {
                        listnbt4.add(NBTUtil.writeBlockState(blockstate1));
                    }

                    listnbt3.add(listnbt4);
                }

                nbt.put("palettes", listnbt3);
            }
        }

        ListNBT listnbt = new ListNBT();

        for (Template.EntityInfo template$entityinfo : this.entities) {
            CompoundNBT compoundnbt1 = new CompoundNBT();
            compoundnbt1.put("pos", this.writeDoubles(template$entityinfo.pos.x, template$entityinfo.pos.y, template$entityinfo.pos.z));
            compoundnbt1.put("blockPos", this.writeInts(template$entityinfo.blockPos.getX(), template$entityinfo.blockPos.getY(), template$entityinfo.blockPos.getZ()));
            if (template$entityinfo.nbt != null) {
                compoundnbt1.put("nbt", template$entityinfo.nbt);
            }

            listnbt.add(compoundnbt1);
        }

        nbt.put("entities", listnbt);
        nbt.put("size", this.writeInts(this.size.getX(), this.size.getY(), this.size.getZ()));
        nbt.putInt("DataVersion", SharedConstants.getVersion().getWorldVersion());

        // CAPSULE save already occupied positions when deployed
        ListNBT occupiedSpawnPositionstaglist = new ListNBT();
        if (this.occupiedPositions != null) {
            for (Map.Entry<BlockPos, Block> entry : occupiedPositions.entrySet()) {
                CompoundNBT nbtEntry = new CompoundNBT();
                nbtEntry.putLong("pos", entry.getKey().toLong());
                nbtEntry.putInt("blockId", Block.getStateId(entry.getValue().getDefaultState()));
                occupiedSpawnPositionstaglist.add(nbtEntry);
            }
            nbt.put("capsule_occupiedSources", occupiedSpawnPositionstaglist);
        }
        return nbt;
    }

    public void read(CompoundNBT compound) {
        this.blocks.clear();
        this.entities.clear();
        ListNBT listnbt = compound.getList("size", 3);
        this.size = new BlockPos(listnbt.getInt(0), listnbt.getInt(1), listnbt.getInt(2));
        ListNBT listnbt1 = compound.getList("blocks", 10);
        if (compound.contains("palettes", 9)) {
            ListNBT listnbt2 = compound.getList("palettes", 9);

            for (int i = 0; i < listnbt2.size(); ++i) {
                this.readPalletesAndBlocks(listnbt2.getList(i), listnbt1);
            }
        } else {
            this.readPalletesAndBlocks(compound.getList("palette", 10), listnbt1);
        }

        ListNBT listnbt5 = compound.getList("entities", 10);

        for (int j = 0; j < listnbt5.size(); ++j) {
            CompoundNBT compoundnbt = listnbt5.getCompound(j);
            ListNBT listnbt3 = compoundnbt.getList("pos", 6);
            Vec3d vec3d = new Vec3d(listnbt3.getDouble(0), listnbt3.getDouble(1), listnbt3.getDouble(2));
            ListNBT listnbt4 = compoundnbt.getList("blockPos", 3);
            BlockPos blockpos = new BlockPos(listnbt4.getInt(0), listnbt4.getInt(1), listnbt4.getInt(2));
            if (compoundnbt.contains("nbt")) {
                CompoundNBT compoundnbt1 = compoundnbt.getCompound("nbt");
                this.entities.add(new Template.EntityInfo(vec3d, blockpos, compoundnbt1));
            }
        }

        // CAPSULE read already occupied positions when deployed
        if (compound.contains("capsule_occupiedSources")) {
            Map<BlockPos, Block> occupiedSources = new HashMap<>();
            ListNBT list = compound.getList("capsule_occupiedSources", 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundNBT entry = list.getCompound(i);
                occupiedSources.put(BlockPos.fromLong(entry.getLong("pos")), Block.getStateById(entry.getInt("blockId")).getBlock());
            }
            this.occupiedPositions = occupiedSources;
        }
    }

    private void readPalletesAndBlocks(ListNBT palletesNBT, ListNBT blocksNBT) {
        CapsuleTemplate.BasicPalette template$basicpalette = new CapsuleTemplate.BasicPalette();
        List<Template.BlockInfo> list = Lists.newArrayList();

        for (int i = 0; i < palletesNBT.size(); ++i) {
            template$basicpalette.addMapping(NBTUtil.readBlockState(palletesNBT.getCompound(i)), i);
        }

        for (int j = 0; j < blocksNBT.size(); ++j) {
            CompoundNBT compoundnbt = blocksNBT.getCompound(j);
            ListNBT listnbt = compoundnbt.getList("pos", 3);
            BlockPos blockpos = new BlockPos(listnbt.getInt(0), listnbt.getInt(1), listnbt.getInt(2));
            BlockState blockstate = template$basicpalette.stateFor(compoundnbt.getInt("state"));
            CompoundNBT compoundnbt1;
            if (compoundnbt.contains("nbt")) {
                compoundnbt1 = compoundnbt.getCompound("nbt");
            } else {
                compoundnbt1 = null;
            }

            list.add(new Template.BlockInfo(blockpos, blockstate, compoundnbt1));
        }

        list.sort(Comparator.comparingInt((p_215384_0_) -> {
            return p_215384_0_.pos.getY();
        }));
        this.blocks.add(list);
    }

    private ListNBT writeInts(int... values) {
        ListNBT listnbt = new ListNBT();

        for (int i : values) {
            listnbt.add(IntNBT.valueOf(i));
        }

        return listnbt;
    }

    private ListNBT writeDoubles(double... values) {
        ListNBT listnbt = new ListNBT();

        for (double d0 : values) {
            listnbt.add(DoubleNBT.valueOf(d0));
        }

        return listnbt;
    }

    public void filterFromWhitelist(HashMap<String, JsonObject> blueprintWhitelist, List<String> outExcluded) {
        List<Template.BlockInfo> newBlockList = this.getBlocks().stream()
                .filter(b -> {
                    ResourceLocation registryName = b.state.getBlock().getRegistryName();
                    boolean included = b.nbt == null
                            || registryName != null && Config.blueprintWhitelist.containsKey(registryName.toString());
                    if (!included && outExcluded != null) outExcluded.add(b.state.toString());
                    return included;
                })
                .map(b -> {
                    if (b.nbt == null) return b;
                    // remove all unlisted nbt data to prevent dupe or cheating
                    CompoundNBT nbt = null;
                    JsonObject allowedNBT = Config.getBlueprintAllowedNBT(b.state.getBlock());
                    if (allowedNBT != null) {
                        nbt = b.nbt.copy();
                        nbt.keySet().removeIf(key -> !allowedNBT.has(key));
                    } else {
                        nbt = new CompoundNBT();
                    }
                    return new Template.BlockInfo(
                            b.pos,
                            b.state,
                            nbt
                    );
                }).collect(Collectors.toList());
        getBlocks().clear();
        getBlocks().addAll(newBlockList);
        // remove all entities
        entities.clear();
    }

    static class BasicPalette implements Iterable<BlockState> {
        public static final BlockState DEFAULT_BLOCK_STATE = Blocks.AIR.getDefaultState();
        private final ObjectIntIdentityMap<BlockState> ids = new ObjectIntIdentityMap<>(16);
        private int lastId;

        private BasicPalette() {
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
            BlockState blockstate = this.ids.getByValue(id);
            return blockstate == null ? DEFAULT_BLOCK_STATE : blockstate;
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
            BlockPos blockpos1 = new BlockPos(Math.min(startPos.getX(), blockpos.getX()), Math.min(startPos.getY(), blockpos.getY()), Math.min(startPos.getZ(), blockpos.getZ()));
            BlockPos blockpos2 = new BlockPos(Math.max(startPos.getX(), blockpos.getX()), Math.max(startPos.getY(), blockpos.getY()), Math.max(startPos.getZ(), blockpos.getZ()));
            this.size = endPos;

            for (BlockPos blockpos3 : BlockPos.getAllInBoxMutable(blockpos1, blockpos2)) {
                BlockPos blockpos4 = blockpos3.subtract(blockpos1);
                BlockState blockstate = worldIn.getBlockState(blockpos3);
                if (!excluded.contains(blockstate.getBlock()) // excluded blocks are not captured at all
                        && (occupiedPositionsToIgnore == null // exclude sourceBlock that were already presents. Capture only if it was changed.
                        || !(occupiedPositionsToIgnore.containsKey(blockpos3)
                        && occupiedPositionsToIgnore.get(blockpos3).equals(blockstate.getBlock())))) {

                    TileEntity tileentity = worldIn.getTileEntity(blockpos3);
                    if (tileentity != null) {
                        CompoundNBT compoundnbt = tileentity.write(new CompoundNBT());
                        compoundnbt.remove("x");
                        compoundnbt.remove("y");
                        compoundnbt.remove("z");
                        list1.add(new Template.BlockInfo(blockpos4, blockstate, compoundnbt));
                    } else if (!blockstate.isOpaqueCube(worldIn, blockpos3) && !blockstate.isCollisionShapeOpaque(worldIn, blockpos3)) {
                        list2.add(new Template.BlockInfo(blockpos4, blockstate, null));
                    } else {
                        list.add(new Template.BlockInfo(blockpos4, blockstate, null));
                    }
                    // save a copy
                    transferedBlocks.add(new BlockPos(blockpos3.getX(), blockpos3.getY(), blockpos3.getZ()));
                }
            }

            List<Template.BlockInfo> list3 = Lists.newArrayList();
            list3.addAll(list);
            list3.addAll(list1);
            list3.addAll(list2);
            this.blocks.clear();
            this.blocks.add(list3);

            List<Entity> capturedEntities = this.snapshotNonLivingEntitiesFromWorld(worldIn, blockpos1, blockpos2.add(1, 1, 1));
            if (outCapturedEntities != null && capturedEntities != null) {
                outCapturedEntities.addAll(capturedEntities);
            }
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
            Vec3d vec3d = new Vec3d(entity.getPosX() - (double) startPos.getX(), entity.getPosY() - (double) startPos.getY(), entity.getPosZ() - (double) startPos.getZ());
            CompoundNBT compoundnbt = new CompoundNBT();
            entity.writeUnlessPassenger(compoundnbt);
            BlockPos blockpos;
            if (entity instanceof PaintingEntity) {
                blockpos = ((PaintingEntity) entity).getHangingPosition().subtract(startPos);
            } else {
                blockpos = new BlockPos(vec3d);
            }

            this.entities.add(new Template.EntityInfo(vec3d, blockpos, compoundnbt));
        }

        return list;
    }

    public static AxisAlignedBB transformedAxisAlignedBB(PlacementSettings placementIn, AxisAlignedBB bb) {
        return new AxisAlignedBB(
                transformedBlockPos(placementIn, new BlockPos(bb.minX, bb.minY, bb.minZ)),
                transformedBlockPos(placementIn, new BlockPos(bb.maxX, bb.maxY, bb.maxZ))
        );
    }

    /**
     * Tweaked version of "addBlocksToWorld" for capsule
     */
    public boolean spawnBlocksAndEntities(World worldIn,
                                          BlockPos pos,
                                          PlacementSettings placementIn,
                                          Map<BlockPos, Block> occupiedPositions,
                                          List<Block> overridableBlocks,
                                          List<BlockPos> outSpawnedBlocks,
                                          List<Entity> outSpawnedEntities) {
        int flags = 2; /** @see net.minecraft.world.World#setBlockState(BlockPos, BlockState, int) */
        if (this.blocks.isEmpty()) {
            return false;
        } else {
            List<Template.BlockInfo> list = placementIn.func_227459_a_(this.blocks, pos);
            if ((!list.isEmpty() || !placementIn.getIgnoreEntities() && !this.entities.isEmpty()) && this.size.getX() >= 1 && this.size.getY() >= 1 && this.size.getZ() >= 1) {
                MutableBoundingBox mutableboundingbox = placementIn.getBoundingBox();
                List<BlockPos> list1 = Lists.newArrayListWithCapacity(placementIn.func_204763_l() ? list.size() : 0);
                List<Pair<BlockPos, CompoundNBT>> list2 = Lists.newArrayListWithCapacity(list.size());
                int i = Integer.MAX_VALUE;
                int j = Integer.MAX_VALUE;
                int k = Integer.MAX_VALUE;
                int l = Integer.MIN_VALUE;
                int i1 = Integer.MIN_VALUE;
                int j1 = Integer.MIN_VALUE;

                for (Template.BlockInfo template$blockinfo : processBlockInfos(this, worldIn, pos, placementIn, list)) {
                    // CAPSULE center the structure
                    BlockPos blockpos = template$blockinfo.pos.add(recenterRotation((size.getX() - 1) / 2, placementIn));
                    if ((mutableboundingbox == null || mutableboundingbox.isVecInside(blockpos))
                            // CAPSULE add a condition to prevent replacement of existing content by the capsule content if the world content is not overridable
                            && (!occupiedPositions.containsKey(blockpos) || overridableBlocks.contains(occupiedPositions.get(blockpos)))) {
                        // CAPSULE capsule addition to allow a rollback in case of error while deploying
                        if (outSpawnedBlocks != null) outSpawnedBlocks.add(blockpos);

                        IFluidState ifluidstate = placementIn.func_204763_l() ? worldIn.getFluidState(blockpos) : null;
                        BlockState blockstate = template$blockinfo.state.mirror(placementIn.getMirror()).rotate(placementIn.getRotation());
                        if (template$blockinfo.nbt != null) {
                            TileEntity tileentity = worldIn.getTileEntity(blockpos);
                            IClearable.clearObj(tileentity);
                            worldIn.setBlockState(blockpos, Blocks.BARRIER.getDefaultState(), 20);
                        }

                        if (worldIn.setBlockState(blockpos, blockstate, flags)) {
                            i = Math.min(i, blockpos.getX());
                            j = Math.min(j, blockpos.getY());
                            k = Math.min(k, blockpos.getZ());
                            l = Math.max(l, blockpos.getX());
                            i1 = Math.max(i1, blockpos.getY());
                            j1 = Math.max(j1, blockpos.getZ());
                            list2.add(Pair.of(blockpos, template$blockinfo.nbt));
                            if (template$blockinfo.nbt != null) {
                                TileEntity tileentity1 = worldIn.getTileEntity(blockpos);
                                if (tileentity1 != null) {
                                    template$blockinfo.nbt.putInt("x", blockpos.getX());
                                    template$blockinfo.nbt.putInt("y", blockpos.getY());
                                    template$blockinfo.nbt.putInt("z", blockpos.getZ());
                                    tileentity1.read(template$blockinfo.nbt);
                                    tileentity1.mirror(placementIn.getMirror());
                                    tileentity1.rotate(placementIn.getRotation());
                                }
                            }

                            if (ifluidstate != null && blockstate.getBlock() instanceof ILiquidContainer) {
                                ((ILiquidContainer) blockstate.getBlock()).receiveFluid(worldIn, blockpos, blockstate, ifluidstate);
                                if (!ifluidstate.isSource()) {
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
                        IFluidState ifluidstate2 = worldIn.getFluidState(blockpos2);

                        for (int k1 = 0; k1 < adirection.length && !ifluidstate2.isSource(); ++k1) {
                            BlockPos blockpos1 = blockpos3.offset(adirection[k1]);
                            IFluidState ifluidstate1 = worldIn.getFluidState(blockpos1);
                            if (ifluidstate1.getActualHeight(worldIn, blockpos1) > ifluidstate2.getActualHeight(worldIn, blockpos3) || ifluidstate1.isSource() && !ifluidstate2.isSource()) {
                                ifluidstate2 = ifluidstate1;
                                blockpos3 = blockpos1;
                            }
                        }

                        if (ifluidstate2.isSource()) {
                            BlockState blockstate2 = worldIn.getBlockState(blockpos2);
                            Block block = blockstate2.getBlock();
                            if (block instanceof ILiquidContainer) {
                                ((ILiquidContainer) block).receiveFluid(worldIn, blockpos2, blockstate2, ifluidstate2);
                                flag = true;
                                iterator.remove();
                            }
                        }
                    }
                }

                if (i <= l) {
                    for (Pair<BlockPos, CompoundNBT> pair : list2) {
                        BlockPos blockpos4 = pair.getFirst();

                        if (pair.getSecond() != null) {
                            TileEntity tileentity2 = worldIn.getTileEntity(blockpos4);
                            if (tileentity2 != null) {
                                tileentity2.markDirty();
                            }
                        }
                    }
                }

                if (!placementIn.getIgnoreEntities()) {
                    this.addEntitiesToWorld(worldIn, pos, placementIn, placementIn.getMirror(), placementIn.getRotation(), placementIn.getCenterOffset(), placementIn.getBoundingBox(), outSpawnedEntities);
                }

                return true;
            } else {
                return false;
            }
        }
    }


    public void removeBlocks(List<BlockPos> couldNotBeRemoved, BlockPos startPos) {
        for (BlockPos blockPos : couldNotBeRemoved) {
            this.getBlocks().removeIf(blockInfo -> blockPos.subtract(startPos).equals(blockInfo.pos));
        }
    }

    /**
     * list positions of futur deployment
     */
    public List<BlockPos> calculateDeployPositions(World world, BlockPos blockPos, PlacementSettings placementSettings) {

        ArrayList<BlockPos> out = new ArrayList<>();
        if (size == null) return out;

        List<Template.BlockInfo> list = placementSettings.func_227459_a_(this.blocks, blockPos);

        if (!list.isEmpty() && this.size.getX() >= 1 && this.size.getY() >= 1 && this.size.getZ() >= 1) {
            MutableBoundingBox structureboundingbox = placementSettings.getBoundingBox();

            for (Template.BlockInfo template$blockinfo : processBlockInfos(this, world, blockPos, placementSettings, list)) {
                BlockPos blockpos = transformedBlockPos(placementSettings, template$blockinfo.pos)
                        .add(blockPos)
                        .add(recenterRotation((size.getX() - 1) / 2, placementSettings));

                if (template$blockinfo.state.getBlock() != Blocks.STRUCTURE_BLOCK && (structureboundingbox == null || structureboundingbox.isVecInside(blockpos))) {
                    out.add(blockpos);
                }
            }
        }
        return out;
    }

    public static BlockPos recenterRotation(int extendSize, PlacementSettings placement) {
        return CapsuleTemplate.transformedBlockPos(placement, new BlockPos(-extendSize, 0, -extendSize)).add(new BlockPos(extendSize, 0, extendSize));
    }

    public boolean canRotate() {
        try {
            for (Template.BlockInfo block : getBlocks()) {
                if (block.nbt != null && !Config.blueprintWhitelist.containsKey(block.state.getBlock().getRegistryName().toString())) {
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

        if (!nbt.contains("DataVersion", 99)) {
            nbt.putInt("DataVersion", 500);
        }

        this.blocks.clear();
        this.blocks.add(new ArrayList<>());
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
        ListNBT tagList = nbt.getList("Entities", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < tagList.size(); ++i) {
            CompoundNBT entityNBT = tagList.getCompound(i);
            ListNBT posList = entityNBT.getList("Pos", Constants.NBT.TAG_DOUBLE);
            Vec3d vec3d = new Vec3d(posList.getDouble(0), posList.getDouble(1), posList.getDouble(2));
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
                        this.getBlocks().add(new Template.BlockInfo(pos, state, teNBT));
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
        ListNBT tagList = nbt.getList("TileEntities", Constants.NBT.TAG_COMPOUND);
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
                blocksById[bi] = block.getDefaultState();

                byteId = ((int) blockIdsByte[bi + 1]) & 0xFF;
                block = palette[(addValue & 0x0F) << 8 | byteId];
                blocksById[bi + 1] = block.getDefaultState();
            }

            // Odd number of blocks, handle the last position
            if ((numBlocks % 2) != 0) {
                final int addValue = ((int) add[ai]) & 0xFF;
                byteId = ((int) blockIdsByte[bi]) & 0xFF;
                block = palette[(addValue & 0xF0) << 4 | byteId];
                blocksById[bi] = block.getDefaultState();
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
                blocksById[i] = block.getDefaultState();
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
            Set<String> keys = tag.keySet();

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
            Set<String> keys = tag.keySet();

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
            for (Block block : ForgeRegistries.BLOCKS.getValues()) {
                if (block != null) {
                    int id = Block.getStateId(block.getDefaultState());

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