package capsule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import capsule.loot.LootPathData;
import net.minecraft.block.Block;
import net.minecraftforge.common.config.Configuration;

public class Config {
	public static Configuration config = null;
	public static List<Block> excludedBlocks;
	public static List<Block> overridableBlocks;
	public static List<Block> opExcludedBlocks;
	public static String[] lootTemplatesPaths;
	public static Map<String,LootPathData> lootTemplatesData = new HashMap<>();
	public static String rewardTemplatesPath;
}
