package capsule.items;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The capsule item entities of each server level, updated by the loaders' entity load and unload events, so that tick
 * handlers never scan every entity. A level is forgotten when it unloads: its item entities get no unload event then.
 */
public class ThrownCapsules {
    private static final Map<Level, Set<ItemEntity>> BY_LEVEL = new HashMap<>();

    public static void onEntityLoad(Entity entity, Level level) {
        if (level instanceof ServerLevel && entity instanceof ItemEntity item && item.getItem().getItem() instanceof CapsuleItem) {
            BY_LEVEL.computeIfAbsent(level, l -> new LinkedHashSet<>()).add(item);
        }
    }

    public static void onEntityUnload(Entity entity, Level level) {
        Set<ItemEntity> capsules = BY_LEVEL.get(level);
        if (capsules != null) capsules.remove(entity);
    }

    public static void onLevelUnload(LevelAccessor level) {
        BY_LEVEL.remove(level);
    }

    /**
     * A copy, so that callers may add or remove entities while iterating.
     */
    public static List<ItemEntity> in(ServerLevel level) {
        Set<ItemEntity> capsules = BY_LEVEL.get(level);
        if (capsules == null || capsules.isEmpty()) return List.of();
        capsules.removeIf(Entity::isRemoved);
        return List.copyOf(capsules);
    }
}
