package capsule.incompat;

import capsule.helpers.Capsule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * The area of one scenario, a forced chunk high above the spawn: positions are relative to its lowest corner.
 */
class Scenario {
    final String name;
    final ServerLevel level;
    private final BlockPos origin;

    Scenario(String name, ServerLevel level, BlockPos origin) {
        this.name = name;
        this.level = level;
        this.origin = origin;
        level.setChunkForced(origin.getX() >> 4, origin.getZ() >> 4, true);
    }

    BlockPos abs(BlockPos pos) {
        return origin.offset(pos);
    }

    void set(BlockPos pos, Block block) {
        set(pos, block.defaultBlockState());
    }

    void set(BlockPos pos, BlockState state) {
        level.setBlockAndUpdate(abs(pos), state);
    }

    BlockState get(BlockPos pos) {
        return level.getBlockState(abs(pos));
    }

    BlockEntity blockEntity(BlockPos pos) {
        return level.getBlockEntity(abs(pos));
    }

    void floor(int size) {
        BlockPos.betweenClosed(BlockPos.ZERO, new BlockPos(15, 0, size - 1)).forEach(pos -> set(pos, Blocks.STONE));
    }

    void check(String what, boolean passed, Object observed) {
        KnownIncompatibilities.check(name + ": " + what, passed, observed);
    }

    void later(int ticks, Runnable task) {
        KnownIncompatibilities.later(this, ticks, task);
    }

    /**
     * A new linked capsule holding the cube of the given size at corner, or null if the capture failed.
     */
    ItemStack capture(BlockPos corner, int size, boolean overpowered) {
        ItemStack capsule = Capsule.newEmptyCapsuleItemStack(0, 0, size, overpowered, null, 0);
        return Capsule.captureAtPosition(capsule, null, size, level, abs(corner)) ? capsule : null;
    }

    /**
     * Deploys the content so that its lowest corner is at corner.
     */
    boolean deploy(ItemStack capsule, BlockPos corner, int size) {
        int extend = (size - 1) / 2;
        return Capsule.deployCapsule(capsule, abs(corner.offset(extend, -1, extend)), null, extend, level);
    }

    /**
     * Captures the cube with a standard, then an overpowered capsule: the blocks at kept stay in place with their block
     * entities, while a stone in the opposite corner is captured each time.
     */
    void neverCaptured(BlockPos corner, int size, BlockPos... kept) {
        BlockPos stone = corner.offset(size - 1, size - 1, size - 1);
        Map<BlockPos, BlockState> states = Arrays.stream(kept).collect(Collectors.toMap(Function.identity(), this::get));
        for (boolean overpowered : new boolean[]{false, true}) {
            String capsule = overpowered ? "an overpowered capsule" : "a capsule";
            set(stone, Blocks.STONE);
            boolean captured = capture(corner, size, overpowered) != null && get(stone).isAir();
            String moved = states.entrySet().stream()
                    .filter(e -> get(e.getKey()) != e.getValue() || e.getValue().hasBlockEntity() && blockEntity(e.getKey()) == null)
                    .map(e -> e.getValue() + " at " + e.getKey()).collect(Collectors.joining(", "));
            check(capsule + " leaves " + states.values().stream().map(s -> ForgeRegistries.BLOCKS.getKey(s.getBlock())).distinct().toList() + " in place",
                    captured && moved.isEmpty(), captured ? "taken: [" + moved + "]" : "the capture failed");
        }
    }

    /**
     * Places the item on top of the block at floor as a player would.
     */
    void placeOn(BlockPos floor, ItemStack stack) {
        var player = FakePlayerFactory.getMinecraft(level);
        BlockPos pos = abs(floor);
        player.moveTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() - 2.5, 0, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false)));
    }

    /**
     * The item handlers of the block at pos, unsided first.
     */
    Stream<IItemHandler> items(BlockPos pos) {
        BlockEntity blockEntity = blockEntity(pos);
        if (blockEntity == null) return Stream.empty();
        return Stream.concat(Stream.of((Direction) null), Arrays.stream(Direction.values()))
                .map(side -> blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, side).orElse(null))
                .filter(Objects::nonNull);
    }

    int count(BlockPos pos, Item item) {
        return items(pos).mapToInt(handler -> IntStream.range(0, handler.getSlots())
                .map(slot -> handler.getStackInSlot(slot).is(item) ? handler.getStackInSlot(slot).getCount() : 0).sum()).max().orElse(0);
    }

    static Block block(String id) {
        return ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));
    }

    static Item item(String id) {
        return ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
    }
}
