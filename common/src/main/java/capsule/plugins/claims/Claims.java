package capsule.plugins.claims;

import capsule.Config;
import capsule.items.CapsuleItem;
import capsule.items.CapsuleItems;
import capsule.platform.Services;
import capsule.plugins.claims.ClaimAdapter.Claim;
import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Whether claim mods let a player capture or deploy blocks. Mods with an adapter are asked through their API, once per
 * chunk or claim (Flan per position, like the probe); the positions they do not cover are probed through the loader's
 * protection hook (a block placement event on NeoForge, Common Protection API on Fabric), so that claim mods without
 * adapter still protect them: each position up to the largest survival capsule, once per chunk column above. When a
 * loaded protection mod cannot be checked, captures and deploys are refused.
 */
public final class Claims {
    private static final Logger LOGGER = LogManager.getLogger(Claims.class);
    private static final List<ClaimAdapter> ADAPTERS = new CopyOnWriteArrayList<>();
    /**
     * The adapters whose failure is logged already: once each.
     */
    private static final Set<ClaimAdapter> REPORTED = ConcurrentHashMap.newKeySet();
    /**
     * Asks the claims for captures and deploys without a player (dispensers, capture bases placed before Capsule 9.1).
     */
    private static final GameProfile NOBODY = new GameProfile(UUID.fromString("9c0b9b7b-b356-41c0-93b2-4bb6afe1586c"), "[Capsule]");
    private static boolean modsLoaded = false;
    /**
     * The recipes largestTier was read from: a reload replaces them.
     */
    @Nullable
    private static RecipeManager tiersRecipes = null;
    private static int largestTier = 1;

    private Claims() {
    }

    /**
     * Adds the claims of a protection mod Capsule has no adapter for.
     */
    public static void register(ClaimAdapter adapter) {
        ADAPTERS.add(adapter);
    }

    public static void unregister(ClaimAdapter adapter) {
        ADAPTERS.remove(adapter);
    }

    public interface Factory {
        ClaimAdapter create();
    }

    /**
     * Loads the adapters of the protection mods present, once, when the server starts. Each adapter calls the API of
     * its mod: lambdas, not constructor references, load an adapter class only when its mod is loaded.
     */
    public static void loadAdapters() {
        if (modsLoaded) return;
        modsLoaded = true;
        load("openpartiesandclaims", () -> new OpenPartiesAndClaimsAdapter());
        load("flan", () -> new FlanAdapter());
    }

    /**
     * Registers the adapter of a protection mod when the mod is loaded, or a marker refusing every capture and deploy
     * when its API is not found.
     *
     * @return the adapter or marker registered, null when the mod is not loaded
     */
    @Nullable
    public static ClaimAdapter load(String modId, Factory factory) {
        if (!Services.PLATFORM.isModLoaded(modId)) return null;
        ClaimAdapter adapter;
        try {
            adapter = factory.create();
        } catch (RuntimeException | LinkageError e) {
            adapter = new Unusable(Services.PLATFORM.modDescription(modId));
            REPORTED.add(adapter);
            LOGGER.error("Captures and deploys are refused: Capsule cannot check the claims of {}, its API was not found ({}). Please report this incompatibility.",
                    adapter.name(), e.toString());
        }
        ADAPTERS.add(adapter);
        return adapter;
    }

    /**
     * A protection mod whose API was not found.
     */
    private record Unusable(String name) implements ClaimAdapter {
        @Override
        public List<Claim> claims(ServerLevel level, BoundingBox box, ServerPlayer player) {
            throw new IllegalStateException("API not found");
        }
    }

    /**
     * The player a capture or deploy is checked as: the connected player with this id, or a fake player with their
     * profile when they are offline.
     */
    @Nullable
    public static ServerPlayer player(ServerLevel level, @Nullable UUID id) {
        if (id == null) return null;
        if (level.getPlayerByUUID(id) instanceof ServerPlayer player) return player;
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
        return player != null ? player : fakePlayer(level, id);
    }

    /**
     * A fake player with the profile of id, which gets no chat feedback.
     */
    @Nullable
    public static ServerPlayer fakePlayer(ServerLevel level, @Nullable UUID id) {
        if (id == null) return null;
        GameProfileCache profiles = level.getServer().getProfileCache();
        GameProfile profile = profiles == null ? null : profiles.get(id).orElse(null);
        return Services.PLATFORM.fakePlayer(level, profile != null ? profile : new GameProfile(id, "[Capsule]"));
    }

    /**
     * The positions the player may not change among positions. Without a player, no claimed position may be changed.
     *
     * @return null when a protection mod cannot be checked: the operation is refused, the player is told
     */
    @Nullable
    public static Predicate<BlockPos> denied(ServerLevel level, Collection<BlockPos> positions, @Nullable ServerPlayer player) {
        Optional<BoundingBox> box = BoundingBox.encapsulatingPositions(positions);
        return box.isPresent() ? denied(level, box.get(), player) : pos -> false;
    }

    /**
     * The positions of box the player may not change. The adapters are asked here; testing a position outside their
     * claims probes it when box is at most perBlockMaxSize wide, else looks up its chunk column, probed here.
     *
     * @return null when a protection mod cannot be checked: the operation is refused, the player is told
     */
    @Nullable
    public static Predicate<BlockPos> denied(ServerLevel level, BoundingBox box, @Nullable ServerPlayer player) {
        ServerPlayer actor = player != null ? player : Services.PLATFORM.fakePlayer(level, NOBODY);
        // per chunk, the claims of each adapter
        Long2ObjectMap<List<List<Claim>>> claimsByChunk = new Long2ObjectOpenHashMap<>();
        for (ClaimAdapter adapter : ADAPTERS) {
            List<Claim> claims;
            try {
                claims = adapter.claims(level, box, actor);
            } catch (RuntimeException | LinkageError e) {
                if (REPORTED.add(adapter)) {
                    LOGGER.error("Captures and deploys are refused while Capsule cannot check the claims of {}. Please report this incompatibility.", adapter.name(), e);
                }
                if (player != null) player.sendSystemMessage(Component.translatable("capsule.error.claimCheckFailed", adapter.name()));
                return null;
            }
            Long2ObjectMap<List<Claim>> adapterClaims = new Long2ObjectOpenHashMap<>();
            for (Claim claim : claims) {
                BoundingBox inBox = intersection(claim.box(), box);
                if (inBox == null) continue;
                for (int chunkX = inBox.minX() >> 4; chunkX <= inBox.maxX() >> 4; chunkX++) {
                    for (int chunkZ = inBox.minZ() >> 4; chunkZ <= inBox.maxZ() >> 4; chunkZ++) {
                        adapterClaims.computeIfAbsent(ChunkPos.asLong(chunkX, chunkZ), k -> new ArrayList<>()).add(claim);
                    }
                }
            }
            adapterClaims.long2ObjectEntrySet().forEach(e -> claimsByChunk.computeIfAbsent(e.getLongKey(), k -> new ArrayList<>()).add(e.getValue()));
        }

        boolean perBlock = perBlock(level, box);
        LongSet deniedColumns = perBlock ? LongSets.EMPTY_SET : deniedColumns(level, box, claimsByChunk, actor);

        return pos -> {
            long chunk = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
            List<List<Claim>> chunkClaims = claimsByChunk.get(chunk);
            boolean claimed = false;
            if (chunkClaims != null) {
                for (List<Claim> claims : chunkClaims) {
                    Claim claim = lastContaining(claims, pos);
                    if (claim == null) continue;
                    if (!claim.allowed() || player == null) return true;
                    claimed = true;
                }
            }
            if (claimed) return false;
            return perBlock ? !Services.PLATFORM.canPlaceBlock(level, pos, actor) : deniedColumns.contains(chunk);
        };
    }

    /**
     * The chunk columns of box denied by mods without adapter, probed once each outside the adapter claims.
     */
    private static LongSet deniedColumns(ServerLevel level, BoundingBox box, Long2ObjectMap<List<List<Claim>>> claimsByChunk, ServerPlayer actor) {
        LongSet denied = new LongOpenHashSet();
        for (int chunkX = box.minX() >> 4; chunkX <= box.maxX() >> 4; chunkX++) {
            for (int chunkZ = box.minZ() >> 4; chunkZ <= box.maxZ() >> 4; chunkZ++) {
                long chunk = ChunkPos.asLong(chunkX, chunkZ);
                BoundingBox column = intersection(box, new BoundingBox(chunkX << 4, box.minY(), chunkZ << 4, (chunkX << 4) + 15, box.maxY(), (chunkZ << 4) + 15));
                BlockPos probe = unclaimedPosition(column, claimsByChunk.getOrDefault(chunk, List.of()));
                if (probe != null && !Services.PLATFORM.canPlaceBlock(level, probe, actor)) denied.add(chunk);
            }
        }
        return denied;
    }

    @Nullable
    private static Claim lastContaining(List<Claim> claims, BlockPos pos) {
        for (int i = claims.size() - 1; i >= 0; i--) {
            if (claims.get(i).box().isInside(pos)) return claims.get(i);
        }
        return null;
    }

    /**
     * The center or a corner of column outside every claim, where the claims of mods without adapter are probed.
     */
    @Nullable
    private static BlockPos unclaimedPosition(BoundingBox column, List<List<Claim>> claims) {
        List<BlockPos> candidates = new ArrayList<>();
        candidates.add(column.getCenter());
        for (int x : new int[]{column.minX(), column.maxX()}) {
            for (int y : new int[]{column.minY(), column.maxY()}) {
                for (int z : new int[]{column.minZ(), column.maxZ()}) {
                    candidates.add(new BlockPos(x, y, z));
                }
            }
        }
        return candidates.stream()
                .filter(pos -> claims.stream().allMatch(adapterClaims -> lastContaining(adapterClaims, pos) == null))
                .findFirst()
                .orElse(null);
    }

    /**
     * Largest size probed per block: the largest capsule of survival, the largest crafted tier with every upgrade.
     * Above, OP captures and deploys would take seconds, so they are probed per chunk column.
     */
    public static int perBlockMaxSize(MinecraftServer server) {
        RecipeManager recipes = server.getRecipeManager();
        if (recipes != tiersRecipes) {
            // copies: getSize resizes invalid sizes, the recipe results are shared
            largestTier = recipes.getAllRecipesFor(RecipeType.CRAFTING).stream()
                    .map(RecipeHolder::value)
                    .filter(recipe -> recipe instanceof ShapedRecipe && CapsuleItems.hasNoEmptyTagsIngredient(recipe))
                    .map(recipe -> recipe.getResultItem(server.registryAccess()).copy())
                    .filter(capsule -> capsule.getItem() instanceof CapsuleItem && !CapsuleItem.isOverpowered(capsule))
                    .mapToInt(CapsuleItem::getSize)
                    .max().orElse(1);
            tiersRecipes = recipes;
        }
        return largestTier + Config.upgradeLimit * CapsuleItems.UPGRADE_STEP;
    }

    /**
     * Whether the positions of box are probed one by one, else once per chunk column.
     */
    static boolean perBlock(ServerLevel level, BoundingBox box) {
        return Math.max(box.getXSpan(), Math.max(box.getYSpan(), box.getZSpan())) <= perBlockMaxSize(level.getServer());
    }

    @Nullable
    static BoundingBox intersection(BoundingBox a, BoundingBox b) {
        if (!a.intersects(b)) return null;
        return new BoundingBox(Math.max(a.minX(), b.minX()), Math.max(a.minY(), b.minY()), Math.max(a.minZ(), b.minZ()),
                Math.min(a.maxX(), b.maxX()), Math.min(a.maxY(), b.maxY()), Math.min(a.maxZ(), b.maxZ()));
    }
}
