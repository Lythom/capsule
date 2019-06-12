package capsule;

import capsule.items.CapsuleItem;
import capsule.loot.LootPathData;
import capsule.structure.CapsuleTemplate;
import capsule.structure.CapsuleTemplateManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockPistonExtension;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.world.BlockEvent;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StructureSaver {

    protected static final Logger LOGGER = LogManager.getLogger(StructureSaver.class);
    public static final String BLUEPRINT_PREFIX = "B-";
    public static Map<String, CapsuleTemplateManager> CapsulesManagers = new HashMap<>();
    private static CapsuleTemplateManager RewardManager = null;
    private static List<String> outExcluded = new ArrayList<>();

    public static void loadLootList(MinecraftServer server) {
        // Init the manager for reward Lists
        for (int i = 0; i < Config.lootTemplatesPaths.length; i++) {
            String path = Config.lootTemplatesPaths[i];
            LootPathData data = Config.lootTemplatesData.get(path);

            File templateFolder = new File(server.getDataDirectory(), path);

            if (path.startsWith("config/") && !templateFolder.exists()) {
                templateFolder.mkdirs();
                // initial with example capsule the first time
                LOGGER.info("First load: initializing the loots in " + path + ". You can change the content of folder with any nbt structure block, schematic, or capsule file. You can remove the folders from capsule.config to remove loots.");
                populateFolder(templateFolder);
            }

            if (templateFolder.exists() && templateFolder.isDirectory()) {
                File[] fileList = templateFolder.listFiles((p_accept_1_, p_accept_2_) -> p_accept_2_.endsWith(".nbt") || p_accept_2_.endsWith(".schematic"));
                data.files = new ArrayList<>();
                if (fileList != null) {
                    for (File templateFile : fileList) {
                        if (templateFile.isFile() && templateFile.getName().endsWith(".nbt"))
                            data.files.add(templateFile.getName().replaceAll(".nbt", ""));
                        if (templateFile.isFile() && templateFile.getName().endsWith(".schematic"))
                            data.files.add(templateFile.getName().replaceAll(".schematic", ""));
                    }
                }
            }
        }
    }

    public static void populateStarterFolder(MinecraftServer server) {
        String path = Config.starterTemplatesPath;
        File templateFolder = new File(server.getDataDirectory(), path);

        if (path.startsWith("config/") && !templateFolder.exists()) {
            templateFolder.mkdirs();
            // initial with example capsule the first time
            LOGGER.info("First load: initializing the starters in "+path+". You can change the content of folder with any nbt structure block, schematic or capsule file, or empty it for no starter capsule.");
            populateFolder(templateFolder);
        }
        if (templateFolder.exists() && templateFolder.isDirectory()) {
            File[] fileList = templateFolder.listFiles((p_accept_1_, p_accept_2_) -> p_accept_2_.endsWith(".nbt") || p_accept_2_.endsWith(".schematic"));
            Config.starterTemplatesList = new ArrayList<>();
            if (fileList != null) {
                for (File templateFile : fileList) {
                    if (templateFile.isFile() && templateFile.getName().endsWith(".nbt"))
                        Config.starterTemplatesList.add(Config.starterTemplatesPath + "/" + templateFile.getName().replaceAll(".nbt", ""));
                    if (templateFile.isFile() && templateFile.getName().endsWith(".schematic"))
                        Config.starterTemplatesList.add(Config.starterTemplatesPath + "/" + templateFile.getName().replaceAll(".schematic", ""));
                }
            }
        }
    }

    private static void populateFolder(File templateFolder) {
        try {
            // source path
            String assetPath = "assets/capsule/loot/common";
            if (templateFolder.getPath().contains("uncommon")) assetPath = "assets/capsule/loot/uncommon";
            if (templateFolder.getPath().contains("rare")) assetPath = "assets/capsule/loot/rare";
            if (templateFolder.getPath().contains("starters")) assetPath = "assets/capsule/starters";
            String[] resources = getResourceListing(StructureSaver.class, assetPath);

            for (String ressource : resources) {
                if (!ressource.isEmpty()) {
                    InputStream sourceTemplate = StructureSaver.class.getClassLoader().getResourceAsStream(assetPath + "/" + ressource);
                    if (sourceTemplate == null) {
                        LOGGER.error("SourceTemplate " + assetPath + "/" + ressource + "couldn't be loaded");
                        break;
                    }
                    Path assetFile = templateFolder.toPath().resolve(ressource.toLowerCase());
                    LOGGER.debug("copying template " + assetPath + "/" + ressource + " to " + assetFile.toString());
                    try {
                        Files.copy(sourceTemplate, assetFile);
                    } catch (Exception e) {
                        LOGGER.error(e);
                    }
                }
            }

        } catch (Exception e) {
            LOGGER.error("Error while copying initial capsule templates, there will be no loots!", e);
        }
    }

    /**
     * List directory contents for a resource folder. Not recursive.
     * This is basically a brute-force implementation.
     * Works for regular files and also JARs.
     *
     * @param clazz Any java class that lives in the same place as the resources you want.
     * @param path  Should end with "/", but not start with one.
     * @return Just the name of each member item, not the full paths.
     * @throws URISyntaxException
     * @throws IOException
     * @author Greg Briggs
     */
    public static String[] getResourceListing(Class<?> clazz, String path) throws URISyntaxException, IOException {
        URL dirURL = clazz.getClassLoader().getResource(path);

        if (dirURL != null && dirURL.getProtocol().equals("file")) {
            /* A file path: easy enough */
            return new File(dirURL.toURI()).list();
        }

        if (dirURL == null) {
            /*
             * In case of a jar file, we can't actually find a directory. Have
             * to assume the same jar as clazz.
             */
            String me = clazz.getName().replace(".", "/") + ".class";
            dirURL = clazz.getClassLoader().getResource(me);
        }

        if (dirURL.getProtocol().equals("jar")) {
            /* A JAR path */
            String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!")); //strip out only the JAR file
            JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));

            LOGGER.debug("Listing files in " + jarPath);

            Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar
            Set<String> result = new HashSet<>(); //avoid duplicates in case it is a subdirectory
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith(path)) { //filter according to the path
                    LOGGER.debug("Found in jar " + name);
                    String entry = name.replace(path + "/", "");
                    LOGGER.debug("Keeping " + entry);
                    result.add(entry);
                }
            }
            jar.close();
            return result.toArray(new String[result.size()]);

        } else {

            InputStream inputstream = clazz.getResourceAsStream("/" + path);
            if (inputstream != null) {
                final InputStreamReader isr = new InputStreamReader(inputstream, StandardCharsets.UTF_8);
                final BufferedReader br = new BufferedReader(isr);

                Set<String> result = new HashSet<>(); //avoid duplicates in case it is a subdirectory
                String filename = null;
                while ((filename = br.readLine()) != null) {
                    result.add(filename);
                }
                return result.toArray(new String[result.size()]);
            }

        }

        throw new UnsupportedOperationException("Cannot list files for URL " + dirURL);
    }

    public static CapsuleTemplateManager getRewardManager(MinecraftServer server) {
        if (RewardManager == null) {
            RewardManager = new CapsuleTemplateManager(server.getDataDirectory().getPath(), net.minecraftforge.fml.common.FMLCommonHandler.instance().getDataFixer());
            File rewardDir = new File(Config.rewardTemplatesPath);
            if (!rewardDir.exists()) {
                rewardDir.mkdirs();
            }
        }
        return RewardManager;
    }

    public static CapsuleTemplate undeploy(WorldServer worldserver, String playerID, String capsuleStructureId, BlockPos startPos, int size, List<Block> excluded,
                                   Map<BlockPos, Block> excludedPositions) {

        MinecraftServer minecraftserver = worldserver.getMinecraftServer();
        if (minecraftserver == null) {
            LOGGER.error("worldserver.getMinecraftServer() returned null");
            return null;
        }
        List<Entity> outCapturedEntities = new ArrayList<>();

        CapsuleTemplateManager templatemanager = getTemplateManager(worldserver);
        if (templatemanager == null) {
            LOGGER.error("getTemplateManager returned null");
            return null;
        }
        CapsuleTemplate template = templatemanager.getTemplate(minecraftserver, new ResourceLocation(capsuleStructureId));
        List<BlockPos> transferedPositions = template.takeBlocksFromWorldIntoCapsule(worldserver, startPos, new BlockPos(size, size, size), excludedPositions,
                excluded, outCapturedEntities);
        EntityPlayer player = null;
        if (playerID != null) {
            template.setAuthor(playerID);
            player = worldserver.getPlayerEntityByName(playerID);
        }
        boolean writingOK = templatemanager.writeTemplate(minecraftserver, new ResourceLocation(capsuleStructureId));
        if (writingOK) {
            List<BlockPos> couldNotBeRemoved = removeTransferedBlockFromWorld(transferedPositions, worldserver, player);
            for (Entity e : outCapturedEntities) {
                e.setDropItemsWhenDead(false);
                e.setDead();
            }
            // check if some remove failed, exclude those blocks from the template.
            if (couldNotBeRemoved != null) {
                template.removeBlocks(couldNotBeRemoved, startPos);
                templatemanager.writeTemplate(minecraftserver, new ResourceLocation(capsuleStructureId));
            }
        } else {
            LOGGER.error("Couldn't write template " + capsuleStructureId);
            if (player != null) {
                player.sendMessage(new TextComponentTranslation("capsule.error.technicalError"));
            }
        }

        return template;

    }

    public static boolean undeployBlueprint(WorldServer worldserver, String playerID, String capsuleStructureId, BlockPos startPos, int size, List<Block> excluded,
                                            Map<BlockPos, Block> excludedPositions) {

        MinecraftServer minecraftserver = worldserver.getMinecraftServer();
        if (minecraftserver == null) return false;
        CapsuleTemplateManager templatemanager = getTemplateManager(worldserver);
        if (templatemanager == null) {
            LOGGER.error("getTemplateManager returned null");
            return false;
        }
        CapsuleTemplate tempTemplate = new CapsuleTemplate();
        CapsuleTemplate blueprintTemplate = templatemanager.get(minecraftserver, new ResourceLocation(capsuleStructureId));
        if (blueprintTemplate == null) return false;
        List<BlockPos> transferedPositions = tempTemplate.takeBlocksFromWorldIntoCapsule(worldserver, startPos, new BlockPos(size, size, size), excludedPositions,
                excluded, null);

        EntityPlayer player = null;
        if (playerID != null) {
            player = worldserver.getPlayerEntityByName(playerID);
        }
        // compare the 2 lists, assume they are sorted the same since the same script is used to build them.
        if (blueprintTemplate.blocks.size() != tempTemplate.blocks.size())
            return false;

        List<String> tempTemplateSorted = tempTemplate.blocks.stream().map(b -> b.blockState.getBlock().getUnlocalizedName() + "@" + b.blockState.getBlock().damageDropped(b.blockState)).sorted().collect(Collectors.toList());
        List<String> blueprintTemplateSorted = blueprintTemplate.blocks.stream().map(b -> b.blockState.getBlock().getUnlocalizedName() + "@" + b.blockState.getBlock().damageDropped(b.blockState)).sorted().collect(Collectors.toList());
        boolean blueprintMatch = IntStream.range(0, tempTemplateSorted.size())
                .allMatch(i -> tempTemplateSorted.get(i).equals(blueprintTemplateSorted.get(i)));

        if (blueprintMatch) {
            List<BlockPos> couldNotBeRemoved = removeTransferedBlockFromWorld(transferedPositions, worldserver, player);
            // check if some remove failed, it should never happen but keep it in case to prevent exploits
            if (couldNotBeRemoved != null) {
                return false;
            }
        }

        return blueprintMatch;
    }


    @Nullable
    public static Map<ItemStackKey, Integer> getMaterialList(ItemStack blueprint, WorldServer worldserver) {
        MinecraftServer minecraftserver = worldserver.getMinecraftServer();
        CapsuleTemplateManager templatemanager = getTemplateManager(worldserver);
        if (templatemanager == null) {
            LOGGER.error("getTemplateManager returned null");
            return null;
        }
        CapsuleTemplate blueprintTemplate = templatemanager.get(minecraftserver, new ResourceLocation(CapsuleItem.getStructureName(blueprint)));
        if (blueprintTemplate == null) return null;
        Map<ItemStackKey, Integer> list = new HashMap<>();

        boolean doorCountedOnce = false;
        for (Template.BlockInfo block : blueprintTemplate.blocks) {// Note: tile entities not supported so nbt data is not used here
            Block b = block.blockState.getBlock();
            ItemStack itemStack = ItemStack.EMPTY;
            try {
                // prevent door to beeing counted twice
                if (b instanceof BlockDoor) {
                    if (doorCountedOnce) {
                        itemStack = ItemStack.EMPTY;
                    } else {
                        itemStack = b.getItem(null, null, block.blockState);
                    }
                    doorCountedOnce = !doorCountedOnce;
                } else if (b instanceof BlockDoublePlant || b instanceof BlockPistonExtension) {
                    itemStack = ItemStack.EMPTY; // free… too complicated for what it worth
                } else {
                    itemStack = b.getItem(null, null, block.blockState);
                }
            } catch (Exception e) {
                // some items requires world to have getItem work, here it produces NullPointerException. fallback to default break state of block.
                itemStack = new ItemStack(Item.getItemFromBlock(b), 1, b.getMetaFromState(block.blockState));
            }
            ItemStackKey stackKey = new ItemStackKey(itemStack);
            if (!itemStack.isEmpty() && itemStack.getItem() != Items.AIR) {
                Integer currValue = list.get(stackKey);
                if (currValue == null) currValue = 0;
                list.put(stackKey, currValue + 1);
            }
        }
        // Note: entities not supportes so no entities check
        return list;
    }

    @Nullable
    public static CapsuleTemplateManager getTemplateManager(WorldServer worldserver) {
        if (worldserver == null) return null;
        File directory = worldserver.getSaveHandler().getWorldDirectory();
        String directoryPath = directory.getPath();

        if (!CapsulesManagers.containsKey(directoryPath)) {
            File capsuleDir = new File(directory, "structures/capsule");
            capsuleDir.mkdirs();
            CapsulesManagers.put(directoryPath, new CapsuleTemplateManager(capsuleDir.toString(), net.minecraftforge.fml.common.FMLCommonHandler.instance().getDataFixer()));
        }
        return CapsulesManagers.get(directoryPath);
    }

    /**
     * Use with caution, delete the blocks at the indicated positions.
     *
     * @return list of blocks that could not be removed
     */
    public static List<BlockPos> removeTransferedBlockFromWorld(List<BlockPos> transferedPositions, WorldServer
            world, EntityPlayer player) {

        List<BlockPos> couldNotBeRemoved = null;

        // disable tileDrop during the operation so that broken block are not
        // itemized on the ground.
        boolean flagdoTileDrops = world.getGameRules().getBoolean("doTileDrops");
        world.getGameRules().setOrCreateGameRule("doTileDrops", "false");
        world.restoringBlockSnapshots = true;

        // delete everything that as been saved in the capsule

        for (BlockPos pos : transferedPositions) {
            IBlockState b = world.getBlockState(pos);
            try {
                // uses same mechanic for TileEntity than net.minecraft.world.gen.structure.template.Template
                if (playerCanRemove(world, pos, player)) {
                    world.setBlockToAir(pos);
                } else {
                    if (couldNotBeRemoved == null) couldNotBeRemoved = new ArrayList<>();
                    couldNotBeRemoved.add(pos);
                }
            } catch (Exception e) {
                LOGGER.error("Block crashed during Capsule capture phase : couldn't be removed. Will be ignored.", e);
                if (player != null) {
                    player.sendMessage(new TextComponentTranslation("capsule.error.technicalError"));
                }
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
        world.getGameRules().setOrCreateGameRule("doTileDrops", String.valueOf(flagdoTileDrops));

        return couldNotBeRemoved;
    }


    public static boolean clearTemplate(WorldServer worldserver, String capsuleStructureId) {
        MinecraftServer minecraftserver = worldserver.getMinecraftServer();

        CapsuleTemplateManager templatemanager = getTemplateManager(worldserver);
        if (templatemanager == null) {
            LOGGER.error("getTemplateManager returned null");
            return false;
        }
        CapsuleTemplate template = templatemanager.getTemplate(minecraftserver, new ResourceLocation(capsuleStructureId));

        List<Template.BlockInfo> blocks = template.blocks;
        List<Template.EntityInfo> entities = template.entities;

        blocks.clear();
        entities.clear();

        return templatemanager.writeTemplate(minecraftserver, new ResourceLocation(capsuleStructureId));

    }

    public static boolean deploy(ItemStack capsule, WorldServer playerWorld, String thrower, BlockPos
            dest, List<Block> overridableBlocks,
                                 Map<BlockPos, Block> outOccupiedSpawnPositions, List<String> outEntityBlocking, PlacementSettings
                                         placementsettings) {


        Pair<CapsuleTemplateManager, CapsuleTemplate> templatepair = getTemplate(capsule, playerWorld);
        CapsuleTemplate template = templatepair.getRight();

        EntityPlayer player = null;
        if (thrower != null) {
            player = playerWorld.getPlayerEntityByName(thrower);
        }

        if (template != null) {
            int size = CapsuleItem.getSize(capsule);
            // check if the destination is valid : no unoverwritable block and no entities in the way.
            boolean destValid = isDestinationValid(template, placementsettings, playerWorld, dest, size, overridableBlocks, outOccupiedSpawnPositions, outEntityBlocking);

            if (destValid) {
                List<BlockPos> spawnedBlocks = new ArrayList<>();
                List<Entity> spawnedEntities = new ArrayList<>();
                try {
                    // check if the player can place a block
                    if (player != null && !playerCanPlace(playerWorld, dest, template, player, placementsettings)) {
                        player.sendMessage(new TextComponentTranslation("capsule.error.notAllowed"));
                        return false;
                    }

                    template.spawnBlocksAndEntities(playerWorld, dest, placementsettings, outOccupiedSpawnPositions, overridableBlocks, spawnedBlocks, spawnedEntities);

                    // Players don't block deployment, instead they are pushed up if they would suffocate
                    List<EntityLivingBase> players = playerWorld.getEntitiesWithinAABB(
                            EntityLivingBase.class,
                            new AxisAlignedBB(dest.getX(), dest.getY(), dest.getZ(), dest.getX() + size, dest.getY() + size, dest.getZ() + size),
                            entity -> (entity instanceof EntityPlayer)
                    );
                    for (EntityLivingBase p : players) {
                        for (int y = 0; y < size; y++) {
                            if (playerWorld.collidesWithAnyBlock(p.getEntityBoundingBox())) {
                                p.setPositionAndUpdate(p.posX, p.posY + 1, p.posZ);
                            }
                        }
                    }

                    return true;
                } catch (Exception err) {
                    LOGGER.error("Couldn't deploy the capsule", err);
                    if (player != null) {
                        player.sendMessage(new TextComponentTranslation("capsule.error.technicalError"));
                    }

                    // rollback
                    removeTransferedBlockFromWorld(spawnedBlocks, playerWorld, player);
                    for (Entity e : spawnedEntities) {
                        e.setDropItemsWhenDead(false);
                        e.setDead();
                    }
                }
            } else {
                // send a chat message to explain failure
                if (player != null) {
                    if (outEntityBlocking.size() > 0) {
                        player.sendMessage(
                                new TextComponentTranslation("capsule.error.cantMergeWithDestinationEntity",
                                        StringUtils.join(outEntityBlocking, ", ")));
                    } else {
                        player.sendMessage(new TextComponentTranslation("capsule.error.cantMergeWithDestination"));
                    }
                }
            }
        } else {
            // send a chat message to explain failure
            if (player != null) {
                player.sendMessage(new TextComponentTranslation("capsule.error.capsuleContentNotFound", CapsuleItem.getStructureName(capsule)));
            }
        }


        return false;
    }

    /**
     * Simulate a block placement at all positions to see if anythink revoke the placement of block by the player.
     */
    private static boolean playerCanPlace(WorldServer worldserver, BlockPos dest, CapsuleTemplate
            template, EntityPlayer player, PlacementSettings placementsettings) {
        if (player != null) {
            List<BlockPos> expectedOut = template.calculateDeployPositions(worldserver, dest, placementsettings);
            for (BlockPos blockPos : expectedOut) {
                if (blockPos.getY() >= worldserver.getHeight() || !isPlaceEventAllowed(worldserver, blockPos, player))
                    return false;
            }
        }
        return true;
    }

    /**
     * Simulate a block placement at all positions to see if anythink revoke the placement of block by the player.
     */
    private static boolean playerCanRemove(WorldServer worldserver, BlockPos blockPos, EntityPlayer player) {
        if (player != null) {
            return isPlaceEventAllowed(worldserver, blockPos, player);
        }
        return true;
    }

    private static boolean isPlaceEventAllowed(WorldServer worldserver, BlockPos blockPos, EntityPlayer player) {
        BlockSnapshot blocksnapshot = new BlockSnapshot(worldserver, blockPos, Blocks.AIR.getDefaultState());
        BlockEvent.PlaceEvent event = new BlockEvent.PlaceEvent(blocksnapshot, Blocks.DIRT.getDefaultState(), player, EnumHand.MAIN_HAND);
        MinecraftForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }

    public static Pair<CapsuleTemplateManager, CapsuleTemplate> getTemplate(ItemStack capsule, WorldServer
            playerWorld) {
        Pair<CapsuleTemplateManager, CapsuleTemplate> template = null;

        boolean isReward = CapsuleItem.isReward(capsule);
        String structureName = CapsuleItem.getStructureName(capsule);
        if (isReward) {
            template = getTemplateForReward(playerWorld.getMinecraftServer(), structureName);
        } else {
            template = getTemplateForCapsule(playerWorld, structureName);
        }
        return template;
    }

    public static Pair<CapsuleTemplateManager, CapsuleTemplate> getTemplateForCapsule(WorldServer
                                                                                              playerWorld, String structureName) {
        CapsuleTemplateManager templatemanager = getTemplateManager(playerWorld);
        if (templatemanager == null || net.minecraft.util.StringUtils.isNullOrEmpty(structureName))
            return Pair.of(null, null);

        CapsuleTemplate template = templatemanager.getTemplate(playerWorld.getMinecraftServer(), new ResourceLocation(structureName));
        return Pair.of(templatemanager, template);
    }

    public static Pair<CapsuleTemplateManager, CapsuleTemplate> getTemplateForReward(MinecraftServer server, String
            structurePath) {
        CapsuleTemplateManager templatemanager = getRewardManager(server);
        if (templatemanager == null || net.minecraft.util.StringUtils.isNullOrEmpty(structurePath))
            return Pair.of(null, null);

        CapsuleTemplate template = templatemanager.getTemplate(server, new ResourceLocation(structurePath));
        return Pair.of(templatemanager, template);
    }

    /**
     * Check whether a merge can be done at the destination
     *
     * @param outOccupiedPositions Output param, the positions occupied a destination that will
     *                             have to be ignored on
     * @return List<BlockPos> occupied but not blocking positions
     */
    public static boolean isDestinationValid(CapsuleTemplate template, PlacementSettings placementIn, WorldServer
            destWorld, BlockPos destOriginPos, int size,
                                             List<Block> overridable, Map<BlockPos, Block> outOccupiedPositions, List<String> outEntityBlocking) {

        IBlockState air = Blocks.AIR.getDefaultState();

        List<Template.BlockInfo> srcblocks = template.blocks;

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
                    IBlockState templateBlockState = air;
                    if (srcInfo != null) {
                        templateBlockState = srcInfo.blockState;
                    }

                    if (!destWorld.isBlockLoaded(destPos)) return false;
                    IBlockState worldDestState = destWorld.getBlockState(destPos);

                    boolean worldDestOccupied = (worldDestState != air && !overridable.contains(worldDestState.getBlock()));
                    if (worldDestState != air && outOccupiedPositions != null) {
                        outOccupiedPositions.put(destPos, worldDestState.getBlock());
                    }

                    boolean srcOccupied = (templateBlockState != air && !overridable.contains(templateBlockState.getBlock()));

                    List<EntityLivingBase> entities = destWorld.getEntitiesWithinAABB(
                            EntityLivingBase.class,
                            new AxisAlignedBB(destPos.getX(), destPos.getY(), destPos.getZ(), destPos.getX() + 1, destPos.getY() + 1, destPos.getZ() + 1),
                            entity -> !(entity instanceof EntityPlayer)
                    );


                    // if destination is occupied, and source is neither
                    // excluded from transportation, nor can't be overriden by
                    // destination, then the merge can't be done.
                    if ((entities.size() > 0 && srcOccupied) || (worldDestOccupied && !overridable.contains(templateBlockState.getBlock()))) {
                        if (entities.size() > 0 && outEntityBlocking != null) {
                            for (Object e : entities) {
                                Entity entity = (Entity) e;
                                if (entity != null) {
                                    outEntityBlocking.add(entity.getName());
                                }
                            }

                        }
                        return false;
                    }
                }
            }
        }

        return true;
    }


    /**
     * Give an id to the capsule that has not already been taken. Ensure that content is not overwritten if capsuleData is removed.
     */
    public static String getUniqueName(WorldServer playerWorld, String player) {
        CapsuleSavedData csd = getCapsuleSavedData(playerWorld);
        String capsuleID = "C-" + player + "-" + csd.getNextCount();
        CapsuleTemplateManager templatemanager = getTemplateManager(playerWorld);
        if (templatemanager == null) {
            LOGGER.error("getTemplateManager returned null");
            return "CExcetion-" + player + "-" + csd.getNextCount();
        }
        while (templatemanager.get(playerWorld.getMinecraftServer(), new ResourceLocation(capsuleID)) != null) {
            capsuleID = "C-" + player + "-" + csd.getNextCount();
        }

        return capsuleID;
    }

    /**
     * Give an id to the capsule that has not already been taken. Ensure that content is not overwritten if capsuleData is removed.
     */
    public static String getBlueprintUniqueName(WorldServer world) {
        CapsuleSavedData csd = getCapsuleSavedData(world);
        String capsuleID = BLUEPRINT_PREFIX + csd.getNextCount();
        CapsuleTemplateManager templatemanager = getTemplateManager(world);
        if (templatemanager == null) {
            LOGGER.error("getTemplateManager returned null");
            return "BException-" + csd.getNextCount();
        }
        while (templatemanager.get(world.getMinecraftServer(), new ResourceLocation(capsuleID)) != null) {
            capsuleID = BLUEPRINT_PREFIX + csd.getNextCount();
        }

        return capsuleID;
    }

    public static boolean copyFromCapsuleTemplate(ItemStack capsule, String destinationStructureName, CapsuleTemplateManager
            destManager, WorldServer worldServer, boolean onlyBlocks, List<String> outExcluded) {
        NBTTagCompound srcTemplateData = getTemplateNBTData(capsule, worldServer);
        if (srcTemplateData == null) return false; // capsule template not found
        return duplicateTemplate(srcTemplateData, destinationStructureName, destManager, worldServer.getMinecraftServer(), onlyBlocks, outExcluded);

    }

    public static boolean duplicateTemplate(NBTTagCompound templateData, String destinationStructureName, CapsuleTemplateManager destManager, MinecraftServer server) {
        return duplicateTemplate(templateData, destinationStructureName, destManager, server, false, null);
    }

    public static boolean duplicateTemplate(NBTTagCompound templateData, String destinationStructureName, CapsuleTemplateManager destManager, MinecraftServer server, boolean onlyBlocks, List<String> outExcluded) {
        // create a destination template
        ResourceLocation destinationLocation = new ResourceLocation(destinationStructureName);
        CapsuleTemplate destTemplate = destManager.getTemplate(server, destinationLocation);
        // populate template from source data
        destTemplate.read(templateData);
        // remove all tile entities
        if (onlyBlocks) {
            List<Template.BlockInfo> newBlockList = destTemplate.blocks.stream().filter(b -> {
                boolean included = b.tileentityData == null;
                if (!included && outExcluded != null) outExcluded.add(b.blockState.toString());
                return included;
            }).collect(Collectors.toList());
            destTemplate.blocks.clear();
            destTemplate.blocks.addAll(newBlockList);
            // remove all entities
            destTemplate.entities.clear();
        }
        // write the new template
        return destManager.writeTemplate(server, destinationLocation);
    }

    /**
     * Extract the template NBTData from a capsule
     */
    public static NBTTagCompound getTemplateNBTData(ItemStack capsule, WorldServer worldServer) {
        return getTemplateNBTData(StructureSaver.getTemplate(capsule, worldServer).getRight());
    }

    public static NBTTagCompound getTemplateNBTData(String path, WorldServer worldServer) {
        Pair<CapsuleTemplateManager, CapsuleTemplate> sourcetemplatepair;
        if (path.startsWith(Config.rewardTemplatesPath)) {
            sourcetemplatepair = StructureSaver.getTemplateForReward(worldServer.getMinecraftServer(), path);
        } else {
            sourcetemplatepair = StructureSaver.getTemplateForCapsule(worldServer, path);
        }
        return getTemplateNBTData(sourcetemplatepair.getRight());
    }

    public static NBTTagCompound getTemplateNBTData(CapsuleTemplate template) {
        if (template == null) return null;
        NBTTagCompound data = new NBTTagCompound();
        template.writeToNBT(data);
        return data;
    }

    /**
     * Get the Capsule saving tool that remembers last capsule id.
     */
    public static CapsuleSavedData getCapsuleSavedData(WorldServer capsuleWorld) {
        CapsuleSavedData capsuleSavedData = (CapsuleSavedData) capsuleWorld.loadData(CapsuleSavedData.class, "capsuleData");
        if (capsuleSavedData == null) {
            capsuleSavedData = new CapsuleSavedData("capsuleData");
            capsuleWorld.setData("capsuleData", capsuleSavedData);
            capsuleSavedData.setDirty(true);
        }
        return capsuleSavedData;
    }

    @Nullable
    public static String createBlueprintTemplate(String srcStructurePath, ItemStack destCapsule, WorldServer worldServer, EntityPlayer
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
                worldServer.getMinecraftServer(),
                true,
                outExcluded
        );

        // try to cleanup previous template to save disk space on the long run
        if (destCapsule.getTagCompound() != null && destCapsule.getTagCompound().hasKey("prevStructureName")) {
            if (templateManager != null)
                templateManager.deleteTemplate(worldServer.getMinecraftServer(), new ResourceLocation(destCapsule.getTagCompound().getString("prevStructureName")));
        }

        if (!created && playerIn != null) {
            playerIn.sendMessage(new TextComponentTranslation("capsule.error.blueprintCreationError"));
        }
        if (outExcluded.size() > 0 && playerIn != null) {
            playerIn.sendMessage(new TextComponentTranslation("capsule.error.blueprintExcluded", "\n* " + String.join("\n* ", outExcluded)));
        }
        return destStructureName;
    }

    public static class ItemStackKey {
        public ItemStack itemStack;

        public ItemStackKey(ItemStack itemStack) {
            this.itemStack = itemStack;
        }

        public boolean equals(Object someOther) {
            if (!(someOther instanceof ItemStackKey)) return false;
            return ((ItemStackKey) someOther).itemStack.isItemEqual(itemStack);
        }

        public int hashCode() {
            int val = itemStack.getItem().hashCode() * 29 + itemStack.getItemDamage();
            return val;
        }
    }

}
