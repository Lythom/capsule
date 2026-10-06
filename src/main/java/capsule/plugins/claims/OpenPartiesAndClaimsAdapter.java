package capsule.plugins.claims;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

/**
 * Open Parties and Claims protects chunk columns. Its server API (OpenPACServerAPI) answers by player id: the chunk
 * owner, their party and allies have access.
 */
class OpenPartiesAndClaimsAdapter implements ClaimAdapter {
    private final Method api;
    private final Method claimsManager;
    private final Method chunkProtection;
    private final Method claimAt;
    private final Method hasChunkAccess;

    OpenPartiesAndClaimsAdapter() throws ReflectiveOperationException {
        Class<?> apiType = Class.forName("xaero.pac.common.server.api.OpenPACServerAPI");
        api = apiType.getMethod("get", MinecraftServer.class);
        claimsManager = apiType.getMethod("getServerClaimsManager");
        chunkProtection = apiType.getMethod("getChunkProtection");
        claimAt = Class.forName("xaero.pac.common.server.claims.api.IServerClaimsManagerAPI")
                .getMethod("get", ResourceLocation.class, int.class, int.class);
        hasChunkAccess = Class.forName("xaero.pac.common.server.claims.protection.api.IChunkProtectionAPI")
                .getMethod("hasChunkAccess", UUID.class, ResourceLocation.class, int.class, int.class);
    }

    @Override
    public String name() {
        return "Open Parties and Claims";
    }

    @Override
    public List<Claim> claims(ServerLevel level, BoundingBox box, ServerPlayer player) throws ReflectiveOperationException {
        Object server = api.invoke(null, level.getServer());
        Object claims = claimsManager.invoke(server);
        Object protection = chunkProtection.invoke(server);
        ResourceLocation dimension = level.dimension().location();
        return ClaimAdapter.perChunk(level, box, (chunkX, chunkZ) -> claimAt.invoke(claims, dimension, chunkX, chunkZ) == null ? null
                : (Boolean) hasChunkAccess.invoke(protection, player.getUUID(), dimension, chunkX, chunkZ));
    }
}
