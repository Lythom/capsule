/**
 * 
 */
package capsule.dimension;

import net.minecraft.world.biome.Biome;

/**
 * @author Lythom
 *
 */
public class CapsuleBiomeGen extends Biome {
	
	public CapsuleBiomeGen(Biome.BiomeProperties props) {
		
		super(props);

		this.theBiomeDecorator.treesPerChunk = 0;
		this.theBiomeDecorator.flowersPerChunk = 0;
		this.theBiomeDecorator.grassPerChunk = 0;

		this.spawnableMonsterList.clear();
		this.spawnableCreatureList.clear();
		this.spawnableWaterCreatureList.clear();
		this.spawnableCaveCreatureList.clear();
	}
}
