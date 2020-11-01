package capsule;

import capsule.helpers.Files;
import capsule.helpers.Serialization;
import capsule.loot.LootPathData;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.google.gson.JsonObject;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.world.storage.loot.LootTables;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = Main.MODID, bus = Bus.MOD)
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
        COMMON_BUILDER.comment("Loot settings").push(CATEGORY_LOOT);
        initLootConfigs();
        COMMON_BUILDER.pop();

        COMMON_BUILDER.comment("enchants settings").push(CATEGORY_ENCHANTS);
        initEnchantsConfigs();
        COMMON_BUILDER.pop();

        COMMON_BUILDER.comment("Balancing settings").push(CATEGORY_BALANCE);
        initCaptureConfigs();
        COMMON_BUILDER.pop();

        COMMON_CONFIG = COMMON_BUILDER.build();
        CLIENT_CONFIG = CLIENT_BUILDER.build();
    }

    // calculated and cached from init
    public static Map<String, LootPathData> lootTemplatesData = new HashMap<>();
    public static List<String> starterTemplatesList = new ArrayList<>();
    public static HashMap<String, JsonObject> blueprintWhitelist = new HashMap<>();
    public static List<Block> excludedBlocks;
    public static List<Block> overridableBlocks;
    public static List<Block> opExcludedBlocks;

    // provided by spec
    public static ForgeConfigSpec.ConfigValue<List<String>> excludedBlocksIds;
    public static ForgeConfigSpec.ConfigValue<List<String>> overridableBlocksIds;
    public static ForgeConfigSpec.ConfigValue<List<String>> opExcludedBlocksIds;
    public static ForgeConfigSpec.ConfigValue<List<String>> lootTemplatesPaths;
    public static ForgeConfigSpec.ConfigValue<List<String>> lootTablesList;
    public static ForgeConfigSpec.ConfigValue<String> starterTemplatesPath;
    public static ForgeConfigSpec.ConfigValue<String> prefabsTemplatesPath;
    public static ForgeConfigSpec.ConfigValue<String> rewardTemplatesPath;
    public static ForgeConfigSpec.IntValue upgradeLimit;
    public static ForgeConfigSpec.BooleanValue allowBlueprintReward;
    public static ForgeConfigSpec.ConfigValue<String> starterMode;

    public static ForgeConfigSpec.ConfigValue<String> enchantRarity;
    public static ForgeConfigSpec.ConfigValue<String> recallEnchantType;

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
        // init paths properties from config
        for (int i = 0; i < Config.lootTemplatesPaths.get().size(); i++) {
            String path = Config.lootTemplatesPaths.get().get(i);

            if (!Config.lootTemplatesData.containsKey(path)) {
                Config.lootTemplatesData.put(path, new LootPathData());
            }

            Config.lootTemplatesData.get(path).weigth = COMMON_BUILDER.comment("Chances to get a capsule from this folder. Higher means more common. Default : 2 (rare), 6 (uncommon) or 10 (common)")
                    .define("lootWeight:" + path, path.endsWith("rare") ? 2 : path.endsWith("uncommon") ? 6 : 10);
        }


    }

    @SubscribeEvent
    public static void onReload(final ModConfig.Reloading configEvent) {
        Files.populateAndLoadLootList(Config.configDir.toFile(), Config.lootTemplatesPaths.get(), Config.lootTemplatesData);
        Config.starterTemplatesList = Files.populateStarters(Config.configDir.toFile(), Config.starterTemplatesPath.get());
        Config.blueprintWhitelist = Files.populateWhitelistConfig(Config.configDir.toFile());
        Config.opExcludedBlocks = Serialization.deserializeBlockList(opExcludedBlocksIds.get());
        Config.excludedBlocks = Serialization.deserializeBlockList(excludedBlocksIds.get());
        Config.overridableBlocks = Serialization.deserializeBlockList(overridableBlocksIds.get());
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
        excludedBlocksIds = COMMON_BUILDER.comment("List of block ids that will never be captured by a non overpowered capsule. While capturing, the blocks will stay in place.\n Ex: minecraft:mob_spawner")
                .define("excludedBlocks", Arrays.asList(excludedBlocksStandardArray));

        opExcludedBlocksIds = COMMON_BUILDER.comment("List of block ids that will never be captured even with an overpowered capsule. While capturing, the blocks will stay in place.\nMod prefix usually indicate an incompatibility, remove at your own risk. See https://github.com/Lythom/capsule/wiki/Known-incompatibilities. \n Ex: minecraft:mob_spawner")
                .define("excludedBlocks", Arrays.asList(excludedBlocksOPArray));

        // Overridable
        List<Material> overridableMaterials = Arrays.asList(Material.AIR, Material.WATER, Material.LEAVES, Material.TALL_PLANTS, Material.SNOW);
        Block[] overridableBlocksList = ForgeRegistries.BLOCKS.getValues().stream()
                .filter(block -> overridableMaterials.contains(block.getDefaultState().getMaterial()))
                .toArray(Block[]::new);

        overridableBlocksIds = COMMON_BUILDER.comment("List of block ids that can be overriden while teleporting blocks.\nPut there blocks that the player don't care about (grass, leaves) so they don't prevent the capsule from deploying.")
                .define("overridableBlocks", Arrays.asList(Serialization.serializeBlockArray(overridableBlocksList)));
    }

    public static void initLootConfigs() {

        // Loot tables that can reward a capsule
        List<String> defaultLootTablesList = Arrays.asList(
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
                LootTables.CHESTS_WOODLAND_MANSION.toString());

        Config.lootTablesList = COMMON_BUILDER.comment("List of loot tables that will eventually reward a capsule.\n Example of valid loot tables : gameplay/fishing/treasure, chests/spawn_bonus_chest, entities/villager (killing a villager).\nAlso see https://minecraft.gamepedia.com/Loot_table#List_of_loot_tables.")
                .define("lootTablesList", defaultLootTablesList);

        Config.lootTemplatesPaths = COMMON_BUILDER.comment("List of paths where the mod will look for structureBlock files. Each save structure have a chance to appear as a reward capsule in a dungeon chest.\nTo Lower the chance of getting a capsule at all, insert an empty folder here and configure its weight accordingly (more weigth on empty folder = less capsule chance per chest).")
                .define("lootTemplatesPaths", Arrays.asList(
                        "config/capsule/loot/common",
                        "config/capsule/loot/uncommon",
                        "config/capsule/loot/rare"
                ));

        Config.starterMode = COMMON_BUILDER.comment("Players can be given one or several starter structures on their first arrival.\nThose structures nbt files can be placed in the folder defined at starterTemplatesPath below.\nPossible values : \"all\", \"random\", or \"none\".\nDefault value: \"random\"")
                .define("starterMode", "random");

        Config.starterTemplatesPath = COMMON_BUILDER.comment("Each structure in this folder will be given to the player as standard reusable capsule on game start.\nEmpty the folder or the value to disable starter capsules.\nDefault value: \"config/capsule/starters\"")
                .define("starterTemplatesPath", "config/capsule/starters");

        Config.prefabsTemplatesPath = COMMON_BUILDER.comment("Each structure in this folder will auto-generate a blueprint recipe that player will be able to craft.\nRemove/Add structure in the folder to disable/enable the recipe.\nDefault value: \"config/capsule/prefabs\"")
                .define("prefabsTemplatesPath", "config/capsule/prefabs");

        Config.rewardTemplatesPath = COMMON_BUILDER.comment("Paths where the mod will look for structureBlock files when invoking command /capsule fromExistingRewards <structureName> [playerName].")
                .define("rewardTemplatesPath", "config/capsule/rewards");

        Config.allowBlueprintReward = COMMON_BUILDER.comment("If true, loot rewards will be pre-charged blueprint when possible (if the content contains no entity).\nIf false loot reward will always be one-use capsules.\nDefault value: true")
                .define("allowBlueprintReward", true);
    }

    public static void initEnchantsConfigs() {

        Config.enchantRarity = COMMON_BUILDER.comment("Rarity of the enchantmant. Possible values : COMMON, UNCOMMON, RARE, VERY_RARE. Default: RARE.")
                .define("recallEnchantRarity", "RARE");

        Config.recallEnchantType = COMMON_BUILDER.comment("Possible targets for the enchantment. By default : null.\nPossible values are ALL, ARMOR, ARMOR_FEET, ARMOR_LEGS, ARMOR_TORSO, ARMOR_HEAD, WEAPON, DIGGER, FISHING_ROD, BREAKABLE, BOW, null.\nIf null or empty, Capsules will be the only items to be able to get this Enchantment.")
                .define("recallEnchantType", "null");
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
