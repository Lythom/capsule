package capsule.helpers;

import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
            } else if (StringUtils.isNullOrEmpty(location.getPath())) {
                List<Block> namespaceBlocks = ForgeRegistries.BLOCKS.getValues().stream()
                        .filter(block -> block.getRegistryName() != null && block.getRegistryName().getNamespace().equals(location.getNamespace()))
                        .collect(Collectors.toList());
                if (namespaceBlocks.isEmpty()) notfound.add(blockId);
                blocks.addAll(namespaceBlocks);
            } else if (ForgeRegistries.BLOCKS.containsKey(location)) {
                blocks.add(ForgeRegistries.BLOCKS.getValue(location));
            }
        }
        if (!notfound.isEmpty()) {
            LOGGER.info(String.format(
                    "Blocks not found from config name : %s. Those blocks won't be considered in the overridable or excluded blocks list when capturing with capsule.",
                    String.join(", ", notfound)
            ));
        }

        return blocks;
    }

    /**
     * Ids of the tags matching the configured ids, written with or without '#'. Their content is only known once tags are loaded.
     */
    public static List<ResourceLocation> deserializeBlockTags(List<? extends String> blockIds) {
        ArrayList<ResourceLocation> tags = new ArrayList<>();
        for (String blockId : blockIds) {
            ResourceLocation location = ResourceLocation.tryParse(blockId.startsWith("#") ? blockId.substring(1) : blockId);
            if (location != null && !StringUtils.isNullOrEmpty(location.getPath()) && (blockId.startsWith("#") || !ForgeRegistries.BLOCKS.containsKey(location))) {
                tags.add(location);
            }
        }
        return tags;
    }

    public static String[] serializeBlockArray(Block[] states) {
        String[] blocksNames = new String[states.length];
        for (int i = 0; i < states.length; i++) {
            ResourceLocation registryName = states[i].getRegistryName();
            blocksNames[i] = registryName == null ? null : registryName.toString();
        }
        return blocksNames;
    }
}
