package capsule.structure;

import capsule.Config;
import capsule.platform.Services;
import capsule.tags.CapsuleTags;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import net.minecraft.SharedConstants;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.IdMapper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Copy of mc original net.minecraft.world.gen.feature.template class, but having fields public to allow external manipulation.
 * Also include some specificities to capsules
 *
 * @author Lythom
 */
public class CapsuleTemplate {
    protected static final Logger LOGGER = LogManager.getLogger(CapsuleTemplate.class);
    // ProblemReporter.ScopedCollector logs through slf4j
    private static final org.slf4j.Logger PROBLEM_LOGGER = LogUtils.getLogger();

    /**
     * Block state properties showing the content of a block entity, false once its content is removed.
     */
    private static final List<BooleanProperty> CONTENT_PROPERTIES = Stream.concat(
            ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.stream(),
            Stream.of(BlockStateProperties.HAS_BOOK, BlockStateProperties.HAS_RECORD, BlockStateProperties.HAS_BOTTLE_0, BlockStateProperties.HAS_BOTTLE_1, BlockStateProperties.HAS_BOTTLE_2)
    ).toList();

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

    private void addEntitiesToWorld(ServerLevelAccessor worldIn, BlockPos offsetPos, StructurePlaceSettings placementIn, Mirror mirrorIn, Rotation rotationIn, BlockPos centerOffset, @Nullable BoundingBox boundsIn, List<Entity> spawnedEntities, ProblemReporter problemReporter) {
        for (StructureTemplate.StructureEntityInfo template$entityinfo : processEntityInfos(this, worldIn, offsetPos, placementIn, this.entities)) {
            BlockPos blockpos = template$entityinfo.blockPos; // FORGE: Position will have already been transformed by processEntityInfos
            if (boundsIn == null || boundsIn.isInside(blockpos)) {
                CompoundTag compoundnbt = template$entityinfo.nbt.copy();
                Vec3 vector3d1 = template$entityinfo.pos; // FORGE: Position will have already been transformed by processEntityInfos
                ListTag listnbt = new ListTag();
                listnbt.add(DoubleTag.valueOf(vector3d1.x));
                listnbt.add(DoubleTag.valueOf(vector3d1.y));
                listnbt.add(DoubleTag.valueOf(vector3d1.z));
                compoundnbt.put("Pos", listnbt);
                compoundnbt.remove("UUID");
                if (compoundnbt.contains("block_pos")) {
                    // item frames and paintings refuse a saved block far from their position (TileX/Y/Z before
                    // 1.21.5, upgraded by the data fixers when the template loads)
                    compoundnbt.store("block_pos", BlockPos.CODEC, BlockPos.containing(vector3d1));
                }
                createEntityIgnoreException(problemReporter, worldIn, compoundnbt).ifPresent((p_242927_6_) -> {
                    float f = p_242927_6_.mirror(placementIn.getMirror());
                    f = f + (p_242927_6_.getYRot() - p_242927_6_.rotate(placementIn.getRotation()));
                    p_242927_6_.snapTo(vector3d1.x, vector3d1.y, vector3d1.z, f, p_242927_6_.getXRot());
                    p_242927_6_.setYBodyRot(f);
                    p_242927_6_.setYHeadRot(f);
                    if (placementIn.shouldFinalizeEntities() && p_242927_6_ instanceof Mob) {
                        ((Mob) p_242927_6_).finalizeSpawn(worldIn, worldIn.getCurrentDifficultyAt(BlockPos.containing(vector3d1)), EntitySpawnReason.STRUCTURE, (SpawnGroupData) null);
                    }
                    worldIn.addFreshEntityWithPassengers(p_242927_6_);
                    if (spawnedEntities != null) spawnedEntities.add(p_242927_6_);
                });
            }
        }
    }

    private static Optional<Entity> createEntityIgnoreException(ProblemReporter problemReporter, ServerLevelAccessor worldIn, CompoundTag nbt) {
        try {
            return EntityType.create(TagValueInput.create(problemReporter, worldIn.registryAccess(), nbt), worldIn.getLevel(),
                    new EntitySpawnRequest(EntitySpawnReason.STRUCTURE, false));
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
                    compoundnbt.put("nbt", template$blockinfo.nbt().copy());
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
                compoundnbt1.put("nbt", template$entityinfo.nbt.copy());
            }

            listnbt.add(compoundnbt1);
        }

        nbt.put("entities", listnbt);
        nbt.put("size", this.newIntegerList(this.size.getX(), this.size.getY(), this.size.getZ()));
        NbtUtils.addCurrentDataVersion(nbt);

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
        ListTag listnbt = compound.getListOrEmpty("size");
        this.size = new BlockPos(listnbt.getIntOr(0, 0), listnbt.getIntOr(1, 0), listnbt.getIntOr(2, 0));
        ListTag listnbt1 = compound.getListOrEmpty("blocks");
        Optional<ListTag> listnbt2 = compound.getList("palettes");
        if (listnbt2.isPresent()) {
            for (int i = 0; i < listnbt2.get().size(); ++i) {
                this.loadPalette(listnbt2.get().getListOrEmpty(i), listnbt1);
            }
        } else {
            this.loadPalette(compound.getListOrEmpty("palette"), listnbt1);
        }

        compound.getListOrEmpty("entities").compoundStream().forEach(compoundnbt -> {
            ListTag listnbt3 = compoundnbt.getListOrEmpty("pos");
            Vec3 vector3d = new Vec3(listnbt3.getDoubleOr(0, 0), listnbt3.getDoubleOr(1, 0), listnbt3.getDoubleOr(2, 0));
            ListTag listnbt4 = compoundnbt.getListOrEmpty("blockPos");
            BlockPos blockpos = new BlockPos(listnbt4.getIntOr(0, 0), listnbt4.getIntOr(1, 0), listnbt4.getIntOr(2, 0));
            compoundnbt.getCompound("nbt").ifPresent(compoundnbt1 -> this.entities.add(new StructureTemplate.StructureEntityInfo(vector3d, blockpos, compoundnbt1)));
        });

        // CAPSULE read already occupied positions when deployed
        if (compound.contains("capsule_occupiedSources")) {
            Map<BlockPos, Block> occupiedSources = new HashMap<>();
            compound.getListOrEmpty("capsule_occupiedSources").compoundStream().forEach(entry ->
                    occupiedSources.put(BlockPos.of(entry.getLongOr("pos", 0L)), Block.stateById(entry.getIntOr("blockId", 0)).getBlock()));
            this.occupiedPositions = occupiedSources;
        }
        if (this.palettes.size() == 0) {
            LOGGER.error("Template located at " + location + " might be missing or corrupted: it should have a palette tag.");
        }
    }

    private void loadPalette(ListTag palettesNBT, ListTag blocksNBT) {
        CapsuleTemplate.BasicPalette template$basicpalette = new CapsuleTemplate.BasicPalette();

        for (int i = 0; i < palettesNBT.size(); ++i) {
            template$basicpalette.addMapping(NbtUtils.readBlockState(BuiltInRegistries.BLOCK, palettesNBT.getCompoundOrEmpty(i)), i);
        }

        List<StructureTemplate.StructureBlockInfo> list2 = Lists.newArrayList();
        List<StructureTemplate.StructureBlockInfo> list = Lists.newArrayList();
        List<StructureTemplate.StructureBlockInfo> list1 = Lists.newArrayList();

        blocksNBT.compoundStream().forEach(compoundnbt -> {
            ListTag listnbt = compoundnbt.getListOrEmpty("pos");
            BlockPos blockpos = new BlockPos(listnbt.getIntOr(0, 0), listnbt.getIntOr(1, 0), listnbt.getIntOr(2, 0));
            BlockState blockstate = template$basicpalette.stateFor(compoundnbt.getIntOr("state", 0));
            CompoundTag compoundnbt1 = compoundnbt.getCompound("nbt").orElse(null);

            StructureTemplate.StructureBlockInfo template$blockinfo = new StructureTemplate.StructureBlockInfo(blockpos, blockstate, compoundnbt1);
            addToLists(template$blockinfo, list2, list, list1);
        });

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

    /**
     * Whether blueprints keep this block: blocks without block entity data are kept, the others only if they are in
     * blueprint_whitelist.json.
     */
    public static boolean isKeptInBlueprint(StructureTemplate.StructureBlockInfo block) {
        if (block.nbt() == null) return true;
        Identifier registryName = BuiltInRegistries.BLOCK.getKey(block.state().getBlock());
        return registryName != null && Config.blueprintWhitelist.containsKey(registryName.toString());
    }

    public void filterFromWhitelist(List<String> outExcluded) {
        List<StructureTemplate.StructureBlockInfo> newBlockList = this.getPalette().stream()
                .filter(b -> {
                    boolean included = isKeptInBlueprint(b);
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
                        nbt.keySet().removeIf(key -> !allowedNBT.has(key));
                    } else {
                        nbt = new CompoundTag();
                    }
                    return new StructureTemplate.StructureBlockInfo(
                            b.pos(),
                            withoutContent(b.state()),
                            nbt
                    );
                }).collect(Collectors.toList());
        getPalette().clear();
        getPalette().addAll(newBlockList);
        // remove all entities
        entities.clear();
    }

    private static BlockState withoutContent(BlockState state) {
        for (BooleanProperty property : CONTENT_PROPERTIES) {
            if (state.hasProperty(property)) state = state.setValue(property, false);
        }
        return state;
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

            try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(PROBLEM_LOGGER)) {
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
                            TagValueOutput output = TagValueOutput.createWithContext(reporter.forChild(blockentity.problemPath()), worldIn.registryAccess());
                            blockentity.saveWithId(output);
                            CompoundTag compoundnbt = output.buildResult();
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
                new AABB(Vec3.atCenterOf(startPos), Vec3.atCenterOf(endPos)),
                entity -> !(entity instanceof ItemEntity) && (!(entity instanceof LivingEntity) || (entity instanceof ArmorStand))
        );
        entities.clear();

        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(PROBLEM_LOGGER)) {
            for (Entity entity : list) {
                Vec3 vec3d = new Vec3(entity.getX() - (double) startPos.getX(), entity.getY() - (double) startPos.getY(), entity.getZ() - (double) startPos.getZ());
                TagValueOutput output = TagValueOutput.createWithContext(reporter.forChild(entity.problemPath()), entity.registryAccess());
                entity.save(output);
                CompoundTag compoundnbt = output.buildResult();
                BlockPos blockpos;
                if (entity instanceof Painting) {
                    blockpos = ((Painting) entity).getPos().subtract(startPos);
                } else {
                    blockpos = BlockPos.containing(vec3d);
                }

                this.entities.add(new StructureTemplate.StructureEntityInfo(vec3d, blockpos, compoundnbt.copy()));
            }
        }
        return list;
    }

    public static AABB transformedAxisAlignedBB(StructurePlaceSettings placementIn, AABB bb) {
        return new AABB(
               Vec3.atLowerCornerOf(calculateRelativePosition(placementIn, BlockPos.containing(bb.minX, bb.minY, bb.minZ))),
               Vec3.atLowerCornerOf(calculateRelativePosition(placementIn, BlockPos.containing(bb.maxX, bb.maxY, bb.maxZ)))
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
                List<BlockPos> list1 = Lists.newArrayListWithCapacity(placementIn.shouldApplyWaterlogging() ? list.size() : 0);
                List<Pair<BlockPos, CompoundTag>> list2 = Lists.newArrayListWithCapacity(list.size());
                int i = Integer.MAX_VALUE;
                int j = Integer.MAX_VALUE;
                int k = Integer.MAX_VALUE;
                int l = Integer.MIN_VALUE;
                int i1 = Integer.MIN_VALUE;
                int j1 = Integer.MIN_VALUE;

                try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(PROBLEM_LOGGER)) {
                    for (StructureTemplate.StructureBlockInfo template$blockinfo : processBlockInfos(this, worldIn, pos, placementIn, list)) {
                        // CAPSULE center the structure
                        BlockPos blockpos = template$blockinfo.pos().offset(recenterRotation((size.getX() - 1) / 2, placementIn));
                        if ((mutableboundingbox == null || mutableboundingbox.isInside(blockpos))
                                // CAPSULE add a condition to prevent replacement of existing content by the capsule content if the world content is not overridable
                                && (!occupiedPositions.containsKey(blockpos) || occupiedPositions.get(blockpos).defaultBlockState().is(CapsuleTags.overridable))) {
                            // CAPSULE capsule addition to allow a rollback in case of error while deploying
                            if (outSpawnedBlocks != null) outSpawnedBlocks.add(blockpos);

                            FluidState ifluidstate = placementIn.shouldApplyWaterlogging() ? worldIn.getFluidState(blockpos) : null;
                            BlockState blockstate = Services.PLATFORM.rotate(template$blockinfo.state().mirror(placementIn.getMirror()), worldIn, blockpos, placementIn.getRotation());
                            BlockEntity blockentity = worldIn.getBlockEntity(blockpos);
                            if (blockentity instanceof Clearable clearable) clearable.clearContent();
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
                                    CompoundTag templateTag = template$blockinfo.nbt() == null ? new CompoundTag() : template$blockinfo.nbt().copy();
                                    templateTag.putInt("x", blockpos.getX());
                                    templateTag.putInt("y", blockpos.getY());
                                    templateTag.putInt("z", blockpos.getZ());
                                    blockentity1.loadWithComponents(TagValueInput.create(reporter.forChild(blockentity1.problemPath()), worldIn.getLevel().registryAccess(), templateTag));
                                    blockentity1.getBlockState().mirror(placementIn.getMirror());
                                    Services.PLATFORM.rotate(blockentity1.getBlockState(), worldIn, placementIn.getRotationPivot(), placementIn.getRotation());
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

                                worldIn.updateNeighborsAt(blockpos4, blockstate3.getBlock());
                            }
                        }
                    }

                    if (!placementIn.isIgnoreEntities()) {
                        this.addEntitiesToWorld(worldIn, pos, placementIn, placementIn.getMirror(), placementIn.getRotation(), placementIn.getRotationPivot(), placementIn.getBoundingBox(), outSpawnedEntities, reporter);
                    }
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
                if (block.nbt() != null && !Config.blueprintWhitelist.containsKey(BuiltInRegistries.BLOCK.getKey(block.state().getBlock()).toString())) {
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
    // MCEdit and Schematica schematics need a block name palette (BlockIDs or SchematicaMapping), legacy numeric ids are not read.
    // Sponge schematics: https://github.com/SpongePowered/Schematic-Specification/tree/master/versions
    // Minecraft 1.12.2, the last version of MCEdit schematics, with namespaced block entity and item ids
    private static final int MCEDIT_DATA_VERSION = 1343;
    // Minecraft 1.13: Sponge v1 schematics have no DataVersion
    private static final int SPONGE_V1_DATA_VERSION = 1519;

    public void readSchematic(CompoundTag file) throws Exception {
        // Sponge v3 nests everything in a Schematic compound
        CompoundTag nbt = file.getCompound("Schematic").orElse(file);
        int version = nbt.getIntOr("Version", 0); // 0 for MCEdit
        if (version > 3) {
            throw new Exception("Schematic version >3 not supported");
        }
        if (version == 0 && !(nbt.getByteArray("Blocks").isPresent() && nbt.getByteArray("Data").isPresent())) {
            throw new Exception("Schematic: Missing data in the schematic");
        }
        int dataVersion = NbtUtils.getDataVersion(nbt, version == 0 ? MCEDIT_DATA_VERSION : SPONGE_V1_DATA_VERSION);
        CompoundTag blocks = version == 3 ? nbt.getCompoundOrEmpty("Blocks") : nbt;

        this.palettes.clear();
        this.palettes.add(new Palette(new ArrayList<>()));
        this.entities.clear();

        int width = nbt.getShortOr("Width", (short) 0);
        int height = nbt.getShortOr("Height", (short) 0);
        int length = nbt.getShortOr("Length", (short) 0);
        String schematicAuthor = nbt.getCompoundOrEmpty("Metadata").getStringOr("Author", "");
        this.author = schematicAuthor.isEmpty() ? "?" : schematicAuthor;

        BlockState[] palette = version == 0 ? readMCEditPalette(nbt) : readSpongePalette(blocks.getCompoundOrEmpty("Palette"));
        if (palette == null || palette.length == 0) {
            throw new Exception("Schematic: Failed to read the block palette, see logs");
        }
        BlockState[] blocksById = version == 0 ? getMCEditBlocks(nbt, palette) : getSpongeBlocks(blocks.getByteArray(version == 3 ? "Data" : "BlockData").orElse(new byte[0]), palette);
        if (blocksById == null) {
            throw new Exception("Schematic: Failed to read the block stats, see logs");
        }

        Map<BlockPos, CompoundTag> blockEntities = getSchematicBlockEntities(nbt, blocks, version, dataVersion);
        readSchematicEntities(nbt, version, dataVersion);

        int index = 0;
        for (int y = 0; y < height; ++y) {
            for (int z = 0; z < length; ++z) {
                for (int x = 0; x < width && index < blocksById.length; ++x, index++) {
                    BlockState state = blocksById[index];
                    if (state != null && !state.isAir()) {
                        BlockPos pos = new BlockPos(x, y, z);
                        getPalette().add(new StructureTemplate.StructureBlockInfo(pos, state, blockEntities.get(pos)));
                    }
                }
            }
        }
        int size = Math.max(width, Math.max(height, length));
        if (size % 2 == 0) size++;
        this.size = new BlockPos(size, size, size);
    }

    /**
     * Block entity data by position, with its id and updated to the current Minecraft version.
     */
    private static Map<BlockPos, CompoundTag> getSchematicBlockEntities(CompoundTag nbt, CompoundTag blocks, int version, int dataVersion) {
        Map<BlockPos, CompoundTag> blockEntities = new HashMap<>();
        ListTag tagList = version == 3 ? blocks.getListOrEmpty("BlockEntities")
                : nbt.getListOrEmpty(version == 2 ? "BlockEntities" : "TileEntities");
        for (CompoundTag entry : tagList.compoundStream().toList()) {
            CompoundTag data = version == 3 ? entry.getCompoundOrEmpty("Data").copy() : entry.copy();
            BlockPos pos;
            if (version == 0) {
                pos = new BlockPos(entry.getIntOr("x", 0), entry.getIntOr("y", 0), entry.getIntOr("z", 0));
                data.remove("x");
                data.remove("y");
                data.remove("z");
            } else {
                int[] positions = entry.getIntArray("Pos").orElse(new int[0]);
                pos = new BlockPos(positions[0], positions[1], positions[2]);
                data.remove("Pos");
                data.remove("Id");
                data.remove("ContentVersion");
                data.putString("id", entry.getStringOr("Id", ""));
            }
            blockEntities.put(pos, updateToCurrentVersion(References.BLOCK_ENTITY, data, dataVersion));
        }
        return blockEntities;
    }

    private void readSchematicEntities(CompoundTag nbt, int version, int dataVersion) {
        // WorldEdit writes Sponge v2 entity positions in world coordinates and the schematic's world position as Offset
        int[] offset = version == 2 ? nbt.getIntArray("Offset").filter(o -> o.length == 3).orElse(new int[3]) : new int[3];
        ListTag tagList = nbt.getListOrEmpty("Entities");
        for (CompoundTag entry : tagList.compoundStream().toList()) {
            CompoundTag data = version == 3 ? entry.getCompoundOrEmpty("Data").copy() : entry.copy();
            ListTag posList = (version == 3 ? entry : data).getListOrEmpty("Pos");
            Vec3 pos = new Vec3(posList.getDoubleOr(0, 0) - offset[0], posList.getDoubleOr(1, 0) - offset[1], posList.getDoubleOr(2, 0) - offset[2]);
            data.remove("Pos");
            if (version > 0) {
                data.remove("Id");
                data.putString("id", entry.getStringOr("Id", ""));
            }
            data = updateToCurrentVersion(References.ENTITY, data, dataVersion);
            this.entities.add(new StructureTemplate.StructureEntityInfo(pos, BlockPos.containing(pos), data));
        }
    }

    private static CompoundTag updateToCurrentVersion(DSL.TypeReference type, CompoundTag data, int dataVersion) {
        int currentVersion = SharedConstants.getCurrentVersion().dataVersion().version();
        return (CompoundTag) DataFixers.getDataFixer().update(type, new Dynamic<>(NbtOps.INSTANCE, data), dataVersion, currentVersion).getValue();
    }

    /**
     * Sponge block data: one palette index per block, as var ints.
     */
    private static BlockState[] getSpongeBlocks(byte[] data, BlockState[] palette) {
        List<BlockState> states = new ArrayList<>();
        int i = 0;
        while (i < data.length) {
            int value = 0;
            int varintLength = 0;
            while (true) {
                value |= (data[i] & 127) << (varintLength++ * 7);
                if (varintLength > 5) {
                    throw new RuntimeException("VarInt too big (probably corrupted data)");
                }
                if ((data[i++] & 128) != 128) break;
            }
            if (value < 0 || value >= palette.length) {
                LOGGER.warn("Schematic: palette index {} out of bounds (palette size {}), skipping block at index {}", value, palette.length, states.size());
                states.add(null);
            } else {
                states.add(palette[value]);
            }
        }
        return states.toArray(new BlockState[0]);
    }

    private BlockState[] readSpongePalette(CompoundTag paletteTag) {
        BlockState[] palette = new BlockState[paletteTag.keySet().stream().mapToInt(key -> paletteTag.getIntOr(key, 0)).max().orElse(-1) + 1];
        Arrays.fill(palette, Blocks.AIR.defaultBlockState());
        for (String key : paletteTag.keySet()) {
            palette[paletteTag.getIntOr(key, 0)] = parseBlockState(key);
        }
        return palette;
    }

    private static BlockState[] getMCEditBlocks(CompoundTag nbt, BlockState[] palette) {
        byte[] blockIdsByte = nbt.getByteArray("Blocks").orElse(new byte[0]);
        final int numBlocks = blockIdsByte.length;
        BlockState[] blocksById = new BlockState[numBlocks];
        Optional<byte[]> addBlocks = nbt.getByteArray("AddBlocks");
        if (addBlocks.isPresent()) {
            byte[] add = addBlocks.get();
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
        else if (nbt.getByteArray("Add").isPresent()) {
            LOGGER.error("Schematic: Old Schematica format detected, not implemented");
            return null;
        }
        // Block ids below 256
        else {
            for (int i = 0; i < numBlocks; i++) {
                BlockState block = palette[((int) blockIdsByte[i]) & 0xFF];
                blocksById[i] = block;
            }
        }
        return blocksById;
    }

    private BlockState[] readMCEditPalette(CompoundTag nbt) {
        BlockState[] palette = new BlockState[4096];
        Arrays.fill(palette, Blocks.AIR.defaultBlockState());
        CompoundTag tag = nbt.getCompound("BlockIDs").orElseGet(() -> nbt.getCompoundOrEmpty("SchematicaMapping"));
        if (tag.isEmpty()) {
            LOGGER.error("Schematic: no BlockIDs or SchematicaMapping palette, legacy numeric block ids are not supported.");
            return null;
        }
        boolean mcedit2 = nbt.getCompound("BlockIDs").isPresent();
        for (String key : tag.keySet()) {
            int id;
            String name;
            try {
                id = mcedit2 ? Integer.parseInt(key) : tag.getShortOr(key, (short) 0);
                name = mcedit2 ? tag.getStringOr(key, "") : key;
            } catch (NumberFormatException e) {
                LOGGER.error("Schematic: Invalid ID '{}' (not a number) in MCEdit2 palette", key);
                continue;
            }
            if (id < 0 || id >= palette.length) {
                LOGGER.error("Schematic: Invalid ID '{}' in the palette for block '{}', max = 4095", id, name);
                return null;
            }
            palette[id] = parseBlockState(name);
        }
        return palette;
    }

    private BlockState parseBlockState(String key) {
        try {
            BlockState state = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, key, true).blockState();
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
