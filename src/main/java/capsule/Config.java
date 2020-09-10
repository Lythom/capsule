package capsule;

import capsule.helpers.Serialization;
import capsule.loot.LootPathData;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.google.gson.JsonObject;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.block.pattern.BlockMaterialMatcher;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootTables;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;

import java.nio.file.Path;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@EventBusSubscriber
public class Config {

    protected static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(Config.class);

    private static final ForgeConfigSpec.Builder COMMON_BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();
    public static ForgeConfigSpec COMMON_CONFIG;
    public static ForgeConfigSpec CLIENT_CONFIG;

    public static final String CATEGORY_BALANCE = "balance";
    public static final String CATEGORY_LOOT = "loot";
    public static final String CATEGORY_RECIPE = "recipe";
    public static final String CATEGORY_ENCHANTS = "enchants";

    static {
        COMMON_BUILDER.comment("Balancing settings").push(CATEGORY_BALANCE);
        COMMON_BUILDER.pop();
        initCaptureConfigs();

        COMMON_BUILDER.comment("Loot settings").push(CATEGORY_LOOT);
        COMMON_BUILDER.pop();
        initLootConfigs();

        COMMON_BUILDER.comment("Recipe settings").push(CATEGORY_RECIPE);
        COMMON_BUILDER.pop();
        initRecipesConfigs();

        COMMON_BUILDER.comment("enchants settings").push(CATEGORY_ENCHANTS);
        COMMON_BUILDER.pop();
        initEnchantsConfigs();

        COMMON_BUILDER.pop();
        COMMON_CONFIG = COMMON_BUILDER.build();
        CLIENT_CONFIG = CLIENT_BUILDER.build();
    }

    public static ForgeConfigSpec.ConfigValue<List<Block>> excludedBlocks;
    public static ForgeConfigSpec.ConfigValue<List<Block>> overridableBlocks;
    public static ForgeConfigSpec.ConfigValue<List<Block>> opExcludedBlocks;
    public static ForgeConfigSpec.ConfigValue<String[]> lootTemplatesPaths;
    public static ForgeConfigSpec.ConfigValue<List<String>> lootTablesList;
    public static ForgeConfigSpec.ConfigValue<Map<String, LootPathData>> lootTemplatesData;
    public static ForgeConfigSpec.ConfigValue<String> starterTemplatesPath;
    public static ForgeConfigSpec.ConfigValue<List<String>> starterTemplatesList;
    public static ForgeConfigSpec.ConfigValue<String> prefabsTemplatesPath;
    public static ForgeConfigSpec.ConfigValue<String> rewardTemplatesPath;
    public static ForgeConfigSpec.IntValue upgradeLimit;
    public static ForgeConfigSpec.ConfigValue<HashMap<String, JsonObject>> blueprintWhitelist;
    public static ForgeConfigSpec.BooleanValue allowBlueprintReward;
    public static ForgeConfigSpec.ConfigValue<String> starterMode;

    public static ForgeConfigSpec.ConfigValue<String> enchantRarity;
    public static ForgeConfigSpec.ConfigValue<String> recallEnchantType;
    public static ForgeConfigSpec.ConfigValue<Map<String, Integer>> capsuleSizes;

    public static Supplier<Integer> ironCapsuleSize = () -> capsuleSizes.get().get("ironCapsuleSize");
    public static Supplier<Integer> goldCapsuleSize = () -> capsuleSizes.get().get("goldCapsuleSize");
    public static Supplier<Integer> diamondCapsuleSize = () -> capsuleSizes.get().get("diamondCapsuleSize");
    public static Supplier<Integer> opCapsuleSize = () -> capsuleSizes.get().get("opCapsuleSize");

    public static Path configDir = null;

    public static void loadConfig(ForgeConfigSpec spec, Path path) {

        final CommentedFileConfig configData = CommentedFileConfig.builder(path)
                .sync()
                .autosave()
                .writingMode(WritingMode.REPLACE)
                .build();

        configData.load();
        spec.setConfig(configData);
    }

    @SubscribeEvent
    public static void onLoad(final ModConfig.Loading configEvent) {

    }

    @SubscribeEvent
    public static void onReload(final ModConfig.Reloading configEvent) {
    }


    public static void initCaptureConfigs() {

        // upgrade limits
        upgradeLimit = COMMON_BUILDER.comment("Number of upgrades an empty capsule can get to improve capacity. If <= 0, the capsule won't be able to upgrade.")
                .defineInRange("capsuleUpgradesLimit", 10, 0, Integer.MAX_VALUE);

        // Excluded
        Block[] defaultExcludedBlocksOP = new Block[]{Blocks.AIR, Blocks.STRUCTURE_VOID, Blocks.BEDROCK};
        Block[] defaultExcludedBlocks = new Block[]{Blocks.SPAWNER, Blocks.END_PORTAL, Blocks.END_PORTAL_FRAME};

        String[] excludedBlocksOPArray = ArrayUtils.addAll(
                Serialization.serializeBlockArray(defaultExcludedBlocksOP),
                "ic2:",
                "refinedstorage:",
                "superfactorymanager:",
                "gregtech:machine",
                "gtadditions:",
                "bloodmagic:alchemy_table",
                "mekanism:machineblock",
                "mekanism:boundingblock"
        );
        String[] excludedBlocksStandardArray = ArrayUtils.addAll(
                Serialization.serializeBlockArray(defaultExcludedBlocks),
                excludedBlocksOPArray
        );
        excludedBlocks = COMMON_BUILDER.comment("List of block ids that will never be captured by a non overpowered capsule. While capturing, the blocks will stay in place.\n Ex: minecraft:mob_spawner")
                .define("excludedBlocks", Serialization.deserializeBlockList(excludedBlocksStandardArray));

        opExcludedBlocks = COMMON_BUILDER.comment("List of block ids that will never be captured even with an overpowered capsule. While capturing, the blocks will stay in place.\nMod prefix usually indicate an incompatibility, remove at your own risk. See https://github.com/Lythom/capsule/wiki/Known-incompatibilities. \n Ex: minecraft:mob_spawner")
                .define("excludedBlocks", Serialization.deserializeBlockList(excludedBlocksOPArray));

        // Overridable
        List<Material> overridableMaterials = Arrays.asList(Material.AIR, Material.WATER, Material.LEAVES, Material.TALL_PLANTS, Material.SNOW);
        List<Block> overridableBlocksList = ForgeRegistries.BLOCKS.getValues().stream()
                .filter(block -> overridableMaterials.contains(block.getDefaultState().getMaterial()))
                .collect(Collectors.toList());
        overridableBlocks = COMMON_BUILDER.comment("List of block ids that can be overriden while teleporting blocks.\nPut there blocks that the player don't care about (grass, leaves) so they don't prevent the capsule from deploying.")
                .define("overridableBlocks", overridableBlocksList);
    }

    public static void initLootConfigs() {

        // Loot tables that can reward a capsule
        String[] defaultLootTablesList = new String[]{
                LootTables.CHESTS_ABANDONED_MINESHAFT.toString(),
                LootTables.CHESTS_DESERT_PYRAMID.toString(),
                LootTables.CHESTS_END_CITY_TREASURE.toString(),
                LootTables.CHESTS_IGLOO_CHEST.toString(),
                LootTables.CHESTS_JUNGLE_TEMPLE.toString(),
                LootTables.CHESTS_SIMPLE_DUNGEON.toString(),
                LootTables.CHESTS_STRONGHOLD_CORRIDOR.toString(),
                LootTables.CHESTS_STRONGHOLD_CROSSING.toString(),
                LootTables.CHESTS_STRONGHOLD_LIBRARY.toString(),
                LootTables.CHESTS_VILLAGE_VILLAGE_TOOLSMITH.toString(),
                LootTables.CHESTS_VILLAGE_VILLAGE_ARMORER.toString(),
                LootTables.CHESTS_VILLAGE_VILLAGE_TEMPLE.toString(),
                LootTables.CHESTS_VILLAGE_VILLAGE_WEAPONSMITH.toString(),
                LootTables.CHESTS_BURIED_TREASURE.toString(),
                LootTables.CHESTS_JUNGLE_TEMPLE_DISPENSER.toString(),
                LootTables.CHESTS_PILLAGER_OUTPOST.toString(),
                LootTables.CHESTS_SHIPWRECK_TREASURE.toString(),
                LootTables.CHESTS_UNDERWATER_RUIN_BIG.toString(),
                LootTables.CHESTS_UNDERWATER_RUIN_SMALL.toString(),
                LootTables.CHESTS_WOODLAND_MANSION.toString()
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

        Property starterModeProp = Config.config.get("loots", "starterMode", "random");
        starterModeProp.setComment("Players can be given one or several starter structures on their first arrival.\nThose structures nbt files can be placed in the folder defined at starterTemplatesPath below.\nPossible values : \"all\", \"random\", or \"none\".\nDefault value: \"random\"");
        Config.starterMode = starterModeProp.getString();

        Property starterTemplatesPathProp = Config.config.get("loots", "starterTemplatesPath", "config/capsule/starters");
        starterTemplatesPathProp.setComment("Each structure in this folder will be given to the player as standard reusable capsule on game start.\nEmpty the folder or the value to disable starter capsules.\nDefault value: \"config/capsule/starters\"");
        Config.starterTemplatesPath = starterTemplatesPathProp.getString();

        Property prefabsTemplatesPathProp = Config.config.get("loots", "prefabsTemplatesPath", "config/capsule/prefabs");
        prefabsTemplatesPathProp.setComment("Each structure in this folder will auto-generate a blueprint recipe that player will be able to craft.\nRemove/Add structure in the folder to disable/enable the recipe.\nDefault value: \"config/capsule/prefabs\"");
        Config.prefabsTemplatesPath = prefabsTemplatesPathProp.getString();

        Property rewardTemplatesPathProp = Config.config.get("loots", "rewardTemplatesPath", "config/capsule/rewards");
        rewardTemplatesPathProp.setComment("Paths where the mod will look for structureBlock files when invoking command /capsule fromExistingRewards <structureName> [playerName].");
        Config.rewardTemplatesPath = rewardTemplatesPathProp.getString();

        Property allowBlueprintRewardProp = Config.config.get("loots", "allowBlueprintReward", true);
        allowBlueprintRewardProp.setComment("If true, loot rewards will be pre-charged blueprint when possible (if the content contains no entity).\nIf false loot reward will always be one-use capsules.\nDefault value: true");
        Config.allowBlueprintReward = allowBlueprintRewardProp.getBoolean();

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

    public static void initRecipesConfigs() {
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
        JsonObject allowedNBT = getBlueprintAllowedNBT(b);
        if (allowedNBT == null) return null;
        return allowedNBT.entrySet().stream()
                .filter(ks -> !ks.getValue().isJsonNull())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
