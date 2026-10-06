package capsule.fabric;

import capsule.CapsuleMod;
import capsule.platform.ItemSource;
import capsule.platform.Platform;
import com.mojang.authlib.GameProfile;
import eu.pb4.common.protection.api.CommonProtection;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class FabricPlatform implements Platform {
    private static final AttachmentType<CompoundTag> PERSISTENT_DATA = AttachmentRegistry.<CompoundTag>builder()
            .persistent(CompoundTag.CODEC)
            .copyOnDeath()
            .initializer(CompoundTag::new)
            .buildAndRegister(ResourceLocation.fromNamespaceAndPath(CapsuleMod.MODID, "persistent_data"));

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public String modDescription(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(mod -> mod.getMetadata().getName() + " " + mod.getMetadata().getVersion().getFriendlyString())
                .orElse(modId);
    }

    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Supplier<T> register(Registry<? super T> registry, String name, Supplier<T> factory) {
        T value = Registry.register((Registry<T>) registry, ResourceLocation.fromNamespaceAndPath(CapsuleMod.MODID, name), factory.get());
        return () -> value;
    }

    @Override
    public CreativeModeTab.Builder creativeTabBuilder() {
        return FabricItemGroup.builder();
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return stack.getRecipeRemainder();
    }

    @Override
    public boolean canPlaceBlock(ServerLevel level, BlockPos pos, Player player) {
        return CommonProtection.canPlaceBlock(level, pos, player.getGameProfile(), player);
    }

    @Override
    public ServerPlayer fakePlayer(ServerLevel level, GameProfile profile) {
        return FakePlayer.get(level, profile);
    }

    @Override
    @Nullable
    public ItemSource getItemSource(Level level, BlockPos pos) {
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(level, pos, null);
        return storage == null ? null : new StorageItemSource(storage);
    }

    @Override
    public BlockState rotate(BlockState state, LevelAccessor level, BlockPos pos, Rotation rotation) {
        return state.rotate(rotation);
    }

    @Override
    public CompoundTag getPersistentData(ServerPlayer player) {
        return player.getAttachedOrCreate(PERSISTENT_DATA);
    }

    /**
     * The views of a Transfer API storage, as slots.
     */
    private static final class StorageItemSource implements ItemSource {
        private final Storage<ItemVariant> storage;
        private final List<StorageView<ItemVariant>> views = new ArrayList<>();

        StorageItemSource(Storage<ItemVariant> storage) {
            this.storage = storage;
            storage.forEach(views::add);
        }

        @Override
        public int size() {
            return views.size();
        }

        @Override
        public ItemStack getItem(int slot) {
            StorageView<ItemVariant> view = views.get(slot);
            return view.isResourceBlank() ? ItemStack.EMPTY : view.getResource().toStack((int) view.getAmount());
        }

        @Override
        public ItemStack extract(int slot, int count) {
            StorageView<ItemVariant> view = views.get(slot);
            ItemVariant resource = view.getResource();
            try (Transaction transaction = Transaction.openOuter()) {
                long extracted = view.extract(resource, count, transaction);
                transaction.commit();
                return resource.toStack((int) extracted);
            }
        }

        @Override
        public void insert(int slot, ItemStack stack) {
            Storage<ItemVariant> target = views.get(slot) instanceof SingleSlotStorage<ItemVariant> slotStorage ? slotStorage : storage;
            try (Transaction transaction = Transaction.openOuter()) {
                target.insert(ItemVariant.of(stack), stack.getCount(), transaction);
                transaction.commit();
            }
        }
    }
}
