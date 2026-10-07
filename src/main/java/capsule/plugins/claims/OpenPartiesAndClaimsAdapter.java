package capsule.plugins.claims;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.claims.api.IServerClaimsManagerAPI;
import xaero.pac.common.server.claims.protection.api.IChunkProtectionAPI;

import java.util.List;

/**
 * Open Parties and Claims protects chunk columns. Its server API answers by player id: the chunk owner, their party
 * and allies have access.
 */
class OpenPartiesAndClaimsAdapter implements ClaimAdapter {

    @Override
    public String name() {
        return "Open Parties and Claims";
    }

    @Override
    public List<Claim> claims(ServerLevel level, BoundingBox box, ServerPlayer player) {
        OpenPACServerAPI api = OpenPACServerAPI.get(level.getServer());
        IServerClaimsManagerAPI claims = api.getServerClaimsManager();
        IChunkProtectionAPI protection = api.getChunkProtection();
        ResourceLocation dimension = level.dimension().location();
        return ClaimAdapter.perChunk(level, box, (chunkX, chunkZ) -> claims.get(dimension, chunkX, chunkZ) == null ? null
                : protection.hasChunkAccess(player.getUUID(), dimension, chunkX, chunkZ));
    }
}
