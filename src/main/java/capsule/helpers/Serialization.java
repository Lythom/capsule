package capsule.helpers;

import net.minecraft.block.Block;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.Tag;
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

    public static List<Block> deserializeBlockList(List<String> blockIds) {
        ArrayList<Block> states = new ArrayList<>();
        ArrayList<String> notfound = new ArrayList<>();

        for (String blockId : blockIds) {
            ResourceLocation excludedLocation = new ResourceLocation(blockId);
            // is it a whole registryName to exclude ?
            if (StringUtils.isNullOrEmpty(excludedLocation.getPath())) {
                List<Block> blockIdsList = ForgeRegistries.BLOCKS.getValues().stream()
                        .filter(block -> {
                            ResourceLocation registryName = block.getRegistryName();
                            if (registryName == null) return false;
                            return registryName.toString().toLowerCase().contains(blockId.toLowerCase());
                        }).collect(Collectors.toList());
                if (blockIdsList.size() > 0) {
                    states.addAll(blockIdsList);
                } else {
                    notfound.add(blockId);
                }
            } else {
                // is it a block ?
                Block b = ForgeRegistries.BLOCKS.getValue(excludedLocation);
                if (b != null) {
                    // exclude the block
                    states.add(b);
                } else {
                    // is it a tag ?
                    Tag<Block> tag = BlockTags.getCollection().get(excludedLocation);
                    if (tag != null) {
                        // exclude all blocks from tag
                        states.addAll(tag.getAllElements());
                    }
                }
            }
        }
        if (notfound.size() > 0) {
            LOGGER.info(String.format(
                    "Blocks not found from config name : %s. Those blocks won't be considered in the overridable or excluded blocks list when capturing with capsule.",
                    String.join(", ", notfound.toArray(new CharSequence[0]))
            ));
        }

        Block[] output = new Block[states.size()];
        return states;
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
