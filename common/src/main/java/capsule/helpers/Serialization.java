package capsule.helpers;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringUtil;
import net.minecraft.world.level.block.Block;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Serialization {
    protected static final Logger LOGGER = LogManager.getLogger(Serialization.class);

    /**
     * Blocks matching the configured ids. An id ending with ':' selects every block of that namespace.
     * Ids that are neither blocks nor namespaces are tags, see deserializeBlockTags.
     */
    public static List<Block> deserializeBlockList(List<? extends String> blockIds) {
        ArrayList<Block> blocks = new ArrayList<>();
        ArrayList<String> notfound = new ArrayList<>();

        for (String blockId : blockIds) {
            if (blockId.startsWith("#")) continue;
            ResourceLocation location = ResourceLocation.tryParse(blockId);
            if (location == null) {
                notfound.add(blockId);
            } else if (StringUtil.isNullOrEmpty(location.getPath())) {
                List<Block> namespaceBlocks = BuiltInRegistries.BLOCK.entrySet().stream()
                        .filter(blockEntry -> blockEntry.getKey().location().getNamespace().equals(location.getNamespace()))
                        .map(Map.Entry::getValue)
                        .toList();
                if (namespaceBlocks.isEmpty()) notfound.add(blockId);
                blocks.addAll(namespaceBlocks);
            } else if (BuiltInRegistries.BLOCK.containsKey(location)) {
                blocks.add(BuiltInRegistries.BLOCK.get(location));
            }
        }
        if (!notfound.isEmpty()) {
            LOGGER.info(String.format(
                    "Blocks couldn't be resolved as Block or Tag from config name : %s. Those blocks won't be considered in the overridable or excluded blocks list when capturing with capsule.",
                    String.join(", ", notfound)
            ));
        }

        return blocks;
    }

    /**
     * Tags matching the configured ids, written with or without '#'. Their content is only known once tags are loaded.
     */
    public static List<TagKey<Block>> deserializeBlockTags(List<? extends String> blockIds) {
        ArrayList<TagKey<Block>> tags = new ArrayList<>();
        for (String blockId : blockIds) {
            ResourceLocation location = ResourceLocation.tryParse(blockId.startsWith("#") ? blockId.substring(1) : blockId);
            if (location != null && !StringUtil.isNullOrEmpty(location.getPath()) && (blockId.startsWith("#") || !BuiltInRegistries.BLOCK.containsKey(location))) {
                tags.add(TagKey.create(Registries.BLOCK, location));
            }
        }
        return tags;
    }

    public static String[] serializeBlockArray(Block[] states) {
        String[] blocksNames = new String[states.length];
        for (int i = 0; i < states.length; i++) {
            ResourceLocation registryName = BuiltInRegistries.BLOCK.getKey(states[i]);
            blocksNames[i] = registryName == null ? null : registryName.toString();
        }
        return blocksNames;
    }
}
