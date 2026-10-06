package capsule.platform;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
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
import java.util.function.Supplier;

public interface Platform {

    boolean isModLoaded(String modId);

    /**
     * The name and version of a loaded mod, as written in logs and messages.
     */
    String modDescription(String modId);

    Path getConfigDir();

    /**
     * Registers an object of the capsule namespace. The supplier returned gives the registered object once the
     * registry is populated.
     */
    <T> Supplier<T> register(Registry<? super T> registry, String name, Supplier<T> factory);

    CreativeModeTab.Builder creativeTabBuilder();

    ItemStack getCraftingRemainingItem(ItemStack stack);

    /**
     * Asks protection mods (claims, spawn protection) whether the player may place a block at pos.
     */
    boolean canPlaceBlock(ServerLevel level, BlockPos pos, Player player);

    /**
     * A player that is not connected, acting for profile (capture bases act for the player who placed them).
     */
    ServerPlayer fakePlayer(ServerLevel level, GameProfile profile);

    /**
     * The item storage of the block at pos (chests, modded storages), or null if it has none.
     */
    @Nullable
    ItemSource getItemSource(Level level, BlockPos pos);

    /**
     * Rotates a block placed at pos, which blocks may handle differently on some loaders.
     */
    BlockState rotate(BlockState state, LevelAccessor level, BlockPos pos, Rotation rotation);

    /**
     * Data saved with the player and kept on death.
     */
    CompoundTag getPersistentData(ServerPlayer player);
}
