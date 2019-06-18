package capsule;

import capsule.helpers.Serialization;
import capsule.loot.LootPathData;
import com.google.gson.JsonObject;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Config {

    protected static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(Config.class);

    public static Configuration config = null;
    public static List<Block> excludedBlocks;
    public static List<Block> overridableBlocks;
    public static List<Block> opExcludedBlocks;
    public static String[] lootTemplatesPaths;
    public static List<String> lootTablesList;
    public static Map<String, LootPathData> lootTemplatesData = new HashMap<>();
    public static String starterTemplatesPath;
    public static List<String> starterTemplatesList = new ArrayList<>();
    public static String rewardTemplatesPath;
    public static int upgradeLimit;
    public static HashMap<String, JsonObject> blueprintWhitelist;

    public static String enchantRarity;
    public static String recallEnchantType;
    public static Map<String, Integer> capsuleSizes = new HashMap<>();

    public static Supplier<Integer> ironCapsuleSize = () -> capsuleSizes.get("ironCapsuleSize");
    public static Supplier<Integer> goldCapsuleSize = () -> capsuleSizes.get("goldCapsuleSize");
    public static Supplier<Integer> diamondCapsuleSize = () -> capsuleSizes.get("diamondCapsuleSize");
    public static Supplier<Integer> opCapsuleSize = () -> capsuleSizes.get("opCapsuleSize");

    public static void readConfig(Configuration config) {
        try {
            Config.config = config;
            config.load();
        } catch (Exception e1) {
            LOGGER.error("Problem loading config file !", e1);
        } finally {
            if (config.hasChanged()) {
                config.save();
            }
        }
    }

    public static void initCaptureConfigs() {

        // upgrade limits
        Property upgradesLimit = Config.config.get("Balancing", "capsuleUpgradesLimit", 10);
        upgradesLimit.setComment("Number of upgrades an empty capsule can get to improve capacity. If <= 0, the capsule won't be able to upgrade.");
        Config.upgradeLimit = upgradesLimit.getInt();

        // Excluded
        Block[] defaultExcludedBlocksOP = new Block[]{Blocks.AIR, Blocks.STRUCTURE_VOID, Blocks.BEDROCK};
        Block[] defaultExcludedBlocks = new Block[]{Blocks.MOB_SPAWNER, Blocks.END_PORTAL, Blocks.END_PORTAL_FRAME};

        String[] excludedBlocksOP = ArrayUtils.addAll(
                Serialization.serializeBlockArray(defaultExcludedBlocksOP),
                "ic2:",
                "refinedstorage:",
                "bloodmagic:alchemy_table",
                "mekanism:machineblock",
                "mekanism:boundingblock"
        );
        String[] excludedBlocks = ArrayUtils.addAll(
                Serialization.serializeBlockArray(defaultExcludedBlocks),
                excludedBlocksOP
        );
        Property excludedBlocksProp = Config.config.get("Balancing", "excludedBlocks", excludedBlocks);
        excludedBlocksProp.setComment("List of block ids that will never be captured by a non overpowered capsule. While capturing, the blocks will stay in place.\n Ex: minecraft:mob_spawner");
        Block[] exBlocks = null;
        exBlocks = Serialization.deserializeBlockArray(excludedBlocksProp.getStringList());
        Config.excludedBlocks = Arrays.asList(exBlocks);

        // OP Excluded
        Property opExcludedBlocksProp = Config.config.get("Balancing", "opExcludedBlocks", excludedBlocksOP);
        opExcludedBlocksProp.setComment("List of block ids that will never be captured even with an overpowered capsule. While capturing, the blocks will stay in place.\n Ex: minecraft:mob_spawner");
        Block[] opExBlocks = null;

        opExBlocks = Serialization.deserializeBlockArray(opExcludedBlocksProp.getStringList());
        Config.opExcludedBlocks = Arrays.asList(opExBlocks);


        // Overridable
        Block[] defaultOverridable = new Block[]{Blocks.AIR, Blocks.WATER, Blocks.LEAVES,
                Blocks.LEAVES2, Blocks.TALLGRASS, Blocks.RED_FLOWER, Blocks.YELLOW_FLOWER,
                Blocks.SNOW_LAYER, Blocks.BROWN_MUSHROOM, Blocks.RED_MUSHROOM, Blocks.DOUBLE_PLANT};

        String[] dynamicOverridable = ForgeRegistries.BLOCKS.getValuesCollection().stream()
                .filter(block -> {
                    ResourceLocation registryName = block.getRegistryName();
                    String domain = registryName != null ? registryName.getResourceDomain() : "";
                    String path = registryName != null ? registryName.getResourcePath().toLowerCase() : "";
                    return Arrays.stream(new String[]{"capsule", "minecraft"}).noneMatch(d -> d.equalsIgnoreCase(domain))
                            && Arrays.stream(new String[]{"leaves", "sapling", "mushroom", "vine"}).anyMatch(path::contains);
                })
                .map(block -> block.getRegistryName().toString())
                .toArray(String[]::new);

        Property overridableBlocksProp = Config.config.get(
                "Balancing",
                "overridableBlocks",
                ArrayUtils.addAll(
                        Serialization.serializeBlockArray(defaultOverridable),
                        dynamicOverridable)

        );
        overridableBlocksProp.setComment("List of block ids that can be overriden while teleporting blocks.\nPut there blocks that the player don't care about (grass, leaves) so they don't prevent the capsule from deploying.");

        Block[] ovBlocks = null;
        ovBlocks = Serialization.deserializeBlockArray(overridableBlocksProp.getStringList());
        Config.overridableBlocks = Arrays.asList(ovBlocks);
    }

    public static void initLootConfigs() {

        // Loot tables that can reward a capsule
        String[] defaultLootTablesList = new String[]{
                LootTableList.CHESTS_ABANDONED_MINESHAFT.toString(),
                LootTableList.CHESTS_DESERT_PYRAMID.toString(),
                LootTableList.CHESTS_END_CITY_TREASURE.toString(),
                LootTableList.CHESTS_IGLOO_CHEST.toString(),
                LootTableList.CHESTS_JUNGLE_TEMPLE.toString(),
                LootTableList.CHESTS_SIMPLE_DUNGEON.toString(),
                LootTableList.CHESTS_STRONGHOLD_CORRIDOR.toString(),
                LootTableList.CHESTS_STRONGHOLD_CROSSING.toString(),
                LootTableList.CHESTS_STRONGHOLD_LIBRARY.toString(),
                LootTableList.CHESTS_VILLAGE_BLACKSMITH.toString(),
                LootTableList.CHESTS_WOODLAND_MANSION.toString()
        };
        Property lootTablesListProp = Config.config.get("loots", "lootTablesList", defaultLootTablesList);
        lootTablesListProp.setComment("List of loot tables that will eventually reward a capsule.\n Example of valid loot tables : gameplay/fishing/treasure, chests/spawn_bonus_chest, entities/villager (killing a villager).\nAlso see https://minecraft.gamepedia.com/Loot_table#List_of_loot_tables.");
        Config.lootTablesList = new ArrayList<>(Arrays.asList(lootTablesListProp.getStringList()));

        // CapsuleTemplate Paths
        Property lootTemplatesPathsProp = Config.config.get("loots", "lootTemplatesPaths", new String[]{
                "config/capsule/loot/common",
                "config/capsule/loot/uncommon",
                "config/capsule/loot/rare"
        });
        lootTemplatesPathsProp.setComment("List of paths where the mod will look for structureBlock files. Each save structure have a chance to appear as a reward capsule in a dungeon chest.\nTo Lower the chance of getting a capsule at all, insert an empty folder here and configure its weight accordingly (more weigth on empty folder = less capsule chance per chest).");
        Config.lootTemplatesPaths = lootTemplatesPathsProp.getStringList();

        Property starterTemplatesPathProp = Config.config.get("loots", "starterTemplatesPath","config/capsule/starters");
        starterTemplatesPathProp.setComment("Each structure in this folder will be given to the player as standard reusable capsule on game start.\nEmpty the folder to disable starter capsules.\nDefault value: \"config/capsule/starters\"");
        Config.starterTemplatesPath = starterTemplatesPathProp.getString();

        Property rewardTemplatesPathProp = Config.config.get("loots", "rewardTemplatesPath", "config/capsule/rewards");
        rewardTemplatesPathProp.setComment("Paths where the mod will look for structureBlock files when invoking command /capsule fromExistingRewards <structureName> [playerName].");
        Config.rewardTemplatesPath = rewardTemplatesPathProp.getString();

        // init paths properties from config
        for (int i = 0; i < Config.lootTemplatesPaths.length; i++) {
            String path = Config.lootTemplatesPaths[i];

            if (!Config.lootTemplatesData.containsKey(path)) {
                Config.lootTemplatesData.put(path, new LootPathData());
            }
            Property pathDataWeight = Config.config.get("loots:" + path, "weight", path.endsWith("rare") ? 2 : path.endsWith("uncommon") ? 6 : 10);
            pathDataWeight.setComment("Chances to get a capsule from this folder. Higher means more common. Default : 2 (rare), 6 (uncommon) or 10 (common)");
            Config.lootTemplatesData.get(path).weigth = pathDataWeight.getInt();
        }
    }

    public static void initReceipeConfigs() {
        Property woodCapsuleSize = Config.config.get("Balancing", "woodCapsuleSize", "1");
        woodCapsuleSize.setComment("Size of the capture cube side for an Iron Capsule. Must be an Odd Number (or it will be rounded down with error message).\n0 to disable.\nDefault: 1");

        Property ironCapsuleSize = Config.config.get("Balancing", "ironCapsuleSize", "3");
        ironCapsuleSize.setComment("Size of the capture cube side for an Iron Capsule. Must be an Odd Number (or it will be rounded down with error message).\n0 to disable.\nDefault: 3");

        Property goldCapsuleSize = Config.config.get("Balancing", "goldCapsuleSize", "5");
        goldCapsuleSize.setComment("Size of the capture cube side for a Gold Capsule. Must be an Odd Number (or it will be rounded down with error message).\n0 to disable.\nDefault: 5");

        Property diamondCapsuleSize = Config.config.get("Balancing", "diamondCapsuleSize", "7");
        diamondCapsuleSize.setComment("Size of the capture cube side for a Diamond Capsule. Must be an Odd Number (or it will be rounded down with error message).\n0 to disable.\nDefault: 7");

        Property obsidianCapsuleSize = Config.config.get("Balancing", "obsidianCapsuleSize", "9");
        obsidianCapsuleSize.setComment("Size of the capture cube side for an Obsidian Capsule. Must be an Odd Number (or it will be rounded down with error message).\n0 to disable.\nDefault: 9");

        Property emeraldCapsuleSize = Config.config.get("Balancing", "emeraldCapsuleSize", "11");
        emeraldCapsuleSize.setComment("Size of the capture cube side for an Emerald Capsule. Must be an Odd Number (or it will be rounded down with error message).\n0 to disable.\nDefault: 11");

        Property opCapsuleSize = Config.config.get("Balancing", "opCapsuleSize", "1");
        opCapsuleSize.setComment("Size of the capture cube side for a Overpowered Capsule. Must be an Odd Number (or it will be rounded down with error message).\n0 to disable.\nDefault: 1");

        Config.config.getCategory("Balancing").values().forEach(property -> {
            if (property.getName().endsWith("CapsuleSize")) {
                Config.capsuleSizes.put(property.getName(), property.getInt());
            }
        });
    }

    public static void initEnchantsConfigs() {
        Property enchantRarityConfig = Config.config.get("Balancing", "recallEnchantRarity", "RARE");
        enchantRarityConfig.setComment("Rarity of the enchantmant. Possible values : COMMON, UNCOMMON, RARE, VERY_RARE. Default: RARE.");
        Config.enchantRarity = enchantRarityConfig.getString();

        Property recallEnchantTypeConfig = Config.config.get("Balancing", "recallEnchantType", "null");
        recallEnchantTypeConfig.setComment("Possible targets for the enchantment. By default : null.\nPossible values are ALL, ARMOR, ARMOR_FEET, ARMOR_LEGS, ARMOR_TORSO, ARMOR_HEAD, WEAPON, DIGGER, FISHING_ROD, BREAKABLE, BOW, null.\nIf null or empty, Capsules will be the only items to be able to get this Enchantment.");
        Config.recallEnchantType = recallEnchantTypeConfig.getString();
    }

    public static BooleanSupplier isEnabled(String key) {
        return () -> !Config.capsuleSizes.containsKey(key) || Config.capsuleSizes.get(key) > 0;
    }


    public static String getRewardPathFromName(String structureName) {
        return rewardTemplatesPath + "/" + structureName;
    }

    public static JsonObject getBlueprintAllowedNBT(Block b) {
        return blueprintWhitelist.get(b.getRegistryName().toString());
    }

    /**
     * Identity NBT is NBT that is required to identify the item as a specific block. Ie. immersive engineering conveyor belts differs by their nbt but use the same block/item class.
     */
    public static List<String> getBlueprintIdentityNBT(Block b) {
        return getBlueprintAllowedNBT(b).entrySet().stream()
                .filter(ks -> !ks.getValue().isJsonNull())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
