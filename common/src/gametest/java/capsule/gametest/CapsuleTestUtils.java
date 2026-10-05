package capsule.gametest;

import capsule.StructureSaver;
import capsule.helpers.Capsule;
import capsule.items.CapsuleItem;
import capsule.structure.CapsuleTemplate;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Loader independent helpers shared by the capsule GameTests.
 */
public class CapsuleTestUtils {

    public static ItemStack emptyCapsule(int size) {
        return Capsule.newEmptyCapsuleItemStack(0xCCCCCC, 0xCCCCCC, size, false, null, 0);
    }

    /**
     * Captures the cube of the given size whose lowest corner is relativeCorner into a new linked capsule.
     */
    public static ItemStack capture(GameTestHelper helper, BlockPos relativeCorner, int size) {
        ItemStack capsule = emptyCapsule(size);
        if (!Capsule.captureAtPosition(capsule, null, size, helper.getLevel(), helper.absolutePos(relativeCorner))) {
            helper.fail("capture failed", relativeCorner);
        }
        return capsule;
    }

    /**
     * Deploys a linked capsule so that its content is centered on relativeAnchor and starts one block above it.
     */
    public static boolean deploy(GameTestHelper helper, ItemStack capsule, BlockPos relativeAnchor, ServerPlayer player) {
        int extendLength = (CapsuleItem.getSize(capsule) - 1) / 2;
        return Capsule.deployCapsule(capsule, helper.absolutePos(relativeAnchor), player == null ? null : player.getUUID(), extendLength, helper.getLevel());
    }

    public static CapsuleTemplate template(GameTestHelper helper, ItemStack capsule) {
        return StructureSaver.getTemplate(capsule, helper.getLevel()).getRight();
    }

    public static void fill(GameTestHelper helper, BlockPos from, BlockPos to, BlockState state) {
        BlockPos.betweenClosed(from, to).forEach(p -> helper.setBlock(p, state));
    }

    /**
     * Empties a relative box without dropping container contents.
     */
    public static void clear(GameTestHelper helper, BlockPos from, BlockPos to) {
        helper.killAllEntities();
        BlockPos.betweenClosed(helper.absolutePos(from), helper.absolutePos(to)).forEach(p -> {
            Clearable.tryClear(helper.getLevel().getBlockEntity(p));
            helper.getLevel().setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
        });
    }

    /**
     * A survival player standing at relativePos. Unlike GameTestHelper.makeMockServerPlayerInLevel it is not added to the
     * player list, so payloads broadcast by the mod never reach its connection, which did not negotiate the mod channels.
     */
    public static ServerPlayer survivalPlayer(GameTestHelper helper, BlockPos relativePos) {
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "test-player"), false);
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        new ServerGamePacketListenerImpl(helper.getLevel().getServer(), connection, player, cookie);
        Vec3 pos = helper.absoluteVec(Vec3.atBottomCenterOf(relativePos));
        player.moveTo(pos.x, pos.y, pos.z, 0, 0);
        helper.getLevel().addNewPlayer(player);
        return player;
    }

    public static void removePlayer(ServerPlayer player) {
        player.serverLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
    }

    public static void assertTrue(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }
}
