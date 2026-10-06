package capsule.plugins.claims;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Get Off My Lawn (Fabric) protects boxes around claim anchors, kept in an R-tree queried by box. Owners and trusted
 * players, and admins in admin mode, may change them.
 */
class GetOffMyLawnAdapter implements ClaimAdapter {
    private final Method claimsInBox;
    private final Method adminMode;
    private final Method forEach;
    private final Method key;
    private final Method value;
    private final Method aabb;
    private final Method hasPermission;

    GetOffMyLawnAdapter() throws ReflectiveOperationException {
        Class<?> utils = Class.forName("draylar.goml.api.ClaimUtils");
        claimsInBox = utils.getMethod("getClaimsInBox", LevelReader.class, BlockPos.class, BlockPos.class);
        adminMode = utils.getMethod("isInAdminMode", Player.class);
        forEach = Class.forName("com.jamieswhiteshirt.rtree3i.Selection").getMethod("forEach", Consumer.class);
        Class<?> entry = Class.forName("com.jamieswhiteshirt.rtree3i.Entry");
        key = entry.getMethod("getKey");
        value = entry.getMethod("getValue");
        aabb = Class.forName("draylar.goml.api.ClaimBox").getMethod("minecraftBox");
        hasPermission = Class.forName("draylar.goml.api.Claim").getMethod("hasPermission", UUID.class);
    }

    @Override
    public String name() {
        return "Get Off My Lawn";
    }

    @Override
    public List<Claim> claims(ServerLevel level, BoundingBox box, ServerPlayer player) throws ReflectiveOperationException {
        List<Object> entries = new ArrayList<>();
        Object selection = claimsInBox.invoke(null, level, new BlockPos(box.minX(), box.minY(), box.minZ()), new BlockPos(box.maxX(), box.maxY(), box.maxZ()));
        forEach.invoke(selection, (Consumer<Object>) entries::add);
        boolean admin = !entries.isEmpty() && (boolean) adminMode.invoke(null, player);
        List<Claim> claims = new ArrayList<>();
        for (Object entry : entries) {
            AABB claimed = (AABB) aabb.invoke(key.invoke(entry));
            BoundingBox blocks = new BoundingBox(Mth.floor(claimed.minX), Mth.floor(claimed.minY), Mth.floor(claimed.minZ),
                    Mth.ceil(claimed.maxX) - 1, Mth.ceil(claimed.maxY) - 1, Mth.ceil(claimed.maxZ) - 1);
            claims.add(new Claim(blocks, admin || (boolean) hasPermission.invoke(value.invoke(entry), player.getUUID())));
        }
        return claims;
    }
}
