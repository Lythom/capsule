package capsule.neoforge;

import capsule.CapsuleMod;
import capsule.platform.ItemSource;
import capsule.platform.Platform;
import com.mojang.authlib.GameProfile;
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
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.crafting.CompoundIngredient;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Arrays;
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
    public String modDescription(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(mod -> mod.getModInfo().getDisplayName() + " " + mod.getModInfo().getVersion())
                .orElse(modId);
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
        ItemStackTemplate remainder = stack.getCraftingRemainder();
        return remainder == null ? ItemStack.EMPTY : remainder.create();
    }

    @Override
    public Ingredient componentsIngredient(ItemStack... stacks) {
        return CompoundIngredient.of(Arrays.stream(stacks).map(stack -> DataComponentIngredient.of(false, stack)).toArray(Ingredient[]::new));
    }

    @Override
    public boolean canPlaceBlock(ServerLevel level, BlockPos pos, Player player) {
        BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        return !NeoForge.EVENT_BUS.post(new BlockEvent.EntityPlaceEvent(snapshot, Blocks.DIRT.defaultBlockState(), player)).isCanceled();
    }

    @Override
    public ServerPlayer fakePlayer(ServerLevel level, GameProfile profile) {
        return FakePlayerFactory.get(level, profile);
    }

    @Override
    @Nullable
    public ItemSource getItemSource(Level level, BlockPos pos) {
        ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, pos, null);
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

    /**
     * The slots of a block's item resource handler (NeoForge's transfer API, which replaced IItemHandler), each change
     * in a transaction of its own.
     */
    private record ItemHandlerSource(ResourceHandler<ItemResource> handler) implements ItemSource {
        @Override
        public int size() {
            return handler.size();
        }

        @Override
        public ItemStack getItem(int slot) {
            return ItemUtil.getStack(handler, slot);
        }

        @Override
        public ItemStack extract(int slot, int count) {
            ItemResource resource = handler.getResource(slot);
            if (resource.isEmpty() || count <= 0) return ItemStack.EMPTY;
            try (Transaction transaction = Transaction.openRoot()) {
                int extracted = handler.extract(slot, resource, count, transaction);
                transaction.commit();
                return extracted <= 0 ? ItemStack.EMPTY : resource.toStack(extracted);
            }
        }

        @Override
        public void insert(int slot, ItemStack stack) {
            if (stack.isEmpty()) return;
            try (Transaction transaction = Transaction.openRoot()) {
                handler.insert(slot, ItemResource.of(stack), stack.getCount(), transaction);
                transaction.commit();
            }
        }
    }
}
