package capsule.structure;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Absolute world positions stored in block entity NBT: a position of a block that moves with the capsule is stored
 * relative to the template origin at capture, and becomes absolute again at deployment. Other positions are kept.
 */
public class AbsolutePositions {
    protected static final Logger LOGGER = LogManager.getLogger(AbsolutePositions.class);

    /**
     * Key of a block entity NBT in a template: the paths of the positions stored relative to the template origin.
     */
    public static final String RELATIVE_PATHS = "capsule:relative_positions";
    /**
     * Sibling key of a position written by GlobalPos.CODEC.
     */
    private static final String DIMENSION = "dimension";
    private static final List<List<String>> XYZ_KEYS = List.of(List.of("x", "y", "z"), List.of("X", "Y", "Z"));

    public enum Format {
        /** [I; x, y, z], written by NbtUtils.writeBlockPos and BlockPos.CODEC */
        INT_ARRAY,
        /** a compound of the int keys x, y, z or X, Y, Z */
        XYZ,
        /** BlockPos.asLong, alone or as an element of a long array */
        LONG
    }

    /**
     * The values of a block entity NBT that are positions: the values of these formats, under one of these keys when
     * some are given, anywhere otherwise.
     */
    public record Rule(Set<Format> formats, Set<String> under) {
        public static final Rule DEFAULT = new Rule(EnumSet.of(Format.INT_ARRAY, Format.XYZ), Set.of());
    }

    private record Capture(Rule rule, BlockPos origin, Predicate<BlockPos> moves, String dimension, ListTag paths) {
    }

    /**
     * Stores relative to origin the positions of nbt that the rule selects and that moves accepts. A position written
     * with a dimension is relative only when it is the dimension of the capture.
     */
    public static void toRelative(CompoundTag nbt, Rule rule, BlockPos origin, Predicate<BlockPos> moves, String dimension) {
        Capture capture = new Capture(rule, origin, moves, dimension, new ListTag());
        relativize(nbt, List.of(), rule.under().isEmpty(), capture);
        if (!capture.paths().isEmpty()) nbt.put(RELATIVE_PATHS, capture.paths());
    }

    private static void relativize(Tag container, List<String> path, boolean inScope, Capture capture) {
        if (container instanceof CompoundTag compound) {
            for (String key : List.copyOf(compound.getAllKeys())) {
                Tag relative = relativeValue(compound.get(key), compound, path, key, inScope || capture.rule().under().contains(key), capture);
                if (relative != null) compound.put(key, relative);
            }
        } else if (container instanceof ListTag || container instanceof LongArrayTag && capture.rule().formats().contains(Format.LONG)) {
            CollectionTag<?> list = (CollectionTag<?>) container;
            for (int i = 0; i < list.size(); i++) {
                Tag relative = relativeValue(list.get(i), null, path, String.valueOf(i), inScope, capture);
                if (relative != null) list.setTag(i, relative);
            }
        }
    }

    /**
     * The relative form of value when it is a position that moves, else null once its content is visited.
     */
    @Nullable
    private static Tag relativeValue(Tag value, @Nullable CompoundTag parent, List<String> parentPath, String key, boolean inScope, Capture capture) {
        List<String> path = new ArrayList<>(parentPath);
        path.add(key);
        BlockPos pos = inScope ? read(value, capture.rule().formats()) : null;
        if (pos == null) {
            relativize(value, path, inScope, capture);
            return null;
        }
        boolean otherDimension = parent != null && parent.contains(DIMENSION, Tag.TAG_STRING) && !parent.getString(DIMENSION).equals(capture.dimension());
        if (otherDimension || !capture.moves().test(pos)) return null;
        ListTag pathTag = new ListTag();
        path.forEach(segment -> pathTag.add(StringTag.valueOf(segment)));
        capture.paths().add(pathTag);
        return write(value, pos.subtract(capture.origin()));
    }

    /**
     * Makes absolute the positions that toRelative stored relative, with toWorld, and sets their dimension.
     */
    public static void toAbsolute(CompoundTag nbt, UnaryOperator<BlockPos> toWorld, String dimension) {
        ListTag paths = nbt.getList(RELATIVE_PATHS, Tag.TAG_LIST);
        nbt.remove(RELATIVE_PATHS);
        for (int i = 0; i < paths.size(); i++) {
            ListTag path = paths.getList(i);
            Tag parent = nbt;
            for (int j = 0; j < path.size() - 1 && parent != null; j++) {
                parent = child(parent, path.getString(j));
            }
            String key = path.getString(path.size() - 1);
            Tag value = parent == null ? null : child(parent, key);
            BlockPos relative = value == null ? null : read(value, EnumSet.allOf(Format.class));
            // a blueprint keeps only some keys of the block entity
            if (relative == null) continue;
            Tag absolute = write(value, toWorld.apply(relative));
            if (parent instanceof CompoundTag compound) {
                compound.put(key, absolute);
                if (compound.contains(DIMENSION, Tag.TAG_STRING)) compound.putString(DIMENSION, dimension);
            } else {
                ((CollectionTag<?>) parent).setTag(Integer.parseInt(key), absolute);
            }
        }
    }

    @Nullable
    private static Tag child(Tag parent, String key) {
        if (parent instanceof CompoundTag compound) return compound.get(key);
        if (parent instanceof CollectionTag<?> list) {
            try {
                int index = Integer.parseInt(key);
                return index >= 0 && index < list.size() ? list.get(index) : null;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    @Nullable
    static BlockPos read(Tag value, Set<Format> formats) {
        if (value instanceof IntArrayTag array && array.size() == 3 && formats.contains(Format.INT_ARRAY)) {
            int[] xyz = array.getAsIntArray();
            return new BlockPos(xyz[0], xyz[1], xyz[2]);
        }
        if (value instanceof CompoundTag compound && formats.contains(Format.XYZ)) {
            List<String> keys = xyzKeys(compound);
            if (keys != null) return new BlockPos(compound.getInt(keys.get(0)), compound.getInt(keys.get(1)), compound.getInt(keys.get(2)));
        }
        if (value instanceof LongTag packed && formats.contains(Format.LONG)) {
            return BlockPos.of(packed.getAsLong());
        }
        return null;
    }

    private static Tag write(Tag original, BlockPos pos) {
        if (original instanceof IntArrayTag) return NbtUtils.writeBlockPos(pos);
        if (original instanceof LongTag) return LongTag.valueOf(pos.asLong());
        CompoundTag compound = ((CompoundTag) original).copy();
        List<String> keys = xyzKeys(compound);
        compound.putInt(keys.get(0), pos.getX());
        compound.putInt(keys.get(1), pos.getY());
        compound.putInt(keys.get(2), pos.getZ());
        return compound;
    }

    @Nullable
    private static List<String> xyzKeys(CompoundTag compound) {
        for (List<String> keys : XYZ_KEYS) {
            if (keys.stream().allMatch(key -> compound.contains(key, Tag.TAG_INT))) return keys;
        }
        return null;
    }

    /**
     * Reads the rules of config/capsule/position_rules.json, by block id or by namespace written "modid:".
     * Ex: [{"block": "somemod:teleporter", "formats": ["long"], "under": ["target"]}, {"block": "othermod:", "formats": []}]
     */
    public static Map<String, Rule> parseRules(JsonArray json) {
        Map<String, Rule> rules = new HashMap<>();
        for (JsonElement element : json) {
            try {
                JsonObject rule = element.getAsJsonObject();
                Set<Format> formats = EnumSet.noneOf(Format.class);
                if (rule.has("formats")) {
                    for (JsonElement format : rule.getAsJsonArray("formats")) {
                        formats.add(Format.valueOf(format.getAsString().toUpperCase(Locale.ROOT)));
                    }
                } else {
                    formats.addAll(Rule.DEFAULT.formats());
                }
                Set<String> under = new HashSet<>();
                if (rule.has("under")) {
                    for (JsonElement key : rule.getAsJsonArray("under")) under.add(key.getAsString());
                }
                rules.put(rule.get("block").getAsString(), new Rule(formats, Set.copyOf(under)));
            } catch (RuntimeException e) {
                LOGGER.error("Ignored the position rule {}: {}", element, e.toString());
            }
        }
        return rules;
    }

    public static Rule ruleFor(Map<String, Rule> rules, ResourceLocation blockId) {
        Rule rule = rules.get(blockId.toString());
        if (rule == null) rule = rules.get(blockId.getNamespace() + ":");
        return rule == null ? Rule.DEFAULT : rule;
    }
}
