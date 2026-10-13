package capsule.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The regions where reusable capsules are deployed, by template name, saved with the overworld data. A deployment
 * leaves the registry when its capsule undeploys it or is emptied by crafting. Deployments made before this registry
 * existed are unknown to it.
 */
public class Deployments extends SavedData {

    public record Deployment(String structureName, ResourceKey<Level> dimension, BlockPos start, int size) {
        public BoundingBox box() {
            return BoundingBox.fromCorners(start, start.offset(size - 1, size - 1, size - 1));
        }
    }

    private final Map<String, Deployment> deployed = new HashMap<>();

    public static Deployments get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Deployments::new, Deployments::load, null), "capsuleDeployments");
    }

    public void add(Deployment deployment) {
        deployed.put(deployment.structureName(), deployment);
        setDirty();
    }

    public void remove(String structureName) {
        if (deployed.remove(structureName) != null) setDirty();
    }

    public List<Deployment> intersecting(ResourceKey<Level> dimension, BoundingBox box) {
        return deployed.values().stream()
                .filter(d -> d.dimension().equals(dimension) && d.box().intersects(box))
                .toList();
    }

    public static Deployments load(CompoundTag nbt, HolderLookup.Provider registries) {
        Deployments deployments = new Deployments();
        for (Tag entry : nbt.getList("deployed", Tag.TAG_COMPOUND)) {
            CompoundTag tag = (CompoundTag) entry;
            Deployment deployment = new Deployment(
                    tag.getString("structureName"),
                    ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(tag.getString("dimension"))),
                    BlockPos.of(tag.getLong("start")),
                    tag.getInt("size"));
            deployments.deployed.put(deployment.structureName(), deployment);
        }
        return deployments;
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        ListTag deployedList = new ListTag();
        for (Deployment deployment : deployed.values()) {
            CompoundTag tag = new CompoundTag();
            tag.putString("structureName", deployment.structureName());
            tag.putString("dimension", deployment.dimension().location().toString());
            tag.putLong("start", deployment.start().asLong());
            tag.putInt("size", deployment.size());
            deployedList.add(tag);
        }
        nbt.put("deployed", deployedList);
        return nbt;
    }
}
