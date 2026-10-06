package capsule.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import xaero.pac.common.parties.party.member.PartyMemberRank;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

import java.util.stream.Stream;

/**
 * Open Parties and Claims (-PmodCompat): chunk claims, party members may build.
 */
public class OpenPartiesAndClaimsTests {

    @GameTest(template = "empty17", batch = "openpartiesandclaims", timeoutTicks = 200)
    public static void openPartiesAndClaimsVetoesStrangers(GameTestHelper helper) {
        OpenPACServerAPI api = OpenPACServerAPI.get(helper.getLevel().getServer());
        ServerPlayer owner = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(0, 1, 0));
        ServerPlayer member = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(0, 1, 16));
        ServerPlayer stranger = CapsuleTestUtils.survivalPlayer(helper, new BlockPos(16, 1, 0));
        IServerPartyAPI party = api.getPartyManager().createPartyForOwner(owner);
        party.addMember(member.getUUID(), PartyMemberRank.MEMBER, member.getGameProfile().getName());

        // the claimed chunk holds the center and the 7 blocks next to it in one direction of each axis (dx, dz)
        BlockPos center = new BlockPos(8, 1, 8);
        ChunkPos claimed = new ChunkPos(helper.absolutePos(center));
        int dx = new ChunkPos(helper.absolutePos(center.west(7))).equals(claimed) ? -1 : 1;
        int dz = new ChunkPos(helper.absolutePos(center.north(7))).equals(claimed) ? -1 : 1;
        BlockPos outside = Stream.of(new BlockPos(0, 1, 8), new BlockPos(16, 1, 8))
                .filter(pos -> !new ChunkPos(helper.absolutePos(pos)).equals(claimed))
                .findFirst().orElseThrow();
        ResourceLocation dimension = helper.getLevel().dimension().location();
        api.getServerClaimsManager().claim(dimension, owner.getUUID(), -1, claimed.x, claimed.z, false);

        new ClaimModScenario(owner, member, stranger, center, center.offset(2 * dx, 0, 0), center.offset(4 * dx, 0, 0), outside,
                center.offset(0, 0, 3 * dz), center.offset(4 * dx, 0, 3 * dz), center.offset(0, 0, 6 * dz), center.offset(4 * dx, 0, 6 * dz),
                outside.south(3))
                .run(helper, () -> {
                    api.getServerClaimsManager().unclaim(dimension, claimed.x, claimed.z);
                    api.getPartyManager().removeParty(party);
                    Stream.of(owner, member, stranger).forEach(CapsuleTestUtils::removePlayer);
                });
    }
}
