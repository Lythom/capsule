package capsule.dimension;

import java.util.Collections;
import java.util.List;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.BiomeGenBase.SpawnListEntry;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkGenerator;

public class CapsuleChunkProvider implements IChunkGenerator {

	public static final int SQUARE_CHUNK_SIZE = 256;
	public static final int CHUNK_SIZE = 16;

	final World world;

	public CapsuleChunkProvider(World world) {
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
	public void recreateStructures(Chunk p_180514_1_, int p_180514_2_, int p_180514_3_) {
		
	}
	
	@Override
	public boolean generateStructures(Chunk chunkIn, int x, int z) {
		return false;
	}
	
	@Override
	public List<SpawnListEntry> getPossibleCreatures(EnumCreatureType creatureType, BlockPos pos) {
		return Collections.<BiomeGenBase.SpawnListEntry>emptyList();
	}
	
	@Override
	public BlockPos getStrongholdGen(World worldIn, String structureName, BlockPos position) {
		return null;
	}

	@Override
	public void populate(int x, int z) {
				
	}
	
}
