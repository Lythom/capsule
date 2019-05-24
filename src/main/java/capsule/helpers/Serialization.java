package capsule.helpers;

import net.minecraft.block.Block;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

public class Serialization {
    protected static final Logger LOGGER = LogManager.getLogger(Serialization.class);

    public static Block[] deserializeBlockArray(String[] blockIds) {
        ArrayList<Block> states = new ArrayList<>();
        ArrayList<String> notfound = new ArrayList<>();

        for (String blockId : blockIds) {
            Block b = Block.getBlockFromName(blockId);
            if (b != null) {
                states.add(b);
            } else {
                notfound.add(blockId);
            }
        }
        if (notfound.size() > 0) {
            LOGGER.warn(String.format(
                    "Blocks not found from config name : %s. Those blocks won't be considered in the overridable or excluded blocks list when capturing with capsule.",
                    String.join(", ", notfound.toArray(new CharSequence[0]))
            ));
        }
        Block[] output = new Block[states.size()];
        return states.toArray(output);
    }

    public static String[] serializeBlockArray(Block[] states) {

        String[] blocksNames = new String[states.length];
        for (int i = 0; i < states.length; i++) {
            blocksNames[i] = Block.REGISTRY.getNameForObject(states[i]).toString();
        }
        return blocksNames;

    }
}
