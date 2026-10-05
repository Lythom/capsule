package capsule.neoforge;

import capsule.CapsuleMod;
import capsule.platform.ItemSource;
import capsule.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.registries.DeferredRegister;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class NeoForgePlatform implements Platform {
    private static final Map<ResourceKey<? extends Registry<?>>, DeferredRegister<?>> REGISTERS = new LinkedHashMap<>();
    private static boolean registered = false;

    static void registerAll(IEventBus modEventBus) {
        REGISTERS.values().forEach(register -> register.register(modEventBus));
        registered = true;
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Supplier<T> register(Registry<? super T> registry, String name, Supplier<T> factory) {
        if (registered) throw new IllegalStateException("capsule:" + name + " registered too late");
        DeferredRegister<Object> register = (DeferredRegister<Object>) REGISTERS.computeIfAbsent(registry.key(),
                key -> DeferredRegister.create((ResourceKey<? extends Registry<Object>>) key, CapsuleMod.MODID));
        return (Supplier<T>) register.register(name, factory);
    }

    @Override
    public CreativeModeTab.Builder creativeTabBuilder() {
        return CreativeModeTab.builder().withTabsBefore(CreativeModeTabs.SPAWN_EGGS);
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return CommonHooks.getCraftingRemainingItem(stack);
    }

    @Override
    public boolean canPlaceBlock(ServerLevel level, BlockPos pos, @Nullable Player player) {
        BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        return !NeoForge.EVENT_BUS.post(new BlockEvent.EntityPlaceEvent(snapshot, Blocks.DIRT.defaultBlockState(), player)).isCanceled();
    }

    @Override
    @Nullable
    public ItemSource getItemSource(Level level, BlockPos pos) {
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        return handler == null ? null : new ItemHandlerSource(handler);
    }

    @Override
    public BlockState rotate(BlockState state, LevelAccessor level, BlockPos pos, Rotation rotation) {
        return state.rotate(level, pos, rotation);
    }

    @Override
    public CompoundTag getPersistentData(ServerPlayer player) {
        return player.getPersistentData();
    }

    private record ItemHandlerSource(IItemHandler handler) implements ItemSource {
        @Override
        public int size() {
            return handler.getSlots();
        }

        @Override
        public ItemStack getItem(int slot) {
            return handler.getStackInSlot(slot);
        }

        @Override
        public ItemStack extract(int slot, int count) {
            return handler.extractItem(slot, count, false);
        }

        @Override
        public void insert(int slot, ItemStack stack) {
            handler.insertItem(slot, stack, false);
        }
    }
}
