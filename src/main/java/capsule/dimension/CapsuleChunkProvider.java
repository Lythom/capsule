package capsule.dimension;

import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderGenerate;

public class CapsuleChunkProvider extends ChunkProviderGenerate {

	public static final int SQUARE_CHUNK_SIZE = 256;
	public static final int CHUNK_SIZE = 16;

	final World world;

	public CapsuleChunkProvider(World world, long i) {
		super(world, i, false, null);
		this.world = world;
	}

	@Override
	public Chunk provideChunk(int x, int z) {
		Chunk chunk = new Chunk(this.world, x, z);

		for (int xc = 0; xc < CHUNK_SIZE; xc++) {
			for (int zc = 0; zc < CHUNK_SIZE; zc++) {
				chunk.setBlockState(new BlockPos(xc, 0, zc), Blocks.bedrock.getDefaultState());
			}
		}
		
		if( !chunk.isPopulated() )
		{
			chunk.setTerrainPopulated(true);
			chunk.resetRelightChecks();
		}

		return chunk;
	}

	@Override
	public void populate(IChunkProvider par1iChunkProvider, int par2, int par3) {

	}

	@Override
	public boolean unloadQueuedChunks() {
		return true;
	}
	
	@Override
	public void recreateStructures(Chunk p_180514_1_, int p_180514_2_, int p_180514_3_) {

	}
	
	/**
	 * Generate structures
	 */
	@Override
	public boolean func_177460_a(IChunkProvider p_177460_1_, Chunk p_177460_2_, int p_177460_3_, int p_177460_4_) {
		return false;
	}

}
