package capsule;

import capsule.items.CapsuleItem;
import capsule.loot.LootPathData;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {

    protected static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(CapsuleItem.class);

    public static Configuration config = null;
    public static List<Block> excludedBlocks;
    public static List<Block> overridableBlocks;
    public static List<Block> opExcludedBlocks;
    public static String[] lootTemplatesPaths;
    public static Map<String, LootPathData> lootTemplatesData = new HashMap<>();
    public static String rewardTemplatesPath;
    public static int upgradeLimit;

    public static String enchantRarity;
    public static String recallEnchantType;
    public static int ironCapsuleSize;
    public static int goldCapsuleSize;
    public static int diamondCapsuleSize;
    public static int opCapsuleSize;

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
        upgradesLimit.setComment("Number of upgrades an empty capsules can get to improve capacity. If <= 0, the capsule won't be able to upgrade.");
        Config.upgradeLimit = upgradesLimit.getInt();

        // Excluded
        Block[] defaultExcludedBlocksOP = new Block[]{Blocks.AIR, Blocks.STRUCTURE_VOID};
        Block[] defaultExcludedBlocks = new Block[]{Blocks.BEDROCK, Blocks.MOB_SPAWNER, Blocks.END_PORTAL, Blocks.END_PORTAL_FRAME};

        String[] excludedBlocksOP = ArrayUtils.addAll(
                Helpers.serializeBlockArray(defaultExcludedBlocksOP),
                new String[]{
                        "ic2:te",
                        "opencomputers:robot",
                        "lootbags:loot_opener",
                        "hatchery:pen"
                }
        );
        String[] excludedBlocks = ArrayUtils.addAll(
                Helpers.serializeBlockArray(defaultExcludedBlocks),
                excludedBlocksOP
        );
        Property excludedBlocksProp = Config.config.get("Balancing", "excludedBlocks", excludedBlocks);
        excludedBlocksProp.setComment("List of block ids that will never be captured by a non overpowered capsule. While capturing, the blocks will stay in place.\n Ex: minecraft:mob_spawner");
        Block[] exBlocks = null;
        exBlocks = Helpers.deserializeBlockArray(excludedBlocksProp.getStringList());
        Config.excludedBlocks = Arrays.asList(exBlocks);

        // OP Excluded
        Property opExcludedBlocksProp = Config.config.get("Balancing", "opExcludedBlocks", excludedBlocksOP);
        opExcludedBlocksProp.setComment("List of block ids that will never be captured even with an overpowered capsule. While capturing, the blocks will stay in place.\n Ex: minecraft:mob_spawner");
        Block[] opExBlocks = null;

        opExBlocks = Helpers.deserializeBlockArray(opExcludedBlocksProp.getStringList());
        Config.opExcludedBlocks = Arrays.asList(opExBlocks);


        // Overridable
        Block[] defaultOverridable = new Block[]{Blocks.AIR, Blocks.WATER, Blocks.LEAVES,
                Blocks.LEAVES2, Blocks.TALLGRASS, Blocks.RED_FLOWER, Blocks.YELLOW_FLOWER,
                Blocks.SNOW_LAYER, Blocks.BROWN_MUSHROOM, Blocks.RED_MUSHROOM, Blocks.DOUBLE_PLANT};
        Property overridableBlocksProp = Config.config.get(
                "Balancing",
                "overridableBlocks",
                Helpers.serializeBlockArray(defaultOverridable)
        );
        overridableBlocksProp.setComment("List of block ids that can be overriden while teleporting blocks.\nPut there blocks that the player don't care about (grass, leaves) so they don't prevent the capsule from deploying.");

        Block[] ovBlocks = null;
        ovBlocks = Helpers.deserializeBlockArray(overridableBlocksProp.getStringList());
        Config.overridableBlocks = Arrays.asList(ovBlocks);
    }

    public static void initLootConfigs() {

        // CapsuleTemplate Paths
        Property lootTemplatesPathsProp = Config.config.get("loots", "lootTemplatesPaths", new String[]{
                "config/capsules/loot/common",
                "config/capsules/loot/uncommon",
                "config/capsules/loot/rare",
                "assets/capsules/loot/common",
                "assets/capsules/loot/uncommon",
                "assets/capsules/loot/rare"
        });
        lootTemplatesPathsProp.setComment("List of paths where the mod will look for structureBlock files. Each save will have a chance to appear as a reward capsule in a dungeon chest.");
        Config.lootTemplatesPaths = lootTemplatesPathsProp.getStringList();

        Property rewardTemplatesPathProp = Config.config.get("loots", "rewardTemplatesPath", "config/capsules/rewards");
        rewardTemplatesPathProp.setComment("Paths where the mod will look for structureBlock files when invoking command /capsule fromStructure <structureName>.");
        Config.rewardTemplatesPath = rewardTemplatesPathProp.getString();

        // init paths properties from config
        for (int i = 0; i < Config.lootTemplatesPaths.length; i++) {
            String path = Config.lootTemplatesPaths[i];

            if (!Config.lootTemplatesData.containsKey(path)) {
                Config.lootTemplatesData.put(path, new LootPathData());
            }
            Property pathDataWeight = Config.config.get("loots:" + path, "weight", path.endsWith("rare") ? 1 : path.endsWith("uncommon") ? 6 : 12);
            pathDataWeight.setComment("Chances to get a capsule from this folder. Higher means more common. Default : 1 (rare), 6 (uncommon) or 12 (common)");
            Config.lootTemplatesData.get(path).weigth = pathDataWeight.getInt();
        }
    }

    public static void initReceipeConfigs() {
        Property ironCapsuleSize = Config.config.get("Balancing", "ironCapsuleSize", "1");
        ironCapsuleSize.setComment("Size of the capture cube side for an Iron Capsule. Must be an Odd Number (or it will be rounded down with error message).\n0 to disable.\nDefault: 1");
        Config.ironCapsuleSize = ironCapsuleSize.getInt();

        Property goldCapsuleSize = Config.config.get("Balancing", "goldCapsuleSize", "3");
        goldCapsuleSize.setComment("Size of the capture cube side for a Gold Capsule. Must be an Odd Number (or it will be rounded down with error message).\n0 to disable.\nDefault: 3");
        Config.goldCapsuleSize = goldCapsuleSize.getInt();

        Property diamondCapsuleSize = Config.config.get("Balancing", "diamondCapsuleSize", "5");
        diamondCapsuleSize.setComment("Size of the capture cube side for a Diamond Capsule. Must be an Odd Number (or it will be rounded down with error message).\n0 to disable.\nDefault: 5");
        Config.diamondCapsuleSize = diamondCapsuleSize.getInt();

        Property opCapsuleSize = Config.config.get("Balancing", "opCapsuleSize", "1");
        opCapsuleSize.setComment("Size of the capture cube side for a Overpowered Capsule. Must be an Odd Number (or it will be rounded down with error message).\n0 to disable.\nDefault: 1");
        Config.opCapsuleSize = opCapsuleSize.getInt();
    }

    public static void initEnchantsConfigs() {
        Property enchantRarityConfig = Config.config.get("Balancing", "recallEnchantRarity", "RARE");
        enchantRarityConfig.setComment("Rarity of the enchantmant. Possible values : COMMON, UNCOMMON, RARE, VERY_RARE. Default: RARE.");
        Config.enchantRarity = enchantRarityConfig.getString();

        Property recallEnchantTypeConfig = Config.config.get("Balancing", "recallEnchantType", "null");
        recallEnchantTypeConfig.setComment("Possible targets for the enchantment. By default : null.\nPossible values are ALL, ARMOR, ARMOR_FEET, ARMOR_LEGS, ARMOR_TORSO, ARMOR_HEAD, WEAPON, DIGGER, FISHING_ROD, BREAKABLE, BOW, null.\nIf null or empty, Capsules will be the only items to be able to get this Enchantment.");
        Config.recallEnchantType = recallEnchantTypeConfig.getString();
    }


}
