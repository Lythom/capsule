package capsule.structure;

import capsule.Config;
import capsule.tags.CapsuleTags;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import net.minecraft.SharedConstants;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.IdMapper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
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

    public final List<CapsuleTemplate.Palette> palettes = Lists.newArrayList();
    public final List<StructureTemplate.StructureEntityInfo> entities = Lists.newArrayList();
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

    public List<StructureTemplate.StructureBlockInfo> getPalette() {
        if (palettes.size() <= 0 || palettes.get(0).blocks == null) return Lists.newArrayList();
        return palettes.get(0).blocks;
    }

    private static void addToLists(StructureTemplate.StructureBlockInfo p_237149_0_, List<StructureTemplate.StructureBlockInfo> p_237149_1_, List<StructureTemplate.StructureBlockInfo> p_237149_2_, List<StructureTemplate.StructureBlockInfo> p_237149_3_) {
        if (p_237149_0_.nbt() != null) {
            p_237149_2_.add(p_237149_0_);
        } else if (!p_237149_0_.state().getBlock().hasDynamicShape() && p_237149_0_.state().isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) {
            p_237149_1_.add(p_237149_0_);
        } else {
            p_237149_3_.add(p_237149_0_);
        }
    }

    private static List<StructureTemplate.StructureBlockInfo> buildInfoList(List<StructureTemplate.StructureBlockInfo> p_237151_0_, List<StructureTemplate.StructureBlockInfo> p_237151_1_, List<StructureTemplate.StructureBlockInfo> p_237151_2_) {
        Comparator<StructureTemplate.StructureBlockInfo> comparator = Comparator.<StructureTemplate.StructureBlockInfo>comparingInt((p_237154_0_) -> {
            return p_237154_0_.pos().getY();
        }).thenComparingInt((p_237153_0_) -> {
            return p_237153_0_.pos().getX();
        }).thenComparingInt((p_237148_0_) -> {
            return p_237148_0_.pos().getZ();
        });
        p_237151_0_.sort(comparator);
        p_237151_2_.sort(comparator);
        p_237151_1_.sort(comparator);
        List<StructureTemplate.StructureBlockInfo> list = Lists.newArrayList();
        list.addAll(p_237151_0_);
        list.addAll(p_237151_2_);
        list.addAll(p_237151_1_);
        return list;
    }

    public static BlockPos calculateRelativePosition(StructurePlaceSettings placementIn, BlockPos pos) {
        return getTransformedPos(pos, placementIn.getMirror(), placementIn.getRotation(), placementIn.getRotationPivot());
    }

    public static Vec3 transformedVec3d(StructurePlaceSettings placementIn, Vec3 pos) {
        return getTransformedPos(pos, placementIn.getMirror(), placementIn.getRotation(), placementIn.getRotationPivot());
    }

    public static List<StructureTemplate.StructureBlockInfo> processBlockInfos(@Nullable CapsuleTemplate template, LevelAccessor worldIn, BlockPos offsetPos, StructurePlaceSettings placementSettingsIn, List<StructureTemplate.StructureBlockInfo> blockInfos) {
        List<StructureTemplate.StructureBlockInfo> list = Lists.newArrayList();

        for (StructureTemplate.StructureBlockInfo template$blockinfo : blockInfos) {
            BlockPos blockpos = calculateRelativePosition(placementSettingsIn, template$blockinfo.pos()).offset(offsetPos);
            StructureTemplate.StructureBlockInfo template$blockinfo1 = new StructureTemplate.StructureBlockInfo(blockpos, template$blockinfo.state(), template$blockinfo.nbt());
            list.add(template$blockinfo1);
        }

        return list;
    }

    public static List<StructureTemplate.StructureEntityInfo> processEntityInfos(@Nullable CapsuleTemplate template, LevelAccessor p_215387_0_, BlockPos offsetPos, StructurePlaceSettings placementSettingsIn, List<StructureTemplate.StructureEntityInfo> blockInfos) {
        List<StructureTemplate.StructureEntityInfo> list = Lists.newArrayList();
        BlockPos recenterOffset = template == null ? BlockPos.ZERO : recenterRotation((template.size.getX() - 1) / 2, placementSettingsIn);

        for (StructureTemplate.StructureEntityInfo entityInfo : blockInfos) {
            Vec3 pos = transformedVec3d(placementSettingsIn, entityInfo.pos).add(Vec3.atLowerCornerOf(offsetPos)).add(Vec3.atLowerCornerOf(recenterOffset));
            BlockPos blockpos = calculateRelativePosition(placementSettingsIn, entityInfo.blockPos).offset(offsetPos);
            StructureTemplate.StructureEntityInfo info = new StructureTemplate.StructureEntityInfo(pos, blockpos, entityInfo.nbt);
            list.add(info);
        }
        return list;
    }

    private void addEntitiesToWorld(ServerLevelAccessor worldIn, BlockPos offsetPos, StructurePlaceSettings placementIn, Mirror mirrorIn, Rotation rotationIn, BlockPos centerOffset, @Nullable BoundingBox boundsIn, List<Entity> spawnedEntities) {
        for (StructureTemplate.StructureEntityInfo template$entityinfo : processEntityInfos(this, worldIn, offsetPos, placementIn, this.entities)) {
            BlockPos blockpos = template$entityinfo.blockPos; // FORGE: Position will have already been transformed by processEntityInfos
            if (boundsIn == null || boundsIn.isInside(blockpos)) {
                CompoundTag compoundnbt = template$entityinfo.nbt;
                Vec3 vector3d1 = template$entityinfo.pos; // FORGE: Position will have already been transformed by processEntityInfos
                ListTag listnbt = new ListTag();
                listnbt.add(DoubleTag.valueOf(vector3d1.x));
                listnbt.add(DoubleTag.valueOf(vector3d1.y));
                listnbt.add(DoubleTag.valueOf(vector3d1.z));
                compoundnbt.put("Pos", listnbt);
                compoundnbt.remove("UUID");
                createEntityIgnoreException(worldIn, compoundnbt).ifPresent((p_242927_6_) -> {
                    float f = p_242927_6_.mirror(placementIn.getMirror());
                    f = f + (p_242927_6_.getYRot() - p_242927_6_.rotate(placementIn.getRotation()));
                    p_242927_6_.moveTo(vector3d1.x, vector3d1.y, vector3d1.z, f, p_242927_6_.getXRot());
                    if (placementIn.shouldFinalizeEntities() && p_242927_6_ instanceof Mob) {
                        ((Mob) p_242927_6_).finalizeSpawn(worldIn, worldIn.getCurrentDifficultyAt(BlockPos.containing(vector3d1)), MobSpawnType.STRUCTURE, (SpawnGroupData) null, compoundnbt);
                    }
                    worldIn.addFreshEntityWithPassengers(p_242927_6_);
                    if (spawnedEntities != null) spawnedEntities.add(p_242927_6_);
                });
            }
        }
    }

    private static Optional<Entity> createEntityIgnoreException(ServerLevelAccessor worldIn, CompoundTag nbt) {
        try {
            return EntityType.create(nbt, worldIn.getLevel());
        } catch (Exception exception) {
            return Optional.empty();
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

    public static Vec3 getTransformedPos(Vec3 target, Mirror mirrorIn, Rotation rotationIn, BlockPos centerOffset) {
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
                return new Vec3((double) (i - j) + d2, d1, (double) (i + j + 1) - d0);
            case CLOCKWISE_90:
                return new Vec3((double) (i + j + 1) - d2, d1, (double) (j - i) + d0);
            case CLOCKWISE_180:
                return new Vec3((double) (i + i + 1) - d0, d1, (double) (j + j + 1) - d2);
            default:
                return flag ? new Vec3(d0, d1, d2) : target;
        }
    }

    public CompoundTag save(CompoundTag nbt) {
        if (this.palettes.isEmpty()) {
            nbt.put("blocks", new ListTag());
            nbt.put("palette", new ListTag());
        } else {
            List<CapsuleTemplate.BasicPalette> list = Lists.newArrayList();
            CapsuleTemplate.BasicPalette template$basicpalette = new CapsuleTemplate.BasicPalette();
            list.add(template$basicpalette);

            for (int i = 1; i < this.palettes.size(); ++i) {
                list.add(new CapsuleTemplate.BasicPalette());
            }

            ListTag listnbt1 = new ListTag();
            List<StructureTemplate.StructureBlockInfo> list1 = palettes.get(0).blocks;

            for (int j = 0; j < list1.size(); ++j) {
                StructureTemplate.StructureBlockInfo template$blockinfo = list1.get(j);
                CompoundTag compoundnbt = new CompoundTag();
                compoundnbt.put("pos", this.newIntegerList(template$blockinfo.pos().getX(), template$blockinfo.pos().getY(), template$blockinfo.pos().getZ()));
                int k = template$basicpalette.idFor(template$blockinfo.state());
                compoundnbt.putInt("state", k);
                if (template$blockinfo.nbt() != null) {
                    compoundnbt.put("nbt", template$blockinfo.nbt());
                }

                listnbt1.add(compoundnbt);

                for (int l = 1; l < this.palettes.size(); ++l) {
                    CapsuleTemplate.BasicPalette template$basicpalette1 = list.get(l);
                    template$basicpalette1.addMapping((this.palettes.get(l).blocks().get(j)).state(), k);
                }
            }

            nbt.put("blocks", listnbt1);
            if (list.size() == 1) {
                ListTag listnbt2 = new ListTag();

                for (BlockState blockstate : template$basicpalette) {
                    listnbt2.add(NbtUtils.writeBlockState(blockstate));
                }

                nbt.put("palette", listnbt2);
            } else {
                ListTag listnbt3 = new ListTag();

                for (CapsuleTemplate.BasicPalette template$basicpalette2 : list) {
                    ListTag listnbt4 = new ListTag();

                    for (BlockState blockstate1 : template$basicpalette2) {
                        listnbt4.add(NbtUtils.writeBlockState(blockstate1));
                    }

                    listnbt3.add(listnbt4);
                }

                nbt.put("palettes", listnbt3);
            }
        }

        ListTag listnbt = new ListTag();

        for (StructureTemplate.StructureEntityInfo template$entityinfo : this.entities) {
            CompoundTag compoundnbt1 = new CompoundTag();
            compoundnbt1.put("pos", this.newDoubleList(template$entityinfo.pos.x, template$entityinfo.pos.y, template$entityinfo.pos.z));
            compoundnbt1.put("blockPos", this.newIntegerList(template$entityinfo.blockPos.getX(), template$entityinfo.blockPos.getY(), template$entityinfo.blockPos.getZ()));
            if (template$entityinfo.nbt != null) {
                compoundnbt1.put("nbt", template$entityinfo.nbt);
            }

            listnbt.add(compoundnbt1);
        }

        nbt.put("entities", listnbt);
        nbt.put("size", this.newIntegerList(this.size.getX(), this.size.getY(), this.size.getZ()));
        nbt.putInt("DataVersion", SharedConstants.getCurrentVersion().getDataVersion().getVersion());

        // CAPSULE save already occupied positions when deployed
        ListTag occupiedSpawnPositionstaglist = new ListTag();
        if (this.occupiedPositions != null) {
            for (Map.Entry<BlockPos, Block> entry : occupiedPositions.entrySet()) {
                CompoundTag nbtEntry = new CompoundTag();
                nbtEntry.putLong("pos", entry.getKey().asLong());
                nbtEntry.putInt("blockId", Block.getId(entry.getValue().defaultBlockState()));
                occupiedSpawnPositionstaglist.add(nbtEntry);
            }
            nbt.put("capsule_occupiedSources", occupiedSpawnPositionstaglist);
        }
        nbt.putString("author", getAuthor());
        return nbt;
    }

    public void load(CompoundTag compound, String location) {
        this.palettes.clear();
        this.entities.clear();
        ListTag listnbt = compound.getList("size", 3);
        this.size = new BlockPos(listnbt.getInt(0), listnbt.getInt(1), listnbt.getInt(2));
        ListTag listnbt1 = compound.getList("blocks", 10);
        if (compound.contains("palettes", 9)) {
            ListTag listnbt2 = compound.getList("palettes", 9);

            for (int i = 0; i < listnbt2.size(); ++i) {
                this.loadPalette(listnbt2.getList(i), listnbt1);
            }
        } else {
            this.loadPalette(compound.getList("palette", 10), listnbt1);
        }

        ListTag listnbt5 = compound.getList("entities", 10);

        for (int j = 0; j < listnbt5.size(); ++j) {
            CompoundTag compoundnbt = listnbt5.getCompound(j);
            ListTag listnbt3 = compoundnbt.getList("pos", 6);
            Vec3 vector3d = new Vec3(listnbt3.getDouble(0), listnbt3.getDouble(1), listnbt3.getDouble(2));
            ListTag listnbt4 = compoundnbt.getList("blockPos", 3);
            BlockPos blockpos = new BlockPos(listnbt4.getInt(0), listnbt4.getInt(1), listnbt4.getInt(2));
            if (compoundnbt.contains("nbt")) {
                CompoundTag compoundnbt1 = compoundnbt.getCompound("nbt");
                this.entities.add(new StructureTemplate.StructureEntityInfo(vector3d, blockpos, compoundnbt1));
            }
        }

        // CAPSULE read already occupied positions when deployed
        if (compound.contains("capsule_occupiedSources")) {
            Map<BlockPos, Block> occupiedSources = new HashMap<>();
            ListTag list = compound.getList("capsule_occupiedSources", 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                occupiedSources.put(BlockPos.of(entry.getLong("pos")), Block.stateById(entry.getInt("blockId")).getBlock());
            }
            this.occupiedPositions = occupiedSources;
        }
        if (this.palettes.size() == 0) {
            LOGGER.error("Template located at " + location + " might be missing or corrupted: it should have a palette tag.");
        }
    }

    private void loadPalette(ListTag palettesNBT, ListTag blocksNBT) {
        CapsuleTemplate.BasicPalette template$basicpalette = new CapsuleTemplate.BasicPalette();

        for (int i = 0; i < palettesNBT.size(); ++i) {
            template$basicpalette.addMapping(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), palettesNBT.getCompound(i)), i);
        }

        List<StructureTemplate.StructureBlockInfo> list2 = Lists.newArrayList();
        List<StructureTemplate.StructureBlockInfo> list = Lists.newArrayList();
        List<StructureTemplate.StructureBlockInfo> list1 = Lists.newArrayList();

        for (int j = 0; j < blocksNBT.size(); ++j) {
            CompoundTag compoundnbt = blocksNBT.getCompound(j);
            ListTag listnbt = compoundnbt.getList("pos", 3);
            BlockPos blockpos = new BlockPos(listnbt.getInt(0), listnbt.getInt(1), listnbt.getInt(2));
            BlockState blockstate = template$basicpalette.stateFor(compoundnbt.getInt("state"));
            CompoundTag compoundnbt1;
            if (compoundnbt.contains("nbt")) {
                compoundnbt1 = compoundnbt.getCompound("nbt");
            } else {
                compoundnbt1 = null;
            }

            StructureTemplate.StructureBlockInfo template$blockinfo = new StructureTemplate.StructureBlockInfo(blockpos, blockstate, compoundnbt1);
            addToLists(template$blockinfo, list2, list, list1);
        }

        List<StructureTemplate.StructureBlockInfo> list3 = buildInfoList(list2, list, list1);
        this.palettes.add(new CapsuleTemplate.Palette(list3));
    }

    private ListTag newIntegerList(int... values) {
        ListTag listnbt = new ListTag();

        for (int i : values) {
            listnbt.add(IntTag.valueOf(i));
        }

        return listnbt;
    }

    private ListTag newDoubleList(double... values) {
        ListTag listnbt = new ListTag();

        for (double d0 : values) {
            listnbt.add(DoubleTag.valueOf(d0));
        }

        return listnbt;
    }

    public void filterFromWhitelist(List<String> outExcluded) {
        List<StructureTemplate.StructureBlockInfo> newBlockList = this.getPalette().stream()
                .filter(b -> {
                    ResourceLocation registryName = ForgeRegistries.BLOCKS.getKey(b.state().getBlock());
                    boolean included = b.nbt() == null
                            || registryName != null && Config.blueprintWhitelist.containsKey(registryName.toString());
                    if (!included && outExcluded != null) outExcluded.add(b.state().toString());
                    return included;
                })
                .map(b -> {
                    if (b.nbt() == null) return b;
                    // remove all unlisted nbt data to prevent dupe or cheating
                    CompoundTag nbt = null;
                    JsonObject allowedNBT = Config.getBlueprintAllowedNBT(b.state().getBlock());
                    if (allowedNBT != null) {
                        nbt = b.nbt().copy();
                        nbt.getAllKeys().removeIf(key -> !allowedNBT.has(key));
                    } else {
                        nbt = new CompoundTag();
                    }
                    return new StructureTemplate.StructureBlockInfo(
                            b.pos(),
                            b.state(),
                            nbt
                    );
                }).collect(Collectors.toList());
        getPalette().clear();
        getPalette().addAll(newBlockList);
        // remove all entities
        entities.clear();
    }

    static class BasicPalette implements Iterable<BlockState> {
        public static final BlockState DEFAULT_BLOCK_STATE = Blocks.AIR.defaultBlockState();
        private final IdMapper<BlockState> ids = new IdMapper<>(16);
        private int lastId;

        private BasicPalette() {
        }

        public int idFor(BlockState state) {
            int i = this.ids.getId(state);
            if (i == -1) {
                i = this.lastId++;
                this.ids.addMapping(state, i);
            }

            return i;
        }

        @Nullable
        public BlockState stateFor(int id) {
            BlockState blockstate = this.ids.byId(id);
            return blockstate == null ? DEFAULT_BLOCK_STATE : blockstate;
        }

        public Iterator<BlockState> iterator() {
            return this.ids.iterator();
        }

        public void addMapping(BlockState p_189956_1_, int p_189956_2_) {
            this.ids.addMapping(p_189956_1_, p_189956_2_);
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
    public List<BlockPos> snapshotBlocksFromWorld(Level worldIn, BlockPos startPos, BlockPos endPos,
                                                  Map<BlockPos, Block> occupiedPositionsToIgnore, List<Block> excluded, List<Entity> outCapturedEntities) {

        List<BlockPos> transferedBlocks = new ArrayList<>();

        if (endPos.getX() >= 1 && endPos.getY() >= 1 && endPos.getZ() >= 1) {
            BlockPos blockpos = startPos.offset(endPos).offset(-1, -1, -1);
            List<StructureTemplate.StructureBlockInfo> list = Lists.newArrayList();
            List<StructureTemplate.StructureBlockInfo> list1 = Lists.newArrayList();
            List<StructureTemplate.StructureBlockInfo> list2 = Lists.newArrayList();
            BlockPos blockpos1 = new BlockPos(Math.min(startPos.getX(), blockpos.getX()), Math.min(startPos.getY(), blockpos.getY()), Math.min(startPos.getZ(), blockpos.getZ()));
            BlockPos blockpos2 = new BlockPos(Math.max(startPos.getX(), blockpos.getX()), Math.max(startPos.getY(), blockpos.getY()), Math.max(startPos.getZ(), blockpos.getZ()));
            this.size = endPos;

            for (BlockPos blockpos3 : BlockPos.betweenClosed(blockpos1, blockpos2)) {
                BlockPos blockpos4 = blockpos3.subtract(blockpos1);
                BlockState blockstate = worldIn.getBlockState(blockpos3);
                if (!excluded.contains(blockstate.getBlock()) // excluded blocks are not captured at all
                        && !blockstate.is(CapsuleTags.excludedBlocks) // excluded tags are not captured at all
                        && (occupiedPositionsToIgnore == null // exclude sourceBlock that were already presents. Capture only if it was changed.
                        || !(occupiedPositionsToIgnore.containsKey(blockpos3)
                        && occupiedPositionsToIgnore.get(blockpos3).equals(blockstate.getBlock())))) {

                    BlockEntity blockentity = worldIn.getBlockEntity(blockpos3);
                    StructureTemplate.StructureBlockInfo template$blockinfo;
                    if (blockentity != null) {
                        CompoundTag compoundnbt = blockentity.saveWithId();
                        compoundnbt.remove("x");
                        compoundnbt.remove("y");
                        compoundnbt.remove("z");
                        template$blockinfo = new StructureTemplate.StructureBlockInfo(blockpos4, blockstate, compoundnbt);
                    } else {
                        template$blockinfo = new StructureTemplate.StructureBlockInfo(blockpos4, blockstate, (CompoundTag) null);
                    }

                    addToLists(template$blockinfo, list, list1, list2);
                    // save a copy
                    transferedBlocks.add(new BlockPos(blockpos3.getX(), blockpos3.getY(), blockpos3.getZ()));
                }
            }

            List<StructureTemplate.StructureBlockInfo> list3 = buildInfoList(list, list1, list2);
            this.palettes.clear();
            this.palettes.add(new CapsuleTemplate.Palette(list3));

            List<Entity> capturedEntities = this.snapshotNonLivingEntitiesFromWorld(worldIn, blockpos1, blockpos2.offset(1, 1, 1));
            if (outCapturedEntities != null && capturedEntities != null) {
                outCapturedEntities.addAll(capturedEntities);
            }
        }
        return transferedBlocks;
    }

    /**
     * takes blocks from the world and puts the data them into this template
     */
    public List<Entity> snapshotNonLivingEntitiesFromWorld(Level worldIn, BlockPos startPos, BlockPos endPos) {

        // rewritten vanilla code from CapsuleTemplate.takeEntitiesFromWorld
        List<Entity> list = worldIn.getEntitiesOfClass(
                Entity.class,
                new AABB(startPos, endPos),
                entity -> !(entity instanceof ItemEntity) && (!(entity instanceof LivingEntity) || (entity instanceof ArmorStand))
        );
        entities.clear();

        for (Entity entity : list) {
            Vec3 vec3d = new Vec3(entity.getX() - (double) startPos.getX(), entity.getY() - (double) startPos.getY(), entity.getZ() - (double) startPos.getZ());
            CompoundTag compoundnbt = new CompoundTag();
            entity.save(compoundnbt);
            BlockPos blockpos;
            if (entity instanceof Painting) {
                blockpos = ((Painting) entity).getPos().subtract(startPos);
            } else {
                blockpos = BlockPos.containing(vec3d);
            }

            this.entities.add(new StructureTemplate.StructureEntityInfo(vec3d, blockpos, compoundnbt.copy()));
        }
        return list;
    }

    public static AABB transformedAxisAlignedBB(StructurePlaceSettings placementIn, AABB bb) {
        return new AABB(
                calculateRelativePosition(placementIn, BlockPos.containing(bb.minX, bb.minY, bb.minZ)),
                calculateRelativePosition(placementIn, BlockPos.containing(bb.maxX, bb.maxY, bb.maxZ))
        );
    }

    /**
     * Tweaked version of "placeInWorld" for capsule
     */
    public boolean spawnBlocksAndEntities(ServerLevelAccessor worldIn,
                                          BlockPos pos,
                                          StructurePlaceSettings placementIn,
                                          Map<BlockPos, Block> occupiedPositions,
                                          List<BlockPos> outSpawnedBlocks,
                                          List<Entity> outSpawnedEntities) {
        int flags = 2; /** @see net.minecraft.world.level.Level#setBlock(BlockPos, BlockState, int) */
        if (this.palettes.isEmpty()) {
            return false;
        } else {
            List<StructureTemplate.StructureBlockInfo> list = null;
            try {
                list = Palette.getRandomPalette(placementIn, this.palettes, pos).blocks();
            } catch (Exception e) {
                LOGGER.error(e);
                list = new ArrayList<>();
            }
            if ((!list.isEmpty() || !placementIn.isIgnoreEntities() && !this.entities.isEmpty()) && this.size.getX() >= 1 && this.size.getY() >= 1 && this.size.getZ() >= 1) {
                BoundingBox mutableboundingbox = placementIn.getBoundingBox();
                List<BlockPos> list1 = Lists.newArrayListWithCapacity(placementIn.shouldKeepLiquids() ? list.size() : 0);
                List<Pair<BlockPos, CompoundTag>> list2 = Lists.newArrayListWithCapacity(list.size());
                int i = Integer.MAX_VALUE;
                int j = Integer.MAX_VALUE;
                int k = Integer.MAX_VALUE;
                int l = Integer.MIN_VALUE;
                int i1 = Integer.MIN_VALUE;
                int j1 = Integer.MIN_VALUE;

                for (StructureTemplate.StructureBlockInfo template$blockinfo : processBlockInfos(this, worldIn, pos, placementIn, list)) {
                    // CAPSULE center the structure
                    BlockPos blockpos = template$blockinfo.pos().offset(recenterRotation((size.getX() - 1) / 2, placementIn));
                    if ((mutableboundingbox == null || mutableboundingbox.isInside(blockpos))
                            // CAPSULE add a condition to prevent replacement of existing content by the capsule content if the world content is not overridable
                            && (!occupiedPositions.containsKey(blockpos) || occupiedPositions.get(blockpos).defaultBlockState().is(CapsuleTags.overridable))) {
                        // CAPSULE capsule addition to allow a rollback in case of error while deploying
                        if (outSpawnedBlocks != null) outSpawnedBlocks.add(blockpos);

                        FluidState ifluidstate = placementIn.shouldKeepLiquids() ? worldIn.getFluidState(blockpos) : null;
                        BlockState blockstate = template$blockinfo.state().mirror(placementIn.getMirror()).rotate(worldIn, blockpos, placementIn.getRotation());
                        BlockEntity blockentity = worldIn.getBlockEntity(blockpos);
                        Clearable.tryClear(blockentity);
                        worldIn.setBlock(blockpos, Blocks.BARRIER.defaultBlockState(), 20);

                        if (worldIn.setBlock(blockpos, blockstate, flags)) {
                            i = Math.min(i, blockpos.getX());
                            j = Math.min(j, blockpos.getY());
                            k = Math.min(k, blockpos.getZ());
                            l = Math.max(l, blockpos.getX());
                            i1 = Math.max(i1, blockpos.getY());
                            j1 = Math.max(j1, blockpos.getZ());
                            list2.add(Pair.of(blockpos, template$blockinfo.nbt()));
                            BlockEntity blockentity1 = worldIn.getBlockEntity(blockpos);
                            if (blockentity1 != null) {
                                CompoundTag templateTag = template$blockinfo.nbt() == null ? new CompoundTag() : template$blockinfo.nbt();
                                templateTag.putInt("x", blockpos.getX());
                                templateTag.putInt("y", blockpos.getY());
                                templateTag.putInt("z", blockpos.getZ());
                                blockentity1.load(templateTag);
                                blockentity1.getBlockState().mirror(placementIn.getMirror());
                                blockentity1.getBlockState().rotate(worldIn, placementIn.getRotationPivot(), placementIn.getRotation());
                            }

                            if (ifluidstate != null && blockstate.getBlock() instanceof LiquidBlockContainer) {
                                ((LiquidBlockContainer) blockstate.getBlock()).placeLiquid(worldIn, blockpos, blockstate, ifluidstate);
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
                        FluidState ifluidstate2 = worldIn.getFluidState(blockpos2);

                        for (int k1 = 0; k1 < adirection.length && !ifluidstate2.isSource(); ++k1) {
                            BlockPos blockpos1 = blockpos3.relative(adirection[k1]);
                            FluidState ifluidstate1 = worldIn.getFluidState(blockpos1);
                            if (ifluidstate1.getHeight(worldIn, blockpos1) > ifluidstate2.getHeight(worldIn, blockpos3) || ifluidstate1.isSource() && !ifluidstate2.isSource()) {
                                ifluidstate2 = ifluidstate1;
                                blockpos3 = blockpos1;
                            }
                        }

                        if (ifluidstate2.isSource()) {
                            BlockState blockstate2 = worldIn.getBlockState(blockpos2);
                            Block block = blockstate2.getBlock();
                            if (block instanceof LiquidBlockContainer) {
                                ((LiquidBlockContainer) block).placeLiquid(worldIn, blockpos2, blockstate2, ifluidstate2);
                                flag = true;
                                iterator.remove();
                            }
                        }
                    }
                }

                if (i <= l) {
                    if (!placementIn.getKnownShape()) {
                        DiscreteVoxelShape voxelshapepart = new BitSetDiscreteVoxelShape(l - i + 1, i1 - j + 1, j1 - k + 1);
                        int l1 = i;
                        int i2 = j;
                        int j2 = k;

                        for (Pair<BlockPos, CompoundTag> pair1 : list2) {
                            BlockPos blockpos5 = pair1.getFirst();
                            voxelshapepart.fill(blockpos5.getX() - l1, blockpos5.getY() - i2, blockpos5.getZ() - j2);
                        }

                        StructureTemplate.updateShapeAtEdge(worldIn, flags, voxelshapepart, l1, i2, j2);
                    }
                    for (Pair<BlockPos, CompoundTag> pair : list2) {
                        BlockPos blockpos4 = pair.getFirst();
                        if (!placementIn.getKnownShape()) {
                            BlockState blockstate1 = worldIn.getBlockState(blockpos4);
                            BlockState blockstate3 = Block.updateFromNeighbourShapes(blockstate1, worldIn, blockpos4);
                            if (blockstate1 != blockstate3) {
                                worldIn.setBlock(blockpos4, blockstate3, flags & -2 | 16);
                            }

                            worldIn.blockUpdated(blockpos4, blockstate3.getBlock());
                        }
                    }
                }

                if (!placementIn.isIgnoreEntities()) {
                    this.addEntitiesToWorld(worldIn, pos, placementIn, placementIn.getMirror(), placementIn.getRotation(), placementIn.getRotationPivot(), placementIn.getBoundingBox(), outSpawnedEntities);
                }

                return true;
            } else {
                return false;
            }
        }
    }


    public void removeBlocks(List<BlockPos> couldNotBeRemoved, BlockPos startPos) {
        for (BlockPos blockPos : couldNotBeRemoved) {
            for (Palette palette : this.palettes) {
                palette.blocks.removeIf(blockInfo -> blockPos.subtract(startPos).equals(blockInfo.pos()));
            }
        }
    }

    /**
     * list positions of futur deployment
     */
    public List<BlockPos> calculateDeployPositions(Level level, BlockPos deployPos, StructurePlaceSettings placementSettings) {

        ArrayList<BlockPos> out = new ArrayList<>();
        if (size == null) return out;

        List<StructureTemplate.StructureBlockInfo> list = null;
        try {
            list = Palette.getRandomPalette(placementSettings, this.palettes, deployPos).blocks();
        } catch (Exception e) {
            LOGGER.error(e);
            list = new ArrayList<>();
        }
        if (!list.isEmpty() && this.size.getX() >= 1 && this.size.getY() >= 1 && this.size.getZ() >= 1) {
            BoundingBox structureboundingbox = placementSettings.getBoundingBox();

            for (StructureTemplate.StructureBlockInfo template$blockinfo : processBlockInfos(this, level, deployPos, placementSettings, list)) {
                BlockPos blockpos = template$blockinfo.pos().offset(recenterRotation((size.getX() - 1) / 2, placementSettings));
                if (template$blockinfo.state().getBlock() != Blocks.STRUCTURE_BLOCK && (structureboundingbox == null || structureboundingbox.isInside(blockpos))) {
                    out.add(blockpos);
                }
            }
        }
        return out;
    }

    public static BlockPos recenterRotation(int extendSize, StructurePlaceSettings placement) {
        return CapsuleTemplate.calculateRelativePosition(placement, new BlockPos(-extendSize, 0, -extendSize)).offset(new BlockPos(extendSize, 0, extendSize));
    }

    public boolean canRotate() {
        if (palettes.isEmpty()) return false;
        try {
            for (StructureTemplate.StructureBlockInfo block : getPalette()) {
                if (block.nbt() != null && !Config.blueprintWhitelist.containsKey(ForgeRegistries.BLOCKS.getKey(block.state().getBlock()).toString())) {
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
    // schematic V2: https://github.com/SpongePowered/Schematic-Specification/blob/master/versions/schematic-2.md
    // schematic V3: https://github.com/SpongePowered/Schematic-Specification/blob/master/versions/schematic-3.md
    public void readSchematic(CompoundTag nbt) throws Exception {
        int version = 1;
        if (nbt.contains("Version", Tag.TAG_INT)) {
            version = nbt.getInt("Version");
        }
        if (version > 3) {
            throw new Exception("Schematic version >3 not supported");
        }
        if (version == 1 && !(nbt.contains("Blocks", Tag.TAG_BYTE_ARRAY) && nbt.contains("Data", Tag.TAG_BYTE_ARRAY))) {
            throw new Exception("Schematic: Missing data in the schematic");
        }

        if (!nbt.contains("DataVersion", 99)) {
            nbt.putInt("DataVersion", 500);
        }

        this.palettes.clear();
        this.palettes.add(new Palette(new ArrayList<>()));
        this.entities.clear();

        int width = nbt.getShort("Width");
        int height = nbt.getShort("Height");
        int length = nbt.getShort("Length");
        int paletteSize = switch (version) {
            case 2 -> nbt.getInt("PaletteMax");
            case 3 -> nbt.getCompound("Blocks").getCompound("PaletteMax").size();
            default -> 4095;
        };
        this.author = "?";
        try {
            this.author = nbt.getCompound("Metadata").getString("Author");
        } catch (Exception e) {
        }


        BlockState[] palette = this.readSchematicPalette(nbt, version, paletteSize);
        if (palette == null || palette.length == 0) {
            throw new Exception("Schematic: Failed to read the block palette, see logs");
        }

        // get blocks informations
        BlockState[] blocksById = getSchematicBlocks(nbt, version, palette, width, height, length);
        if (blocksById == null) {
            throw new Exception("Schematic: Failed to read the block stats, see logs");
        }

        // get tile entities
        Map<BlockPos, CompoundTag> tiles = getSchematicBlockEntities(nbt, version);

        // get entities
        this.entities.clear();
        ListTag tagList = nbt.getList("Entities", Tag.TAG_COMPOUND);
        for (int i = 0; i < tagList.size(); ++i) {
            CompoundTag entityNBT = tagList.getCompound(i).copy();
            ListTag posList = entityNBT.getList("Pos", Tag.TAG_DOUBLE);
            Vec3 vec3d = new Vec3(posList.getDouble(0), posList.getDouble(1), posList.getDouble(2));
            entityNBT.remove("Pos");
            this.entities.add(new StructureTemplate.StructureEntityInfo(vec3d, BlockPos.containing(vec3d), entityNBT));
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
                        CompoundTag teNBT = tiles.get(pos);
                        getPalette().add(new StructureTemplate.StructureBlockInfo(pos, state, teNBT));
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
    }

    private Map<BlockPos, CompoundTag> getSchematicBlockEntities(CompoundTag nbt, int version) {
        Map<BlockPos, CompoundTag> blockEntities = new HashMap<>();
        if (version == 1) {
            ListTag tagList = nbt.getList("TileEntities", Tag.TAG_COMPOUND);
            for (int i = 0; i < tagList.size(); ++i) {
                CompoundTag tag = tagList.getCompound(i);
                BlockPos pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
                blockEntities.put(pos, tag);
            }
        } else if (version == 2 || version == 3) {
            ListTag tagList = version == 2 ? nbt.getList("BlockEntities", Tag.TAG_COMPOUND) : nbt.getCompound("Blocks").getList("BlockEntities", Tag.TAG_COMPOUND);
            for (int i = 0; i < tagList.size(); ++i) {
                CompoundTag tag = tagList.getCompound(i).copy();
                int[] positions = tag.getIntArray("Pos");
                BlockPos pos = new BlockPos(positions[0], positions[1], positions[2]);
                tag.remove("Pos");
                blockEntities.put(pos, tag);
            }
        }
        return blockEntities;
    }

    @Nullable
    private BlockState[] getSchematicBlocks(CompoundTag nbt, int version, BlockState[] palette, int width, int height,
                                            int length) throws Exception {
        byte[] blockIdsByte = switch (version) {
            case 1 -> nbt.getByteArray("Blocks");
            case 2 -> nbt.getByteArray("BlockData");
            case 3 -> nbt.getCompound("Blocks").getByteArray("Data");
            default -> null;
        };
        final int numBlocks = blockIdsByte.length;
//        if (numBlocks != (width * height * length)) {
//            LOGGER.error("Schematic: Mismatched block array size compared to the width/height/length, blocks: {}, W x H x L: {} x {} x {}",
//                    numBlocks, width, height, length);
//            return null;
//        }
        BlockState[] blocksById = new BlockState[numBlocks];
        if (version == 2 || version == 3) {
            int index = 0;
            int i = 0;
            int value = 0;
            int varint_length = 0;
            while (i < numBlocks) {
                value = 0;
                varint_length = 0;

                while (true) {
                    value |= (blockIdsByte[i] & 127) << (varint_length++ * 7);
                    if (varint_length > 5) {
                        throw new RuntimeException("VarInt too big (probably corrupted data)");
                    }
                    if ((blockIdsByte[i] & 128) != 128) {
                        i++;
                        break;
                    }
                    i++;
                }
                // index = (y * length + z) * width + x
                int y = index / (width * length);
                int z = (index % (width * length)) / width;
                int x = (index % (width * length)) % width;
                BlockState state = palette[value];
                blocksById[index] = state;
                index++;
            }
        } else if (nbt.contains("AddBlocks", Tag.TAG_BYTE_ARRAY)) {
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

            BlockState block;
            int byteId;
            int bi, ai;

            // Handle two positions per iteration, ie. one full byte of the add array
            for (bi = 0, ai = 0; bi < loopMax; bi += 2, ai++) {
                final int addValue = ((int) add[ai]) & 0xFF;

                byteId = ((int) blockIdsByte[bi]) & 0xFF;
                block = palette[(addValue & 0xF0) << 4 | byteId];
                blocksById[bi] = block;

                byteId = ((int) blockIdsByte[bi + 1]) & 0xFF;
                block = palette[(addValue & 0x0F) << 8 | byteId];
                blocksById[bi + 1] = block;
            }

            // Odd number of blocks, handle the last position
            if ((numBlocks % 2) != 0) {
                final int addValue = ((int) add[ai]) & 0xFF;
                byteId = ((int) blockIdsByte[bi]) & 0xFF;
                block = palette[(addValue & 0xF0) << 4 | byteId];
                blocksById[bi] = block;
            }
        }
        // Old Schematica format
        else if (nbt.contains("Add", Tag.TAG_BYTE_ARRAY)) {
            LOGGER.error("Schematic: Old Schematica format detected, not implemented");
            return null;
        }
        // No palette, use the registry IDs directly
        else {
            for (int i = 0; i < numBlocks; i++) {
                BlockState block = palette[((int) blockIdsByte[i]) & 0xFF];
                blocksById[i] = block;
            }
        }
        return blocksById;
    }

    @Nullable
    private BlockState[] readSchematicPalette(CompoundTag nbt, int version, int paletteSize) throws Exception {
        final BlockState air = Blocks.AIR.defaultBlockState();
        BlockState[] palette = new BlockState[paletteSize];
        Arrays.fill(palette, air);

        // MCEdit2 palette (Legacy)
        if (nbt.contains("BlockIDs", Tag.TAG_COMPOUND)) {
            CompoundTag tag = nbt.getCompound("BlockIDs");
            Set<String> keys = tag.getAllKeys();

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

                BlockState block = parseBlockState(key);
                if (block != null) {
                    palette[id] = block;
                } else {
                    LOGGER.error("Schematic: Missing/non-existing block '{}' in MCEdit2 palette", key);
                }
            }
        } else {
            // other type of palettes have different locations but works the same
            CompoundTag tag = null;
            if (version == 2) {
                tag = nbt.getCompound("Palette");
            } else if (version == 3) {
                tag = nbt.getCompound("Blocks").getCompound("Palette");
            } else if (nbt.contains("SchematicaMapping", Tag.TAG_COMPOUND)) {
                tag = nbt.getCompound("SchematicaMapping");
            }
            Set<String> keys = tag.getAllKeys();

            for (String key : keys) {
                int idx = tag.getShort(key);

                if (idx >= palette.length) {
                    LOGGER.error("Schematic: Invalid ID '{}' in MCEdit2 palette for block '{}', max = {}", idx, key, paletteSize);
                    return null;
                }

                palette[idx] = parseBlockState(key);
            }
        }

        return palette;
    }

    private BlockState parseBlockState(String key) {
        try {
            BlockState state = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), key, true).blockState();
            return state;
        } catch (CommandSyntaxException exception) {
            LOGGER.error("Could not parse {}. {}", key, exception);
            return Blocks.AIR.defaultBlockState();
        }
    }

    public static final class Palette {
        private final List<StructureTemplate.StructureBlockInfo> blocks;
        private final Map<Block, List<StructureTemplate.StructureBlockInfo>> cache = Maps.newHashMap();

        private Palette(List<StructureTemplate.StructureBlockInfo> p_i232120_1_) {
            this.blocks = p_i232120_1_;
            this.blocks.sort(Comparator.comparingInt(b -> b.pos().getY()));
        }

        public List<StructureTemplate.StructureBlockInfo> blocks() {
            return this.blocks;
        }

        public static Palette getRandomPalette(StructurePlaceSettings placementSettings, List<Palette> p_237132_1_, @Nullable BlockPos p_237132_2_) {
            int i = p_237132_1_.size();
            if (i == 0) {
                throw new IllegalStateException("No palettes");
            } else {
                return p_237132_1_.get(placementSettings.getRandom(p_237132_2_).nextInt(i));
            }
        }
    }
}
