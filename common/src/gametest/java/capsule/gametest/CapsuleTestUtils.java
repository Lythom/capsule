package capsule.gametest;

import capsule.StructureSaver;
import capsule.helpers.Capsule;
import capsule.items.CapsuleItem;
import capsule.structure.CapsuleTemplate;
import com.mojang.authlib.GameProfile;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.lang.reflect.Constructor;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
            helper.fail(Component.literal("capture failed"), relativeCorner);
        }
        return capsule;
    }

    /**
     * Deploys a linked capsule so that its content is centered on relativeAnchor and starts one block above it.
     */
    public static boolean deploy(GameTestHelper helper, ItemStack capsule, BlockPos relativeAnchor, ServerPlayer player) {
        int extendLength = (CapsuleItem.getSize(capsule) - 1) / 2;
        return Capsule.deployCapsule(capsule, helper.absolutePos(relativeAnchor), player, extendLength, helper.getLevel());
    }

    /**
     * Captures the cube of the given size at relativeCorner with a standard, then an overpowered capsule: the blocks at
     * kept stay in place with their block entities, while a stone in the opposite corner is captured each time.
     */
    public static void assertNeverCaptured(GameTestHelper helper, BlockPos relativeCorner, int size, BlockPos... kept) {
        BlockPos stone = relativeCorner.offset(size - 1, size - 1, size - 1);
        Map<BlockPos, BlockState> states = Arrays.stream(kept).collect(Collectors.toMap(Function.identity(), helper::getBlockState));
        for (boolean overpowered : new boolean[]{false, true}) {
            String capsule = overpowered ? "an overpowered capsule" : "a capsule";
            helper.setBlock(stone, Blocks.STONE);
            if (!Capsule.captureAtPosition(Capsule.newEmptyCapsuleItemStack(0, 0, size, overpowered, null, 0), null, size, helper.getLevel(), helper.absolutePos(relativeCorner))) {
                helper.fail(Component.literal(capsule + " failed to capture"), relativeCorner);
            }
            helper.assertBlockNotPresent(Blocks.STONE, stone);
            states.forEach((pos, state) -> {
                assertTrue(helper, helper.getBlockState(pos) == state, capsule + " took " + state + " at " + pos + ", left " + helper.getBlockState(pos));
                assertTrue(helper, !state.hasBlockEntity() || helper.getLevel().getBlockEntity(helper.absolutePos(pos)) != null, capsule + " took the block entity of " + state + " at " + pos);
            });
        }
    }

    public static Block block(String id) {
        return BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
    }

    public static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
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
            if (helper.getLevel().getBlockEntity(p) instanceof Clearable clearable) clearable.clearContent();
            helper.getLevel().setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
        });
    }

    /**
     * A survival player standing at relativePos. Unlike GameTestHelper.makeMockServerPlayerInLevel it is not added to the
     * player list, so payloads broadcast by the mod never reach its connection, which did not negotiate the mod channels.
     */
    public static ServerPlayer survivalPlayer(GameTestHelper helper, BlockPos relativePos) {
        return survivalPlayer(helper, relativePos, null);
    }

    /**
     * A survival player whose system messages (chat feedback) are added to messages, if not null. Without messages it
     * is a plain ServerPlayer, which some mods (Flan) tell apart from fake players by its class.
     */
    public static ServerPlayer survivalPlayer(GameTestHelper helper, BlockPos relativePos, @Nullable List<Component> messages) {
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "test-player"), false);
        ServerPlayer player = messages == null ? new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation())
                : new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation()) {
            @Override
            public void sendSystemMessage(Component message, boolean overlay) {
                messages.add(message);
            }
        };
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        // packets sent to the player are dropped, not queued
        new EmbeddedChannel(new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) {
                ReferenceCountUtil.release(message);
                promise.setSuccess();
            }
        }, connection);
        new ServerGamePacketListenerImpl(helper.getLevel().getServer(), connection, player, cookie);
        // since 26.1 a new player starts in the default game mode of the server, creative on the GameTest server
        player.setGameMode(GameType.SURVIVAL);
        Vec3 pos = helper.absoluteVec(Vec3.atBottomCenterOf(relativePos));
        player.snapTo(pos.x, pos.y, pos.z, 0, 0);
        helper.getLevel().addNewPlayer(player);
        return player;
    }

    public static void removePlayer(ServerPlayer player) {
        player.level().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
    }

    public static void assertTrue(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }

    /**
     * Runs action with the recipes of manager plus extra, then puts the recipes back. Since Minecraft 1.21.2 the
     * recipe manager has no API to replace its recipes, so its recipe map is swapped by reflection.
     */
    public static <T> T withRecipes(RecipeManager manager, List<RecipeHolder<?>> extra, Supplier<T> action) {
        try {
            Field field = Arrays.stream(RecipeManager.class.getDeclaredFields())
                    .filter(f -> f.getType() == RecipeMap.class).findFirst().orElseThrow();
            field.setAccessible(true);
            RecipeMap original = (RecipeMap) field.get(manager);
            field.set(manager, recipeMap(Stream.concat(original.values().stream(), extra.stream()).toList()));
            try {
                return action.get();
            } finally {
                field.set(manager, original);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * A recipe map of these recipes. Since 26.3 RecipeMap#create reads the recipe registry, so the map is built through
     * its private constructor (recipes by type, recipes by key), by reflection.
     */
    private static RecipeMap recipeMap(List<RecipeHolder<?>> recipes) {
        ImmutableMultimap.Builder<RecipeType<?>, RecipeHolder<?>> byType = ImmutableMultimap.builder();
        ImmutableMap.Builder<ResourceKey<Recipe<?>>, RecipeHolder<?>> byKey = ImmutableMap.builder();
        for (RecipeHolder<?> recipe : recipes) {
            byType.put(recipe.value().getType(), recipe);
            byKey.put(recipe.id(), recipe);
        }
        try {
            Constructor<RecipeMap> constructor = RecipeMap.class.getDeclaredConstructor(Multimap.class, Map.class);
            constructor.setAccessible(true);
            return constructor.newInstance(byType.build(), byKey.buildKeepingLast());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
