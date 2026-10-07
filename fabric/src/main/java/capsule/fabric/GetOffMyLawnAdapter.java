package capsule.fabric;

import capsule.plugins.claims.ClaimAdapter;
import draylar.goml.api.ClaimUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Get Off My Lawn protects boxes around claim anchors, kept in an R-tree queried by box. Owners and trusted players,
 * and admins in admin mode, may change them.
 */
class GetOffMyLawnAdapter implements ClaimAdapter {

    @Override
    public String name() {
        return "Get Off My Lawn";
    }

    @Override
    public List<Claim> claims(ServerLevel level, BoundingBox box, ServerPlayer player) {
        boolean admin = ClaimUtils.isInAdminMode(player);
        List<Claim> claims = new ArrayList<>();
        ClaimUtils.getClaimsInBox(level, new BlockPos(box.minX(), box.minY(), box.minZ()), new BlockPos(box.maxX(), box.maxY(), box.maxZ())).forEach(entry -> {
            AABB claimed = entry.getKey().minecraftBox();
            BoundingBox blocks = new BoundingBox(Mth.floor(claimed.minX), Mth.floor(claimed.minY), Mth.floor(claimed.minZ),
                    Mth.ceil(claimed.maxX) - 1, Mth.ceil(claimed.maxY) - 1, Mth.ceil(claimed.maxZ) - 1);
            claims.add(new Claim(blocks, admin || entry.getValue().hasPermission(player.getUUID())));
        });
        return claims;
    }
}
