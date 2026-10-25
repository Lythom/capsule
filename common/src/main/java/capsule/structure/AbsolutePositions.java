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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Absolute world positions stored in the NBT of the block entities and entities that a rule of
 * config/capsule/position_rules.json lists: a position of a block that moves with the capsule is stored relative to
 * the template origin at capture, and becomes absolute again at deployment. Other positions are kept.
 * <p>
 * A rule lists paths of NBT keys separated by '.': "key[]" goes through every element of a list, "prefix*" through
 * every key starting with prefix. The value at a path is a position when it is [I; x, y, z], a compound of the int keys
 * x, y, z or X, Y, Z, a GlobalPos {dimension, pos}, a long (BlockPos.asLong) or a long array of them.
 */
public class AbsolutePositions {
    protected static final Logger LOGGER = LogManager.getLogger(AbsolutePositions.class);

    /**
     * Key of a block entity or entity NBT in a template: the paths of the positions stored relative to the template
     * origin.
     */
    public static final String RELATIVE_PATHS = "capsule:relative_positions";
    private static final List<List<String>> XYZ_KEYS = List.of(List.of("x", "y", "z"), List.of("X", "Y", "Z"));

    /**
     * The paths of the rule of id, else of its namespace, none without rule.
     */
    public static List<String> pathsFor(Map<String, List<String>> rules, ResourceLocation id) {
        List<String> paths = rules.get(id.toString());
        if (paths == null) paths = rules.get(id.getNamespace() + ":");
        return paths == null ? List.of() : paths;
    }

    /**
     * Stores relative to origin the positions at the rule paths that moves accepts. A GlobalPos is relative only when
     * it is in the dimension of the capture.
     */
    public static void toRelative(CompoundTag nbt, List<String> rulePaths, BlockPos origin, Predicate<BlockPos> moves, String dimension) {
        ListTag converted = new ListTag();
        for (String rulePath : rulePaths) {
            for (List<String> path : resolve(nbt, rulePath)) {
                Tag value = get(nbt, path);
                BlockPos pos = read(value);
                if (pos == null || !moves.test(pos) || value instanceof CompoundTag globalPos && isGlobalPos(globalPos) && !globalPos.getString("dimension").equals(dimension)) {
                    continue;
                }
                set(nbt, path, write(value, pos.subtract(origin), null));
                ListTag pathTag = new ListTag();
                path.forEach(segment -> pathTag.add(StringTag.valueOf(segment)));
                converted.add(pathTag);
            }
        }
        if (!converted.isEmpty()) nbt.put(RELATIVE_PATHS, converted);
    }

    /**
     * Makes absolute the positions that toRelative stored relative, with toWorld, in dimension.
     */
    public static void toAbsolute(CompoundTag nbt, UnaryOperator<BlockPos> toWorld, String dimension) {
        convert(nbt, relative -> true, toWorld, dimension);
        nbt.remove(RELATIVE_PATHS);
    }

    /**
     * Makes absolute again the relative positions of blocks that stay in the world, which stayed accepts.
     */
    public static void keepAbsolute(CompoundTag nbt, BlockPos origin, Predicate<BlockPos> stayed) {
        ListTag remaining = convert(nbt, stayed, relative -> relative.offset(origin), null);
        if (remaining.isEmpty()) {
            nbt.remove(RELATIVE_PATHS);
        } else {
            nbt.put(RELATIVE_PATHS, remaining);
        }
    }

    /**
     * A copy of nbt where the relative positions are all BlockPos.ZERO: they depend on where the template was captured.
     */
    public static CompoundTag withoutRelativePositions(CompoundTag nbt) {
        CompoundTag copy = nbt.copy();
        convert(copy, relative -> true, relative -> BlockPos.ZERO, null);
        copy.remove(RELATIVE_PATHS);
        return copy;
    }

    /**
     * Converts the relative positions that which accepts, and returns the paths of the others.
     */
    private static ListTag convert(CompoundTag nbt, Predicate<BlockPos> which, UnaryOperator<BlockPos> conversion, @Nullable String dimension) {
        ListTag paths = nbt.getList(RELATIVE_PATHS, Tag.TAG_LIST);
        ListTag remaining = new ListTag();
        for (int i = 0; i < paths.size(); i++) {
            ListTag pathTag = paths.getList(i);
            List<String> path = new ArrayList<>();
            for (int j = 0; j < pathTag.size(); j++) path.add(pathTag.getString(j));
            Tag value = get(nbt, path);
            BlockPos relative = value == null ? null : read(value);
            // a blueprint keeps only some keys of the block entity
            if (relative == null) continue;
            if (which.test(relative)) {
                set(nbt, path, write(value, conversion.apply(relative), dimension));
            } else {
                remaining.add(pathTag);
            }
        }
        return remaining;
    }

    /**
     * The paths of the values that rulePath designates in nbt, with list indices, one per element of a long array.
     */
    private static List<List<String>> resolve(CompoundTag nbt, String rulePath) {
        List<List<String>> paths = List.of(List.of());
        for (String segment : rulePath.split("\\.")) {
            boolean everyElement = segment.endsWith("[]");
            String key = everyElement ? segment.substring(0, segment.length() - 2) : segment;
            List<List<String>> next = new ArrayList<>();
            for (List<String> path : paths) {
                if (!(get(nbt, path) instanceof CompoundTag compound)) continue;
                List<String> keys = key.endsWith("*")
                        ? compound.getAllKeys().stream().filter(k -> k.startsWith(key.substring(0, key.length() - 1))).sorted().toList()
                        : compound.contains(key) ? List.of(key) : List.of();
                for (String match : keys) {
                    List<String> child = append(path, match);
                    if (!everyElement) {
                        next.add(child);
                    } else if (compound.get(match) instanceof CollectionTag<?> list) {
                        for (int i = 0; i < list.size(); i++) next.add(append(child, String.valueOf(i)));
                    }
                }
            }
            paths = next;
        }
        List<List<String>> values = new ArrayList<>();
        for (List<String> path : paths) {
            if (get(nbt, path) instanceof LongArrayTag longs) {
                for (int i = 0; i < longs.size(); i++) values.add(append(path, String.valueOf(i)));
            } else {
                values.add(path);
            }
        }
        return values;
    }

    private static List<String> append(List<String> path, String segment) {
        List<String> appended = new ArrayList<>(path);
        appended.add(segment);
        return appended;
    }

    @Nullable
    private static Tag get(Tag root, List<String> path) {
        Tag tag = root;
        for (String segment : path) {
            if (tag instanceof CompoundTag compound) {
                tag = compound.get(segment);
            } else if (tag instanceof CollectionTag<?> list) {
                int index = Integer.parseInt(segment);
                tag = index < list.size() ? list.get(index) : null;
            } else {
                return null;
            }
        }
        return tag;
    }

    private static void set(CompoundTag root, List<String> path, Tag value) {
        Tag parent = get(root, path.subList(0, path.size() - 1));
        String key = path.getLast();
        if (parent instanceof CompoundTag compound) {
            compound.put(key, value);
        } else if (parent instanceof CollectionTag<?> list) {
            list.setTag(Integer.parseInt(key), value);
        }
    }

    @Nullable
    private static BlockPos read(Tag value) {
        if (value instanceof IntArrayTag array && array.size() == 3) {
            int[] xyz = array.getAsIntArray();
            return new BlockPos(xyz[0], xyz[1], xyz[2]);
        }
        if (value instanceof LongTag packed) return BlockPos.of(packed.getAsLong());
        if (value instanceof CompoundTag compound) {
            if (isGlobalPos(compound)) return read(compound.get("pos"));
            List<String> keys = xyzKeys(compound);
            if (keys != null) return new BlockPos(compound.getInt(keys.get(0)), compound.getInt(keys.get(1)), compound.getInt(keys.get(2)));
        }
        return null;
    }

    /**
     * The value with pos in the format of original, and dimension when it is a GlobalPos and dimension is given.
     */
    private static Tag write(Tag original, BlockPos pos, @Nullable String dimension) {
        if (original instanceof IntArrayTag) return NbtUtils.writeBlockPos(pos);
        if (original instanceof LongTag) return LongTag.valueOf(pos.asLong());
        CompoundTag compound = ((CompoundTag) original).copy();
        if (isGlobalPos(compound)) {
            compound.put("pos", NbtUtils.writeBlockPos(pos));
            if (dimension != null) compound.putString("dimension", dimension);
            return compound;
        }
        List<String> keys = xyzKeys(compound);
        compound.putInt(keys.get(0), pos.getX());
        compound.putInt(keys.get(1), pos.getY());
        compound.putInt(keys.get(2), pos.getZ());
        return compound;
    }

    private static boolean isGlobalPos(CompoundTag compound) {
        return compound.contains("dimension", Tag.TAG_STRING) && compound.get("pos") instanceof IntArrayTag pos && pos.size() == 3;
    }

    @Nullable
    private static List<String> xyzKeys(CompoundTag compound) {
        for (List<String> keys : XYZ_KEYS) {
            if (keys.stream().allMatch(key -> compound.contains(key, Tag.TAG_INT))) return keys;
        }
        return null;
    }

    /**
     * Reads the rules of position_rules.json: [{"id": "<block or entity id, or modid:>", "positions": ["<path>", ...]}].
     */
    public static Map<String, List<String>> parseRules(JsonArray json) {
        Map<String, List<String>> rules = new HashMap<>();
        for (JsonElement element : json) {
            try {
                JsonObject rule = element.getAsJsonObject();
                List<String> paths = new ArrayList<>();
                for (JsonElement path : rule.getAsJsonArray("positions")) paths.add(path.getAsString());
                rules.put(rule.get("id").getAsString(), List.copyOf(paths));
            } catch (RuntimeException e) {
                LOGGER.error("Ignored the position rule {}: {}", element, e.toString());
            }
        }
        return rules;
    }
}
