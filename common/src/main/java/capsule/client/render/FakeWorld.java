package capsule.client.render;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.attribute.EnvironmentAttributeReader;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.LevelTickAccess;

import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The blocks of a capsule preview or of a capture animation, over a real level for everything else. It is the client
 * BlockAndTintGetter the block models are tessellated against: full light, no directional shading and the biome
 * tints of the delegate, as the 1.21.1 preview had.
 */
public class FakeWorld implements LevelAccessor, BlockAndTintGetter {
    /**
     * No face darker than another, as 1.21.1's getShade returned 1 for every face.
     */
    private static final CardinalLighting NO_SHADING = new CardinalLighting(1, 1, 1, 1, 1, 1);
    private final Level delegate;
    private Map<BlockPos, BlockState> posToBlock;

    public FakeWorld(Level delegate) {
        this.delegate = Objects.requireNonNull(delegate);
        posToBlock = new HashMap<>();
    }

    public Set<Entry<BlockPos, BlockState>> entrySet() {
        return Collections.unmodifiableSet(posToBlock.entrySet());
    }

    public Level getDelegate() {
        return delegate;
    }

    @Override
    public void playSound(@Nullable Entity player, BlockPos pos, SoundEvent soundIn, SoundSource category, float volume, float pitch) {

    }

    @Override
    public void addParticle(ParticleOptions particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {

    }

    @Override
    public void levelEvent(@Nullable Entity p_217378_1_, int p_217378_2_, BlockPos p_217378_3_, int p_217378_4_) {

    }

    @Override
    public void gameEvent(Holder<GameEvent> p_220404_, Vec3 p_220405_, GameEvent.Context p_220406_) {
    }

    @Override
    public List<Entity> getEntities(@Nullable Entity entity, AABB axisAlignedBB, @Nullable Predicate<? super Entity> predicate) {
        return new ArrayList<>();
    }

    @Override
    public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> pEntityTypeTest, AABB pArea, Predicate<? super T> pPredicate) {
        return new ArrayList<>();
    }

    @Override
    public <T extends Entity> List<T> getEntitiesOfClass(Class<T> aClass, AABB axisAlignedBB, @Nullable Predicate<? super T> predicate) {
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
        return delegate.registryAccess();
    }

    @Override
    public FeatureFlagSet enabledFeatures() {
        return delegate.enabledFeatures();
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
    public boolean isFluidAtPosition(BlockPos pPos, Predicate<FluidState> pPredicate) {
        return false;
    }

    @Override
    public long nextSubTickCount() {
        return delegate.nextSubTickCount();
    }

    @Override
    public LevelTickAccess<Block> getBlockTicks() {
        return delegate.getBlockTicks();
    }

    @Override
    public LevelTickAccess<Fluid> getFluidTicks() {
        return delegate.getFluidTicks();
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

    @Nullable
    @Override
    public MinecraftServer getServer() {
        return this.delegate.getServer();
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
    public RandomSource getRandom() {
        return delegate.getRandom();
    }

    /**
     * Checks to see if an air block exists at the provided location. Note that this only checks to see if the blocks
     * material is set to air, meaning it is possible for non-vanilla blocks to still pass this check.
     */
    @Override
    public boolean isEmptyBlock(BlockPos pos) {
        return getBlockState(pos).isAir();
    }

    @Override
    public Holder<Biome> getBiome(BlockPos pos) {
        return delegate.getBiome(pos);
    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(int x, int y, int z) {
        return delegate.getUncachedNoiseBiome(x, y, z);
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
        return delegate.getBiomeManager();
    }

    @Override
    public EnvironmentAttributeReader environmentAttributes() {
        return delegate.environmentAttributes();
    }

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
        if (this.delegate.isOutsideBuildHeight(pos))
            return Blocks.VOID_AIR.defaultBlockState();
        BlockState state = getOverriddenBlockState(pos);
        return state != null ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState(); // In the end that's what mc does
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
        if (this.delegate.isOutsideBuildHeight(pos))
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
        return !this.getBlockState(pos).isAir() && removeBlock(pos, true);
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

    public float getBrightness(BlockPos pos) {
        return 15;
    }

    public int getBrightness(LightLayer pLightType, BlockPos pBlockPos) {
        return 15;
    }

    public int getRawBrightness(BlockPos pBlockPos, int pAmount) {
        return 15;
    }

    public boolean canSeeSky(BlockPos pBlockPos) {
        return true;
    }

    @Override
    public CardinalLighting cardinalLighting() {
        return NO_SHADING;
    }

    /**
     * The biome color at the position in the delegate, without blending (1.21.1's LevelReader#getBlockTint).
     */
    @Override
    public int getBlockTint(BlockPos pos, ColorResolver resolver) {
        return resolver.getColor(getBiome(pos).value(), pos.getX(), pos.getZ());
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return delegate.getLightEngine();
    }

}
