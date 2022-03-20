package capsule.client.render;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.TickList;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.storage.LevelData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

public class FakeWorld implements LevelAccessor {
	private final LevelAccessor delegate;
	private Map<BlockPos, BlockState> posToBlock;

	public FakeWorld(LevelAccessor delegate) {
		this.delegate = Objects.requireNonNull(delegate);
		posToBlock = new HashMap<>();
	}

	public Set<Entry<BlockPos, BlockState>> entrySet() {
		return Collections.unmodifiableSet(posToBlock.entrySet());
	}

	public LevelAccessor getDelegate() {
		return delegate;
	}

	@Override
	public void playSound(@Nullable Player player, BlockPos pos, SoundEvent soundIn, SoundSource category, float volume, float pitch) {

	}

	@Override
	public void addParticle(ParticleOptions particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {

	}

	@Override
	public void levelEvent(@Nullable Player p_217378_1_, int p_217378_2_, BlockPos p_217378_3_, int p_217378_4_) {

	}

	@Override
	public List<Entity> getEntities(@Nullable Entity entity, AABB axisAlignedBB, @Nullable Predicate<? super Entity> predicate) {
		return new ArrayList<>();
	}

	@Override
	public <T extends Entity> List<T> getEntitiesOfClass(Class<? extends T> aClass, AABB axisAlignedBB, @Nullable Predicate<? super T> predicate) {
		return new ArrayList<>();
	}

	@Override
	public List<? extends Player> players() {
		return new ArrayList<>();
	}

	@Nullable
	@Override
	public ChunkAccess getChunk(int p_217353_1_, int p_217353_2_, ChunkStatus status, boolean p_217353_4_) {
		return getChunk(p_217353_1_, p_217353_2_);
	}

	@Override
	public BlockPos getHeightmapPos(Types heightmapType, BlockPos pos) {
		return getDelegate().getHeightmapPos(heightmapType, pos);
	}

	@Override
	public RegistryAccess registryAccess() {
		return null;
	}

	@Override
	public boolean removeBlock(BlockPos blockPos, boolean b) {
		return removeOverride(blockPos);
	}

	@Override
	public boolean isStateAtPosition(BlockPos pos, Predicate<BlockState> statePredicate) {
		return statePredicate.test(getBlockState(pos));
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public int getMoonPhase() {
		return delegate.getMoonPhase();
	}

	@Override
	public TickList<Block> getBlockTicks() {
		return delegate.getBlockTicks();
	}

	@Override
	public TickList<Fluid> getLiquidTicks() {
		return delegate.getLiquidTicks();
	}

	/**
	 * Gets the chunk at the specified location.
	 */
	@Override
	public ChunkAccess getChunk(int chunkX, int chunkZ) {
		return delegate.getChunk(chunkX, chunkZ);
	}

	@Override
	public LevelData getLevelData() {
		return delegate.getLevelData();
	}

	@Override
	public DifficultyInstance getCurrentDifficultyAt(BlockPos pos) {
		return delegate.getCurrentDifficultyAt(pos);
	}

	@Override
	public Difficulty getDifficulty() {
		return delegate.getDifficulty();
	}

	/**
	 * gets the world's chunk provider
	 */
	@Override
	public ChunkSource getChunkSource() {
		return delegate.getChunkSource();
	}


	@Override
	public Random getRandom() {
		return delegate.getRandom();
	}

//    @Override
//    public void updateNeighbors(BlockPos p_230547_1_, Block p_230547_2_) {
//                /*
//        left blank as we can't notify Blocks as this isn't a subclass of world and it makes no sense notifying the Blocks in the delegate
//        as they won't know about this Block...
//        */
//    }

	/**
	 * Checks to see if an air block exists at the provided location. Note that this only checks to see if the blocks
	 * material is set to air, meaning it is possible for non-vanilla blocks to still pass this check.
	 */
	@Override
	public boolean isEmptyBlock(BlockPos pos) {
		return getBlockState(pos).isAir(this, pos);
	}

	@Override
	public Biome getBiome(BlockPos pos) {
		return delegate.getBiome(pos);
	}

	@Override
	public Biome getUncachedNoiseBiome(int x, int y, int z) {
		return null;
	}

	@Override
	public int getHeight(Heightmap.Types heightmapType, int x, int z) {
		return delegate.getHeight(heightmapType, x, z);
	}

	@Override
	public int getSkyDarken() {
		return delegate.getSkyDarken();
	}

	@Override
	public BiomeManager getBiomeManager() {
		return null;
	}

// removed 1.16
//    @Override
//    public BiomeManager getBiomeManager() {
//        return null;
//    }
//

	@Override
	public WorldBorder getWorldBorder() {
		return delegate.getWorldBorder();
	}

	@Override
	public boolean isUnobstructed(@Nullable Entity entityIn, VoxelShape shape) {
		return delegate.isUnobstructed(entityIn, shape);
	}

	@Override
	public int getDirectSignal(BlockPos pos, Direction direction) {
		return delegate.getDirectSignal(pos, direction);
	}

	@Override
	public boolean isClientSide() {
		return delegate.isClientSide();
	}

	@Override
	public int getSeaLevel() {
		return delegate.getSeaLevel();
	}

	@Override
	public DimensionType dimensionType() {
		return delegate.dimensionType();
	}

	@Nullable
	@Override
	public BlockEntity getBlockEntity(BlockPos p_175625_1_) {
		return null;
	}

	@Override
	public BlockState getBlockState(BlockPos pos) {
		if (Level.isOutsideBuildHeight(pos))
			return Blocks.VOID_AIR.defaultBlockState();
		BlockState state = getOverriddenBlockState(pos);
		return state != null ? state : Blocks.AIR.defaultBlockState();
	}

	@Override
	public FluidState getFluidState(BlockPos pos) {
		return getBlockState(pos).getFluidState(); // In the end that's what mc does
	}

	@Override
	public int getMaxLightLevel() {
		return delegate.getMaxLightLevel();
	}

	@Override
	public boolean setBlock(BlockPos p_241211_1_, BlockState p_241211_2_, int p_241211_3_, int p_241211_4_) {
		return false;
	}

	/**
	 * Sets a block state into this world.Flags are as follows:
	 * 1 will cause a block update.
	 * 2 will send the change to clients.
	 * 4 will prevent the block from being re-rendered.
	 * 8 will force any re-renders to run on the main thread instead
	 * 16 will prevent neighbor reactions (e.g. fences connecting, observers pulsing).
	 * 32 will prevent neighbor reactions from spawning drops.
	 * 64 will signify the block is being moved.
	 * Flags can be OR-ed
	 */
	@Override
	public boolean setBlock(BlockPos pos, BlockState newState, int flags) {
		if (Level.isOutsideBuildHeight(pos))
			return false;
		posToBlock.put(pos, newState);
		return true;
	}

	/**
	 * Sets a block to air, but also plays the sound and particles and can spawn drops
	 */
	@Override
	public boolean destroyBlock(BlockPos pos, boolean dropBlock) {
		// adapted from World
		return ! this.getBlockState(pos).isAir(this, pos) && removeBlock(pos, true);
	}

	@Override
	public boolean destroyBlock(BlockPos pos, boolean dropBlock, @Nullable Entity entity, int recursionLeft) {
		return false;
	}

	//-------------------Extra Methods--------------------

	@Nullable
	public BlockState getOverriddenBlockState(BlockPos pos) {
		return posToBlock.get(pos);
	}

	public void clear() {
		posToBlock.clear();
	}

	public boolean removeOverride(BlockPos pos) {
		BlockState state = posToBlock.remove(pos);
		if (state != null) {
			return true;
		}
		return false;
	}

	@Override
	public float getBrightness(BlockPos pos) {
		return delegate.getBrightness(pos);
	}

	@Override
	public float getShade(Direction p_230487_1_, boolean p_230487_2_) {
		return 0;
	}

	@Override
	public LevelLightEngine getLightEngine() {
		return delegate.getLightEngine();
	}
}