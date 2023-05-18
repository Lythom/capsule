package capsule;

import capsule.items.CapsuleItem;
import capsule.plugins.securitycraft.SecurityCraftOwnerCheck;
import capsule.structure.CapsuleTemplate;
import capsule.structure.CapsuleTemplateManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecartContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static net.minecraft.nbt.Tag.TAG_COMPOUND;

@Mod.EventBusSubscriber(modid = CapsuleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class StructureSaver {
    protected static final Logger LOGGER = LogManager.getLogger(StructureSaver.class);
    public static final String BLUEPRINT_PREFIX = "b-";
    public static Map<String, CapsuleTemplateManager> CapsulesManagers = new HashMap<>();
    private static CapsuleTemplateManager RewardManager = null;
    private static final List<String> outExcluded = new ArrayList<>();

    public static CapsuleTemplateManager getRewardManager(ResourceManager resourceManager) {
        if (RewardManager == null) {
            File rewardDir = new File(Config.rewardTemplatesPath);
            if (!rewardDir.exists()) {
                rewardDir.mkdirs();
            }
            RewardManager = new CapsuleTemplateManager(resourceManager, new File("."), DataFixers.getDataFixer());
        }
        return RewardManager;
    }

    // very powerfull high level cut to prevent any drop during capturing phase
    // it is synchronized so it should not create any side effect
    private static boolean preventItemDrop = false;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void handleEntityJoinWorldEvent(final EntityJoinLevelEvent e) {
        if (preventItemDrop && e.getEntity() instanceof ItemEntity) {
            e.setCanceled(true);
        }
    }

    public static CapsuleTemplate undeploy(ServerLevel worldserver, @Nullable UUID playerID, String capsuleStructureId, BlockPos startPos, int size, List<Block> excluded,
                                           Map<BlockPos, Block> legacyItemOccupied) {

        MinecraftServer minecraftserver = worldserver.getServer();
        if (minecraftserver == null) {
            LOGGER.error("worldserver.getServer() returned null");
            return null;
        }
        List<Entity> outCapturedEntities = new ArrayList<>();

        CapsuleTemplateManager templatemanager = getTemplateManager(worldserver.getServer());
        if (templatemanager == null) {
            LOGGER.error("getTemplateManager returned null");
            return null;
        }
        CapsuleTemplate template = templatemanager.getOrCreateTemplate(new ResourceLocation(capsuleStructureId));
        Map<BlockPos, Block> occupiedPositions = template.occupiedPositions;
        if (legacyItemOccupied != null) occupiedPositions = legacyItemOccupied;
        List<BlockPos> transferedPositions = template.snapshotBlocksFromWorld(worldserver, startPos, new BlockPos(size, size, size), occupiedPositions,
                excluded, outCapturedEntities);
        template.removeOccupiedPositions();
        Player player = null;
        if (playerID != null) {
            player = worldserver.getPlayerByUUID(playerID);
            if (player != null) template.setAuthor(player.getGameProfile().getName());
        }
        boolean writingOK = templatemanager.writeToFile(new ResourceLocation(capsuleStructureId));
        if (writingOK) {
            List<BlockPos> couldNotBeRemoved = removeTransferedBlockFromWorld(transferedPositions, worldserver, player);
            for (Entity e : outCapturedEntities) {
                if (e instanceof AbstractMinecartContainer) {
                    AbstractMinecartContainer eMinecart = (AbstractMinecartContainer) e;
                    eMinecart.clearContent();
                }
                e.remove(Entity.RemovalReason.DISCARDED);
            }
            // check if some remove failed, exclude those blocks from the template.
            if (couldNotBeRemoved != null) {
                template.removeBlocks(couldNotBeRemoved, startPos);
            }
            templatemanager.writeToFile(new ResourceLocation(capsuleStructureId));

        } else {
            printWriteTemplateError(player, capsuleStructureId);
        }

        return template;

    }

    public static boolean undeployBlueprint(ServerLevel worldserver, UUID playerID, ItemStack blueprintItemStack, BlockPos startPos, int size, List<Block> excluded) {
        Pair<CapsuleTemplateManager, CapsuleTemplate> blueprint = StructureSaver.getTemplate(blueprintItemStack, worldserver);
        CapsuleTemplate blueprintTemplate = blueprint.getRight();
        if (blueprintTemplate == null) return false;

        CapsuleTemplate tempTemplate = new CapsuleTemplate();
        Map<BlockPos, Block> occupiedPositions = blueprintTemplate.occupiedPositions;
        Map<BlockPos, Block> legacyItemOccupied = CapsuleItem.getOccupiedSourcePos(blueprintItemStack);
        if (legacyItemOccupied != null) occupiedPositions = legacyItemOccupied;
        List<BlockPos> transferedPositions = tempTemplate.snapshotBlocksFromWorld(worldserver, startPos, new BlockPos(size, size, size), occupiedPositions,
                excluded, null);
        List<StructureTemplate.StructureBlockInfo> worldBlocks = tempTemplate.getPalette().stream().filter(b -> !isFlowingLiquid(b)).collect(Collectors.toList());
        List<StructureTemplate.StructureBlockInfo> blueprintBLocks = blueprintTemplate.getPalette().stream().filter(b -> !isFlowingLiquid(b)).collect(Collectors.toList());

        Player player = null;
        if (playerID != null) {
            player = worldserver.getPlayerByUUID(playerID);
        }
        // compare the 2 lists, assume they are sorted the same since the same script is used to build them.
        if (blueprintBLocks.size() != worldBlocks.size())
            return false;

        List<String> tempTemplateSorted = worldBlocks.stream()
                .map(StructureSaver::serializeComparable)
                .sorted()
                .collect(Collectors.toList());
        List<String> blueprintTemplateSorted = blueprintBLocks.stream()
                .map(StructureSaver::serializeComparable)
                .sorted()
                .collect(Collectors.toList());
        boolean blueprintMatch = IntStream.range(0, tempTemplateSorted.size())
                .allMatch(i -> tempTemplateSorted.get(i).equals(blueprintTemplateSorted.get(i)));

        blueprintMatch = blueprintMatch && worldBlocks.stream().allMatch(b -> b.nbt == null || !b.nbt.contains("Items") || b.nbt.getList("Items", TAG_COMPOUND).isEmpty());

        if (blueprintMatch) {
            blueprintTemplate.removeOccupiedPositions();
            String capsuleStructureId = CapsuleItem.getStructureName(blueprintItemStack);
            boolean written = blueprint.getLeft().writeToFile(new ResourceLocation(capsuleStructureId));
            if (written) {
                List<BlockPos> couldNotBeRemoved = removeTransferedBlockFromWorld(transferedPositions, worldserver, player);
                // check if some remove failed, it should never happen but keep it in case to prevent exploits
                if (couldNotBeRemoved != null) {
                    return false;
                }
            } else {
                printWriteTemplateError(player, capsuleStructureId);
            }
        }

        return blueprintMatch;
    }

    public static String serializeComparable(StructureTemplate.StructureBlockInfo b) {
        return b.state.getBlock().getDescriptionId()
                + "@"
                + b.state.getBlock().defaultBlockState()
                + (b.nbt == null ? "" : nbtStringNotEmpty(filterIdentityNBT(b)));
    }

    public static CompoundTag filterIdentityNBT(StructureTemplate.StructureBlockInfo b) {
        CompoundTag nbt = b.nbt.copy();
        List<String> converted = Config.getBlueprintIdentityNBT(b.state.getBlock());
        nbt.getAllKeys().removeIf(key -> converted == null || !converted.contains(key));
        return nbt;
    }

    public static String nbtStringNotEmpty(CompoundTag nbt) {
        if (nbt.isEmpty()) return "";
        return nbt.toString();
    }

    public static boolean isFlowingLiquid(StructureTemplate.StructureBlockInfo b) {
        return b.state.getBlock() instanceof LiquidBlock && b.state.getValue(LiquidBlock.LEVEL) != 0;
    }


    @Nullable
    public static CapsuleTemplateManager getTemplateManager(MinecraftServer server) {
        LevelResource folder = new LevelResource("capsules");
        Path directoryPath = server.getWorldPath(folder);

        if (!CapsulesManagers.containsKey(directoryPath.toString())) {
            File capsuleDir = directoryPath.toFile();
            capsuleDir.mkdirs();
            CapsulesManagers.put(directoryPath.toString(), new CapsuleTemplateManager(server.getResourceManager(), capsuleDir, DataFixers.getDataFixer()));
        }
        return CapsulesManagers.get(directoryPath.toString());
    }

    /**
     * Use with caution, delete the blocks at the indicated positions.
     *
     * @return list of blocks that could not be removed
     */
    public static List<BlockPos> removeTransferedBlockFromWorld(List<BlockPos> transferedPositions, ServerLevel
            world, @Nullable Player player) {

        List<BlockPos> couldNotBeRemoved = null;

        // disable tileDrop during the operation so that broken block are not
        // itemized on the ground.
        GameRules.BooleanValue entityDropsGameRule = world.getGameRules().getRule(GameRules.RULE_DOENTITYDROPS);
        GameRules.BooleanValue tileDropsGameRule = world.getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS);

        boolean flagdoEntityDrops = world.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS);
        boolean flagdoTileDrops = world.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS);
        entityDropsGameRule.set(false, world.getServer());
        tileDropsGameRule.set(false, world.getServer());
        world.restoringBlockSnapshots = true;
        StructureSaver.preventItemDrop = true;

        // delete everything that as been saved in the capsule
        try {
            for (BlockPos pos : transferedPositions) {
                BlockState b = world.getBlockState(pos);
                try {
                    // uses same mechanic for BlockEntity than net.minecraft.world.gen.feature.template.Template
                    if (playerCanRemove(world, pos, player)) {
                        BlockEntity BlockEntity = b.hasBlockEntity() ? world.getBlockEntity(pos) : null;
                        // content of TE have been snapshoted, remove the content
                        if (BlockEntity != null) {
                            Clearable.tryClear(BlockEntity);
                            world.setBlock(pos, Blocks.BARRIER.defaultBlockState(), 20); // from Template.placeInWorld
                        }

                        world.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    } else {
                        if (couldNotBeRemoved == null) couldNotBeRemoved = new ArrayList<>();
                        couldNotBeRemoved.add(pos);
                    }
                } catch (Exception e) {
                    printDeployError(player, e, "Block crashed during Capsule capture phase : couldn't be removed. Will be ignored.");
                    try {
                        world.setBlock(pos, b, 3);
                    } catch (Exception ignored) {
                    }
                    if (couldNotBeRemoved == null) couldNotBeRemoved = new ArrayList<>();
                    couldNotBeRemoved.add(pos);
                }
            }
        } finally {
            // revert rule to previous value even in case of crash
            StructureSaver.preventItemDrop = false;
            world.restoringBlockSnapshots = false;
            entityDropsGameRule.set(flagdoEntityDrops, world.getServer());
            tileDropsGameRule.set(flagdoTileDrops, world.getServer());
        }

        return couldNotBeRemoved;
    }


    public static boolean deploy(ItemStack capsule, ServerLevel playerWorld, @Nullable UUID thrower, BlockPos
            dest, List<Block> overridableBlocks, StructurePlaceSettings placementsettings) {

        Pair<CapsuleTemplateManager, CapsuleTemplate> templatepair = getTemplate(capsule, playerWorld);
        CapsuleTemplate template = templatepair.getRight();

        if (template == null) return false;

        Player player = null;
        if (thrower != null) {
            player = playerWorld.getServer().getPlayerList().getPlayer(thrower);
        }

        Map<BlockPos, Block> outOccupiedSpawnPositions = new HashMap<>();
        int size = CapsuleItem.getSize(capsule);
        // check if the destination is valid : no unoverwritable block and no entities in the way.
        List<Component> outErrors = new ArrayList<>();
        checkDestination(template, placementsettings, playerWorld, dest, size, overridableBlocks, outOccupiedSpawnPositions, outErrors);
        if (outErrors.size() > 0) {
            if (player != null) printDeployFailure(player, outErrors);
            return false;
        }

        // check if the player can place a block
        if (player != null && !playerCanPlace(playerWorld, dest, template, player, placementsettings)) {
            player.sendSystemMessage(Component.translatable("capsule.error.notAllowed"));
            return false;
        }

        final Map<BlockPos, Block> occupiedPositions = outOccupiedSpawnPositions;
        List<BlockPos> spawnedBlocks = new ArrayList<>();
        List<Entity> spawnedEntities = new ArrayList<>();

        CapsuleTemplateManager templateManager = templatepair.getLeft();
        String capsuleStructureId = CapsuleItem.getStructureName(capsule);
        template.saveOccupiedPositions(occupiedPositions);
        if (!templateManager.writeToFile(new ResourceLocation(capsuleStructureId))) {
            printWriteTemplateError(player, capsuleStructureId);
            return false;
        }

        try {
            template.spawnBlocksAndEntities(playerWorld, dest, placementsettings, occupiedPositions, overridableBlocks, spawnedBlocks, spawnedEntities);
            placePlayerOnTop(playerWorld, dest, size);

            return true;
        } catch (Exception err) {
            printDeployError(player, err, "Couldn't deploy the capsule");

            // rollback
            removeTransferedBlockFromWorld(spawnedBlocks, playerWorld, player);
            template.removeOccupiedPositions();
            if (!templateManager.writeToFile(new ResourceLocation(capsuleStructureId))) {
                printWriteTemplateError(player, capsuleStructureId);
            }
            for (Entity e : spawnedEntities) {
                if (e instanceof AbstractMinecartContainer) {
                    AbstractMinecartContainer eMinecart = (AbstractMinecartContainer) e;
                    eMinecart.clearContent();
                }
                e.remove(Entity.RemovalReason.DISCARDED);
            }
            return false;
        }
    }

    private static void printDeployFailure(Player player, List<Component> outErrors) {
        MutableComponent msg = Component.literal("");
        for (int i = 0, outErrorsSize = outErrors.size(); i < outErrorsSize; i++) {
            Component outError = outErrors.get(i);
            msg.append(outError);
            if (i < outErrors.size() - 1) msg.append("\n");
        }
        player.sendSystemMessage(msg);
    }

    public static void placePlayerOnTop(ServerLevel playerWorld, BlockPos dest, int size) {
        // Players don't block deployment, instead they are pushed up if they would suffocate
        List<LivingEntity> players = playerWorld.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(dest.getX(), dest.getY(), dest.getZ(), dest.getX() + size, dest.getY() + size, dest.getZ() + size),
                entity -> (entity instanceof Player)
        );
        for (LivingEntity p : players) {
            for (int y = 0; y < size; y++) {
                if (!checkBlockCollision(p)) {
                    p.moveTo(p.getX(), p.getY() + 1, p.getZ());
                }
            }
        }
    }

    private static boolean checkBlockCollision(Entity entity) {
        return BlockPos.betweenClosedStream(entity.getBoundingBox()).allMatch(p -> {
            BlockState state = entity.level.getBlockState(p);
            return state.isAir();
        });
    }

    public static void printDeployError(@Nullable Player player, Exception err, String s) {
        LOGGER.error(s, err);
        if (player != null) {
            player.sendSystemMessage(Component.translatable("capsule.error.technicalError"));
        }
    }

    public static void printWriteTemplateError(@Nullable Player player, String capsuleStructureId) {
        LOGGER.error("Couldn't write template " + capsuleStructureId);
        if (player != null) {
            player.sendSystemMessage(Component.translatable("capsule.error.technicalError"));
        }
    }

    /**
     * Simulate a block placement at all positions to see if anythink revoke the placement of block by the player.
     */
    private static boolean playerCanPlace(ServerLevel worldserver, BlockPos dest, CapsuleTemplate
            template, Player player, StructurePlaceSettings placementsettings) {
        if (player != null) {
            List<BlockPos> expectedOut = template.calculateDeployPositions(worldserver, dest, placementsettings);
            for (BlockPos blockPos : expectedOut) {
                if (blockPos.getY() >= worldserver.getMaxBuildHeight() || blockPos.getY() <= worldserver.getMinBuildHeight() || !isEntityPlaceEventAllowed(worldserver, blockPos, player))
                    return false;
            }
        }
        return true;
    }

    /**
     * Simulate a block placement at all positions to see if anythink revoke the placement of block by the player.
     */
    private static boolean playerCanRemove(ServerLevel worldserver, BlockPos blockPos, @Nullable Player player) {
        if (player != null) {
            return isEntityPlaceEventAllowed(worldserver, blockPos, player)
                    && SecurityCraftOwnerCheck.canTakeBlock(worldserver, blockPos, player);
        }
        return true;
    }

    private static boolean isEntityPlaceEventAllowed(ServerLevel worldserver, BlockPos blockPos, @Nullable Player player) {
        BlockSnapshot blocksnapshot = BlockSnapshot.create(worldserver.dimension(), worldserver, blockPos);
        BlockEvent.EntityPlaceEvent event = new BlockEvent.EntityPlaceEvent(blocksnapshot, Blocks.DIRT.defaultBlockState(), player);
        MinecraftForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }

    public static Pair<CapsuleTemplateManager, CapsuleTemplate> getTemplate(ItemStack capsule, ServerLevel
            playerWorld) {
        Pair<CapsuleTemplateManager, CapsuleTemplate> template = null;

        boolean isReward = CapsuleItem.isReward(capsule);
        String structureName = CapsuleItem.getStructureName(capsule);
        if (isReward || structureName.startsWith("config/") && CapsuleItem.isBlueprint(capsule)) {
            template = getTemplateForReward(playerWorld.getServer(), structureName);
        } else {
            template = getTemplateForCapsule(playerWorld, structureName);
        }
        return template;
    }

    public static Pair<CapsuleTemplateManager, CapsuleTemplate> getTemplateForCapsule(ServerLevel
                                                                                              playerWorld, String structurePath) {
        CapsuleTemplateManager templatemanager = getTemplateManager(playerWorld.getServer());
        if (templatemanager == null || net.minecraft.util.StringUtil.isNullOrEmpty(structurePath))
            return Pair.of(null, null);

        String path = structurePath.toLowerCase();
        CapsuleTemplate template = templatemanager.getOrCreateTemplate(new ResourceLocation(path));
        return Pair.of(templatemanager, template);
    }

    public static Pair<CapsuleTemplateManager, CapsuleTemplate> getTemplateForReward(MinecraftServer server, String
            structurePath) {
        CapsuleTemplateManager templatemanager = getRewardManager(server.getResourceManager());
        if (templatemanager == null || net.minecraft.util.StringUtil.isNullOrEmpty(structurePath))
            return Pair.of(null, null);

        String path = structurePath.toLowerCase();
        CapsuleTemplate template = templatemanager.getOrCreateTemplate(new ResourceLocation(path));
        return Pair.of(templatemanager, template);
    }

    /**
     * Check whether a merge can be done at the destination
     *
     * @param outOccupiedPositions Output param, the positions occupied a destination that will
     *                             have to be ignored on
     * @return List<BlockPos> occupied but not blocking positions
     */
    public static void checkDestination(CapsuleTemplate template, StructurePlaceSettings placementIn, ServerLevel
            destWorld, BlockPos destOriginPos, int size,
                                        List<Block> overridable, Map<BlockPos, Block> outOccupiedPositions, List<Component> outErrors) {

        BlockState air = Blocks.AIR.defaultBlockState();

        List<StructureTemplate.StructureBlockInfo> srcblocks = template.getPalette();

        Map<BlockPos, StructureTemplate.StructureBlockInfo> blockInfoByPosition = new HashMap<>();
        for (StructureTemplate.StructureBlockInfo template$blockinfo : srcblocks) {
            BlockPos blockpos = CapsuleTemplate.calculateRelativePosition(placementIn, template$blockinfo.pos).offset(destOriginPos).offset(CapsuleTemplate.recenterRotation((size - 1) / 2, placementIn));
            blockInfoByPosition.put(blockpos, template$blockinfo);
        }

        // check the destination is ok for every block of the template
        for (int y = size - 1; y >= 0; y--) {
            for (int x = 0; x < size; x++) {
                for (int z = 0; z < size; z++) {

                    BlockPos destPos = destOriginPos.offset(x, y, z);
                    StructureTemplate.StructureBlockInfo srcInfo = blockInfoByPosition.get(destPos);
                    BlockState templateBlockState = air;
                    if (srcInfo != null) {
                        templateBlockState = srcInfo.state;
                    }

                    if (!destWorld.hasChunkAt(destPos)) {
                        outErrors.add(Component.translatable("capsule.error.areaNotLoaded"));
                        return;
                    }
                    BlockState worldDestState = destWorld.getBlockState(destPos);

                    boolean worldDestOccupied = (!worldDestState.isAir() && !overridable.contains(worldDestState.getBlock()));
                    if (!worldDestState.isAir() && outOccupiedPositions != null) {
                        outOccupiedPositions.put(destPos, worldDestState.getBlock());
                    }

                    boolean srcOccupied = (!templateBlockState.isAir() && !overridable.contains(templateBlockState.getBlock()));

                    List<LivingEntity> entities = destWorld.getEntitiesOfClass(
                            LivingEntity.class,
                            new AABB(destPos.getX(), destPos.getY(), destPos.getZ(), destPos.getX() + 1, destPos.getY() + 1, destPos.getZ() + 1),
                            entity -> !(entity instanceof Player)
                    );


                    // if destination is occupied, and source is neither
                    // excluded from transportation, nor can't be overriden by
                    // destination, then the merge can't be done.
                    if (entities.size() > 0 && srcOccupied) {
                        boolean found = false;
                        for (Object e : entities) {
                            Entity entity = (Entity) e;
                            if (entity != null) {
                                outErrors.add(Component.translatable("capsule.error.cantMergeWithDestinationEntity", entity.getDisplayName()));
                                found = true;
                            }
                        }
                        if (!found)
                            outErrors.add(Component.translatable("capsule.error.cantMergeWithDestinationEntity", "???"));

                        return;
                    }
                    if (worldDestOccupied && !overridable.contains(templateBlockState.getBlock())) {
                        outErrors.add(Component.translatable("capsule.error.cantMergeWithDestination", destPos.toString()));
                        return;
                    }
                }
            }
        }
    }


    /**
     * Give an id to the capsule that has not already been taken. Ensure that content is not overwritten if capsuleData is removed.
     */
    public static String getUniqueName(ServerLevel playerWorld, String player) {
        CapsuleSavedData csd = getCapsuleSavedData(playerWorld.getServer().overworld());
        String p = player.toLowerCase().codePoints()
                .filter(c -> ResourceLocation.isAllowedInResourceLocation((char) c))
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
        String capsuleID = "c-" + p + "-" + csd.getNextCount();
        CapsuleTemplateManager templatemanager = getTemplateManager(playerWorld.getServer().overworld().getServer());
        if (templatemanager == null) {
            LOGGER.error("getTemplateManager returned null");
            return "cexception-" + p + "-" + csd.getNextCount();
        }
        while (templatemanager.getTemplate(new ResourceLocation(capsuleID)) != null) {
            capsuleID = "c-" + p + "-" + csd.getNextCount();
        }

        return capsuleID;
    }

    /**
     * Give an id to the capsule that has not already been taken. Ensure that content is not overwritten if capsuleData is removed.
     */
    public static String getBlueprintUniqueName(ServerLevel world) {
        CapsuleSavedData csd = getCapsuleSavedData(world);
        String capsuleID = BLUEPRINT_PREFIX + csd.getNextCount();
        CapsuleTemplateManager templatemanager = getTemplateManager(world.getServer());
        if (templatemanager == null) {
            LOGGER.error("getTemplateManager returned null");
            return "bexception-" + csd.getNextCount();
        }
        while (templatemanager.getTemplate(new ResourceLocation(capsuleID)) != null) {
            capsuleID = BLUEPRINT_PREFIX + csd.getNextCount();
        }

        return capsuleID;
    }

    public static boolean copyFromCapsuleTemplate(ItemStack capsule, String destinationStructureName, CapsuleTemplateManager
            destManager, ServerLevel worldServer, boolean onlyBlocks, List<String> outExcluded) {
        CompoundTag srcTemplateData = getTemplateNBTData(capsule, worldServer);
        if (srcTemplateData == null) return false; // capsule template not found
        return duplicateTemplate(srcTemplateData, destinationStructureName, destManager, onlyBlocks, outExcluded);

    }

    public static boolean duplicateTemplate(CompoundTag templateData, String destinationStructureName, CapsuleTemplateManager destManager, MinecraftServer server) {
        return duplicateTemplate(templateData, destinationStructureName, destManager, false, null);
    }

    public static boolean duplicateTemplate(CompoundTag templateData, String destinationStructureName, CapsuleTemplateManager destManager, boolean onlyWhitelisted, List<String> outExcluded) {
        // create a destination template
        String sanitized = destinationStructureName.toLowerCase();
        ResourceLocation destinationLocation = new ResourceLocation(sanitized);
        CapsuleTemplate destTemplate = destManager.getOrCreateTemplate(destinationLocation);
        // populate template from source data
        destTemplate.load(templateData, destinationLocation.toString());
        // empty occupied position, it makes no sense for a new template to copy those situational data
        destTemplate.occupiedPositions = null;
        // remove all tile entities
        if (onlyWhitelisted) {
            destTemplate.filterFromWhitelist(outExcluded);
        }
        // write the new template
        return destManager.writeToFile(destinationLocation);
    }

    /**
     * Extract the template NBTData from a capsule
     */
    public static CompoundTag getTemplateNBTData(ItemStack capsule, ServerLevel worldServer) {
        return getTemplateNBTData(StructureSaver.getTemplate(capsule, worldServer).getRight());
    }

    public static CompoundTag getTemplateNBTData(String path, ServerLevel worldServer) {
        Pair<CapsuleTemplateManager, CapsuleTemplate> sourcetemplatepair;
        if (path.startsWith(Config.rewardTemplatesPath) || path.startsWith("config/")) {
            sourcetemplatepair = StructureSaver.getTemplateForReward(worldServer.getServer(), path);
        } else {
            sourcetemplatepair = StructureSaver.getTemplateForCapsule(worldServer, path);
        }
        return getTemplateNBTData(sourcetemplatepair.getRight());
    }

    public static CompoundTag getTemplateNBTData(CapsuleTemplate template) {
        if (template == null) return null;
        CompoundTag data = new CompoundTag();
        template.save(data);
        return data;
    }

    /**
     * Get the Capsule saving tool that remembers last capsule id.
     */
    public static CapsuleSavedData getCapsuleSavedData(ServerLevel capsuleWorld) {
        return capsuleWorld.getDataStorage().computeIfAbsent(CapsuleSavedData::load, CapsuleSavedData::new, "capsuleData");
    }

    @Nullable
    public static String createBlueprintTemplate(String srcStructurePath, ItemStack destCapsule, ServerLevel worldServer, Player
            playerIn) {
        if (worldServer == null) {
            LOGGER.error("worldServer is null");
            return null;
        }

        String destStructureName = getBlueprintUniqueName(worldServer) + "-" + srcStructurePath.replace("/", "_");

        CapsuleTemplateManager templateManager = getTemplateManager(worldServer.getServer());
        outExcluded.clear();
        boolean created = templateManager != null && duplicateTemplate(
                getTemplateNBTData(srcStructurePath, worldServer),
                destStructureName,
                templateManager,
                true,
                outExcluded
        );

        // try to cleanup previous template to save disk space on the long run
        if (destCapsule.getTag() != null && destCapsule.getTag().contains("prevStructureName")) {
            if (templateManager != null) {
                try {
                    templateManager.deleteTemplate(new ResourceLocation(destCapsule.getTag().getString("prevStructureName")));
                } catch (Exception e) {
                    LOGGER.error(e);
                }
            }
        }

        if (!created && playerIn != null) {
            playerIn.sendSystemMessage(Component.translatable("capsule.error.blueprintCreationError"));
        }
        if (outExcluded.size() > 0 && playerIn != null) {
            playerIn.sendSystemMessage(Component.translatable("capsule.error.blueprintExcluded", "\n* " + String.join("\n* ", outExcluded)));
        }
        return destStructureName;
    }

    public static class ItemStackKey implements Comparable<ItemStackKey> {
        public ItemStack itemStack;

        public ItemStackKey(ItemStack itemStack) {
            this.itemStack = itemStack;
        }

        public boolean equals(Object someOther) {
            if (!(someOther instanceof ItemStackKey)) return false;
            final ItemStack otherStack = ((ItemStackKey) someOther).itemStack;
            return otherStack.sameItem(this.itemStack) && (!otherStack.hasTag() && !this.itemStack.hasTag() || otherStack.getTag().equals(this.itemStack.getTag()));
        }

        public int hashCode() {
            int val = itemStack.getItem().hashCode() * 29 + CapsuleItem.getState(itemStack).getValue();
            return val;
        }


        @Override
        public int compareTo(ItemStackKey o) {
            if (o == null || o.itemStack == null) return 1;
            return this.equals(o) ? 0 : serializeItemStack(this.itemStack).compareTo(serializeItemStack(o.itemStack));
        }

        public static String serializeItemStack(ItemStack itemstack) {
            return itemstack.getItem().getDescriptionId()
                    + "@"
                    + CapsuleItem.getState(itemstack);
        }
    }

}
