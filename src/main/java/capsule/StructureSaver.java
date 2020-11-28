package capsule;

import capsule.items.CapsuleItem;
import capsule.structure.CapsuleTemplate;
import capsule.structure.CapsuleTemplateManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.minecart.ContainerMinecartEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IClearable;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.datafix.DataFixesManager;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.GameRules;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.world.BlockEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StructureSaver {

    protected static final Logger LOGGER = LogManager.getLogger(StructureSaver.class);
    public static final String BLUEPRINT_PREFIX = "b-";
    public static Map<String, CapsuleTemplateManager> CapsulesManagers = new HashMap<>();
    private static CapsuleTemplateManager RewardManager = null;
    private static final List<String> outExcluded = new ArrayList<>();

    public static CapsuleTemplateManager getRewardManager(MinecraftServer server) {
        if (RewardManager == null) {
            RewardManager = new CapsuleTemplateManager(server, server.getDataDirectory(), DataFixesManager.getDataFixer());
            File rewardDir = new File(Config.rewardTemplatesPath);
            if (!rewardDir.exists()) {
                rewardDir.mkdirs();
            }
        }
        return RewardManager;
    }

    public static CapsuleTemplate undeploy(ServerWorld worldserver, UUID playerID, String capsuleStructureId, BlockPos startPos, int size, List<Block> excluded,
                                           Map<BlockPos, Block> legacyItemOccupied) {

        MinecraftServer minecraftserver = worldserver.getServer();
        if (minecraftserver == null) {
            LOGGER.error("worldserver.getServer() returned null");
            return null;
        }
        List<Entity> outCapturedEntities = new ArrayList<>();

        CapsuleTemplateManager templatemanager = getTemplateManager(worldserver);
        if (templatemanager == null) {
            LOGGER.error("getTemplateManager returned null");
            return null;
        }
        CapsuleTemplate template = templatemanager.getTemplateDefaulted(new ResourceLocation(capsuleStructureId));
        Map<BlockPos, Block> occupiedPositions = template.occupiedPositions;
        if (legacyItemOccupied != null) occupiedPositions = legacyItemOccupied;
        List<BlockPos> transferedPositions = template.snapshotBlocksFromWorld(worldserver, startPos, new BlockPos(size, size, size), occupiedPositions,
                excluded, outCapturedEntities);
        template.removeOccupiedPositions();
        PlayerEntity player = null;
        if (playerID != null) {
            player = worldserver.getPlayerByUuid(playerID);
            if (player != null) template.setAuthor(player.getGameProfile().getName());
        }
        boolean writingOK = templatemanager.writeToFile(new ResourceLocation(capsuleStructureId));
        if (writingOK) {
            List<BlockPos> couldNotBeRemoved = removeTransferedBlockFromWorld(transferedPositions, worldserver, player);
            for (Entity e : outCapturedEntities) {
                if (e instanceof ContainerMinecartEntity) {
                    ContainerMinecartEntity eMinecart = (ContainerMinecartEntity) e;
                    eMinecart.dropContentsWhenDead(false);
                }
                e.remove();
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

    public static boolean undeployBlueprint(ServerWorld worldserver, UUID playerID, ItemStack blueprintItemStack, BlockPos startPos, int size, List<Block> excluded) {

        MinecraftServer minecraftserver = worldserver.getServer();
        if (minecraftserver == null) return false;

        Pair<CapsuleTemplateManager, CapsuleTemplate> blueprint = StructureSaver.getTemplate(blueprintItemStack, worldserver);
        CapsuleTemplate blueprintTemplate = blueprint.getRight();
        if (blueprintTemplate == null) return false;

        CapsuleTemplate tempTemplate = new CapsuleTemplate();
        Map<BlockPos, Block> occupiedPositions = blueprintTemplate.occupiedPositions;
        Map<BlockPos, Block> legacyItemOccupied = CapsuleItem.getOccupiedSourcePos(blueprintItemStack);
        if (legacyItemOccupied != null) occupiedPositions = legacyItemOccupied;
        List<BlockPos> transferedPositions = tempTemplate.snapshotBlocksFromWorld(worldserver, startPos, new BlockPos(size, size, size), occupiedPositions,
                excluded, null);
        List<Template.BlockInfo> worldBlocks = tempTemplate.getBlocks().stream().filter(b -> !isFlowingLiquid(b)).collect(Collectors.toList());
        List<Template.BlockInfo> blueprintBLocks = blueprintTemplate.getBlocks().stream().filter(b -> !isFlowingLiquid(b)).collect(Collectors.toList());

        PlayerEntity player = null;
        if (playerID != null) {
            player = worldserver.getPlayerByUuid(playerID);
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

        blueprintMatch = blueprintMatch && worldBlocks.stream().allMatch(b -> b.nbt == null || !b.nbt.contains("Items") || b.nbt.getList("Items", Constants.NBT.TAG_COMPOUND).isEmpty());

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

    public static String serializeComparable(Template.BlockInfo b) {
        return b.state.getBlock().getTranslationKey()
                + "@"
                + b.state.getBlock().getDefaultState()
                + (b.nbt == null ? "" : nbtStringNotEmpty(filterIdentityNBT(b)));
    }

    public static CompoundNBT filterIdentityNBT(Template.BlockInfo b) {
        CompoundNBT nbt = b.nbt.copy();
        List<String> converted = Config.getBlueprintIdentityNBT(b.state.getBlock());
        nbt.keySet().removeIf(key -> converted == null || !converted.contains(key));
        return nbt;
    }

    public static String nbtStringNotEmpty(CompoundNBT nbt) {
        if (nbt.isEmpty()) return "";
        return nbt.toString();
    }

    public static boolean isFlowingLiquid(Template.BlockInfo b) {
        return b.state.getBlock() instanceof FlowingFluidBlock && b.state.get(FlowingFluidBlock.LEVEL) != 0;
    }


    @Nullable
    public static CapsuleTemplateManager getTemplateManager(ServerWorld world) {
        if (world == null) return null;
        File directory = world.getSaveHandler().getWorldDirectory();
        String directoryPath = directory.getPath();

        if (!CapsulesManagers.containsKey(directoryPath)) {
            File capsuleDir = new File(directory, "generated/capsules/structures");
            capsuleDir.mkdirs();
            CapsulesManagers.put(directoryPath, new CapsuleTemplateManager(world.getServer(), capsuleDir, DataFixesManager.getDataFixer()));
        }
        return CapsulesManagers.get(directoryPath);
    }

    /**
     * Use with caution, delete the blocks at the indicated positions.
     *
     * @return list of blocks that could not be removed
     */
    public static List<BlockPos> removeTransferedBlockFromWorld(List<BlockPos> transferedPositions, ServerWorld
            world, PlayerEntity player) {

        List<BlockPos> couldNotBeRemoved = null;

        // disable tileDrop during the operation so that broken block are not
        // itemized on the ground.
        GameRules.BooleanValue entityDropsGameRule = world.getGameRules().get(GameRules.DO_ENTITY_DROPS);
        GameRules.BooleanValue tileDropsGameRule = world.getGameRules().get(GameRules.DO_TILE_DROPS);

        boolean flagdoEntityDrops = world.getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS);
        boolean flagdoTileDrops = world.getGameRules().getBoolean(GameRules.DO_TILE_DROPS);
        entityDropsGameRule.set(false, world.getServer());
        tileDropsGameRule.set(false, world.getServer());
        world.restoringBlockSnapshots = true;

        // delete everything that as been saved in the capsule
        for (BlockPos pos : transferedPositions) {
            BlockState b = world.getBlockState(pos);
            try {
                // uses same mechanic for TileEntity than net.minecraft.world.gen.feature.template.Template
                if (playerCanRemove(world, pos, player)) {
                    TileEntity tileentity = b.hasTileEntity() ? world.getTileEntity(pos) : null;
                    // content of TE have been snapshoted, remove the content
                    if (tileentity != null) {
                        IClearable.clearObj(tileentity);
                        world.setBlockState(pos, Blocks.BARRIER.getDefaultState(), 20); // from Template.addBlocksToWorld
                    }

                    world.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
                } else {
                    if (couldNotBeRemoved == null) couldNotBeRemoved = new ArrayList<>();
                    couldNotBeRemoved.add(pos);
                }
            } catch (Exception e) {
                printDeployError(player, e, "Block crashed during Capsule capture phase : couldn't be removed. Will be ignored.");
                try {
                    world.setBlockState(pos, b);
                } catch (Exception ignored) {
                }
                if (couldNotBeRemoved == null) couldNotBeRemoved = new ArrayList<>();
                couldNotBeRemoved.add(pos);
            }
        }

        // revert rule to previous value even in case of crash
        world.restoringBlockSnapshots = false;
        entityDropsGameRule.set(flagdoEntityDrops, world.getServer());
        tileDropsGameRule.set(flagdoTileDrops, world.getServer());
        return couldNotBeRemoved;
    }


    public static boolean deploy(ItemStack capsule, ServerWorld playerWorld, UUID thrower, BlockPos
            dest, List<Block> overridableBlocks, List<UUID> outEntityBlocking, PlacementSettings placementsettings) {

        Pair<CapsuleTemplateManager, CapsuleTemplate> templatepair = getTemplate(capsule, playerWorld);
        CapsuleTemplate template = templatepair.getRight();

        if (template == null) return false;

        PlayerEntity player = null;
        if (thrower != null) {
            player = playerWorld.getPlayerByUuid(thrower);
        }

        Map<BlockPos, Block> outOccupiedSpawnPositions = new HashMap<>();
        int size = CapsuleItem.getSize(capsule);
        // check if the destination is valid : no unoverwritable block and no entities in the way.
        List<ITextComponent> outErrors = new ArrayList<>();
        checkDestination(template, placementsettings, playerWorld, dest, size, overridableBlocks, outOccupiedSpawnPositions, outErrors);
        if (outErrors.size() > 0) {
            printDeployFailure(player, outErrors);
            return false;
        }

        // check if the player can place a block
        if (player != null && !playerCanPlace(playerWorld, dest, template, player, placementsettings)) {
            player.sendMessage(new TranslationTextComponent("capsule.error.notAllowed"));
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
                if (e instanceof ContainerMinecartEntity) {
                    ContainerMinecartEntity eMinecart = (ContainerMinecartEntity) e;
                    eMinecart.dropContentsWhenDead(false);
                }
                e.remove();
            }
            return false;
        }
    }

    private static void printDeployFailure(PlayerEntity player, List<ITextComponent> outErrors) {
        StringTextComponent msg = new StringTextComponent("");
        for (int i = 0, outErrorsSize = outErrors.size(); i < outErrorsSize; i++) {
            ITextComponent outError = outErrors.get(i);
            msg.appendSibling(outError);
            if (i < outErrors.size() - 1) msg.appendText("\n");
        }
        player.sendMessage(msg);
    }

    public static void placePlayerOnTop(ServerWorld playerWorld, BlockPos dest, int size) {
        // Players don't block deployment, instead they are pushed up if they would suffocate
        List<LivingEntity> players = playerWorld.getEntitiesWithinAABB(
                LivingEntity.class,
                new AxisAlignedBB(dest.getX(), dest.getY(), dest.getZ(), dest.getX() + size, dest.getY() + size, dest.getZ() + size),
                entity -> (entity instanceof PlayerEntity)
        );
        for (LivingEntity p : players) {
            for (int y = 0; y < size; y++) {
                if (playerWorld.checkBlockCollision(p.getBoundingBox())) {
                    p.setPositionAndUpdate(p.getPosX(), p.getPosY() + 1, p.getPosZ());
                }
            }
        }
    }

    public static void printDeployError(PlayerEntity player, Exception err, String s) {
        LOGGER.error(s, err);
        if (player != null) {
            player.sendMessage(new TranslationTextComponent("capsule.error.technicalError"));
        }
    }

    public static void printWriteTemplateError(PlayerEntity player, String capsuleStructureId) {
        LOGGER.error("Couldn't write template " + capsuleStructureId);
        if (player != null) {
            player.sendMessage(new TranslationTextComponent("capsule.error.technicalError"));
        }
    }

    /**
     * Simulate a block placement at all positions to see if anythink revoke the placement of block by the player.
     */
    private static boolean playerCanPlace(ServerWorld worldserver, BlockPos dest, CapsuleTemplate
            template, PlayerEntity player, PlacementSettings placementsettings) {
        if (player != null) {
            List<BlockPos> expectedOut = template.calculateDeployPositions(worldserver, dest, placementsettings);
            for (BlockPos blockPos : expectedOut) {
                if (blockPos.getY() >= worldserver.getHeight() || !isEntityPlaceEventAllowed(worldserver, blockPos, player))
                    return false;
            }
        }
        return true;
    }

    /**
     * Simulate a block placement at all positions to see if anythink revoke the placement of block by the player.
     */
    private static boolean playerCanRemove(ServerWorld worldserver, BlockPos blockPos, PlayerEntity player) {
        if (player != null) {
            return isEntityPlaceEventAllowed(worldserver, blockPos, player);
        }
        return true;
    }

    private static boolean isEntityPlaceEventAllowed(ServerWorld worldserver, BlockPos blockPos, PlayerEntity player) {
        BlockSnapshot blocksnapshot = new BlockSnapshot(worldserver, blockPos, Blocks.AIR.getDefaultState());
        BlockEvent.EntityPlaceEvent event = new BlockEvent.EntityPlaceEvent(blocksnapshot, Blocks.DIRT.getDefaultState(), player);
        MinecraftForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }

    public static Pair<CapsuleTemplateManager, CapsuleTemplate> getTemplate(ItemStack capsule, ServerWorld
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

    public static Pair<CapsuleTemplateManager, CapsuleTemplate> getTemplateForCapsule(ServerWorld
                                                                                              playerWorld, String structurePath) {
        CapsuleTemplateManager templatemanager = getTemplateManager(playerWorld);
        if (templatemanager == null || net.minecraft.util.StringUtils.isNullOrEmpty(structurePath))
            return Pair.of(null, null);

        String path = structurePath.toLowerCase();
        CapsuleTemplate template = templatemanager.getTemplateDefaulted(new ResourceLocation(path));
        return Pair.of(templatemanager, template);
    }

    public static Pair<CapsuleTemplateManager, CapsuleTemplate> getTemplateForReward(MinecraftServer server, String
            structurePath) {
        CapsuleTemplateManager templatemanager = getRewardManager(server);
        if (templatemanager == null || net.minecraft.util.StringUtils.isNullOrEmpty(structurePath))
            return Pair.of(null, null);

        String path = structurePath.toLowerCase();
        CapsuleTemplate template = templatemanager.getTemplateDefaulted(new ResourceLocation(path));
        return Pair.of(templatemanager, template);
    }

    /**
     * Check whether a merge can be done at the destination
     *
     * @param outOccupiedPositions Output param, the positions occupied a destination that will
     *                             have to be ignored on
     * @return List<BlockPos> occupied but not blocking positions
     */
    public static void checkDestination(CapsuleTemplate template, PlacementSettings placementIn, ServerWorld
            destWorld, BlockPos destOriginPos, int size,
                                        List<Block> overridable, Map<BlockPos, Block> outOccupiedPositions, List<ITextComponent> outErrors) {

        BlockState air = Blocks.AIR.getDefaultState();

        List<Template.BlockInfo> srcblocks = template.getBlocks();

        Map<BlockPos, Template.BlockInfo> blockInfoByPosition = new HashMap<>();
        for (Template.BlockInfo template$blockinfo : srcblocks) {
            BlockPos blockpos = CapsuleTemplate.transformedBlockPos(placementIn, template$blockinfo.pos).add(destOriginPos).add(CapsuleTemplate.recenterRotation((size - 1) / 2, placementIn));
            blockInfoByPosition.put(blockpos, template$blockinfo);
        }

        // check the destination is ok for every block of the template
        for (int y = size - 1; y >= 0; y--) {
            for (int x = 0; x < size; x++) {
                for (int z = 0; z < size; z++) {

                    BlockPos destPos = destOriginPos.add(x, y, z);
                    Template.BlockInfo srcInfo = blockInfoByPosition.get(destPos);
                    BlockState templateBlockState = air;
                    if (srcInfo != null) {
                        templateBlockState = srcInfo.state;
                    }

                    if (!destWorld.isBlockLoaded(destPos)) {
                        outErrors.add(new TranslationTextComponent("capsule.error.areaNotLoaded"));
                        return;
                    }
                    BlockState worldDestState = destWorld.getBlockState(destPos);

                    boolean worldDestOccupied = (!worldDestState.isAir(destWorld, destPos) && !overridable.contains(worldDestState.getBlock()));
                    if (!worldDestState.isAir(destWorld, destPos) && outOccupiedPositions != null) {
                        outOccupiedPositions.put(destPos, worldDestState.getBlock());
                    }

                    boolean srcOccupied = (!templateBlockState.isAir(destWorld, destPos) && !overridable.contains(templateBlockState.getBlock()));

                    List<LivingEntity> entities = destWorld.getEntitiesWithinAABB(
                            LivingEntity.class,
                            new AxisAlignedBB(destPos.getX(), destPos.getY(), destPos.getZ(), destPos.getX() + 1, destPos.getY() + 1, destPos.getZ() + 1),
                            entity -> !(entity instanceof PlayerEntity)
                    );


                    // if destination is occupied, and source is neither
                    // excluded from transportation, nor can't be overriden by
                    // destination, then the merge can't be done.
                    if (entities.size() > 0 && srcOccupied) {
                        boolean found = false;
                        for (Object e : entities) {
                            Entity entity = (Entity) e;
                            if (entity != null) {
                                outErrors.add(new TranslationTextComponent("capsule.error.cantMergeWithDestinationEntity", entity.getDisplayName()));
                                found = true;
                            }
                        }
                        if (!found)
                            outErrors.add(new TranslationTextComponent("capsule.error.cantMergeWithDestinationEntity", "???"));

                        return;
                    }
                    if (worldDestOccupied && !overridable.contains(templateBlockState.getBlock())) {
                        outErrors.add(new TranslationTextComponent("capsule.error.cantMergeWithDestination", destPos.toString()));
                        return;
                    }
                }
            }
        }

        return;
    }


    /**
     * Give an id to the capsule that has not already been taken. Ensure that content is not overwritten if capsuleData is removed.
     */
    public static String getUniqueName(ServerWorld playerWorld, String player) {
        CapsuleSavedData csd = getCapsuleSavedData(playerWorld);
        String p = player.toLowerCase();
        String capsuleID = "c-" + p + "-" + csd.getNextCount();
        CapsuleTemplateManager templatemanager = getTemplateManager(playerWorld);
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
    public static String getBlueprintUniqueName(ServerWorld world) {
        CapsuleSavedData csd = getCapsuleSavedData(world);
        String capsuleID = BLUEPRINT_PREFIX + csd.getNextCount();
        CapsuleTemplateManager templatemanager = getTemplateManager(world);
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
            destManager, ServerWorld worldServer, boolean onlyBlocks, List<String> outExcluded) {
        CompoundNBT srcTemplateData = getTemplateNBTData(capsule, worldServer);
        if (srcTemplateData == null) return false; // capsule template not found
        return duplicateTemplate(srcTemplateData, destinationStructureName, destManager, onlyBlocks, outExcluded);

    }

    public static boolean duplicateTemplate(CompoundNBT templateData, String destinationStructureName, CapsuleTemplateManager destManager, MinecraftServer server) {
        return duplicateTemplate(templateData, destinationStructureName, destManager, false, null);
    }

    public static boolean duplicateTemplate(CompoundNBT templateData, String destinationStructureName, CapsuleTemplateManager destManager, boolean onlyWhitelisted, List<String> outExcluded) {
        // create a destination template
        String sanitized = destinationStructureName.toLowerCase();
        ResourceLocation destinationLocation = new ResourceLocation(sanitized);
        CapsuleTemplate destTemplate = destManager.getTemplateDefaulted(destinationLocation);
        // populate template from source data
        destTemplate.read(templateData);
        // empty occupied position, it makes no sense for a new template to copy those situational data
        destTemplate.occupiedPositions = null;
        // remove all tile entities
        if (onlyWhitelisted) {
            destTemplate.filterFromWhitelist(Config.blueprintWhitelist, outExcluded);
        }
        // write the new template
        return destManager.writeToFile(destinationLocation);
    }

    /**
     * Extract the template NBTData from a capsule
     */
    public static CompoundNBT getTemplateNBTData(ItemStack capsule, ServerWorld worldServer) {
        return getTemplateNBTData(StructureSaver.getTemplate(capsule, worldServer).getRight());
    }

    public static CompoundNBT getTemplateNBTData(String path, ServerWorld worldServer) {
        Pair<CapsuleTemplateManager, CapsuleTemplate> sourcetemplatepair;
        if (path.startsWith(Config.rewardTemplatesPath) || path.startsWith("config/")) {
            sourcetemplatepair = StructureSaver.getTemplateForReward(worldServer.getServer(), path);
        } else {
            sourcetemplatepair = StructureSaver.getTemplateForCapsule(worldServer, path);
        }
        return getTemplateNBTData(sourcetemplatepair.getRight());
    }

    public static CompoundNBT getTemplateNBTData(CapsuleTemplate template) {
        if (template == null) return null;
        CompoundNBT data = new CompoundNBT();
        template.writeToNBT(data);
        return data;
    }

    /**
     * Get the Capsule saving tool that remembers last capsule id.
     */
    public static CapsuleSavedData getCapsuleSavedData(ServerWorld capsuleWorld) {
        return capsuleWorld.getSavedData().getOrCreate(CapsuleSavedData::new, "capsuleData");
    }

    @Nullable
    public static String createBlueprintTemplate(String srcStructurePath, ItemStack destCapsule, ServerWorld worldServer, PlayerEntity
            playerIn) {
        if (worldServer == null) {
            LOGGER.error("worldServer is null");
            return null;
        }

        String destStructureName = getBlueprintUniqueName(worldServer) + "-" + srcStructurePath.replace("/", "_");

        CapsuleTemplateManager templateManager = getTemplateManager(worldServer);
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
            if (templateManager != null)
                templateManager.deleteTemplate(worldServer.getServer(), new ResourceLocation(destCapsule.getTag().getString("prevStructureName")));
        }

        if (!created && playerIn != null) {
            playerIn.sendMessage(new TranslationTextComponent("capsule.error.blueprintCreationError"));
        }
        if (outExcluded.size() > 0 && playerIn != null) {
            playerIn.sendMessage(new TranslationTextComponent("capsule.error.blueprintExcluded", "\n* " + String.join("\n* ", outExcluded)));
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
            return otherStack.isItemEqual(this.itemStack) && (!otherStack.hasTag() && !this.itemStack.hasTag() || otherStack.getTag().equals(this.itemStack.getTag()));
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
            return itemstack.getItem().getTranslationKey()
                    + "@"
                    + CapsuleItem.getState(itemstack);
        }
    }

}
