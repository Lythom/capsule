/**
 * 
 */
package capsule.dimension;

import net.minecraft.world.biome.BiomeGenBase;

/**
 * @author Lythom
 *
 */
public class CapsuleBiomeGen extends BiomeGenBase {
	public CapsuleBiomeGen(int id) {
		super(id);
		this.setBiomeName("Capsule Biome");

		this.setDisableRain();
		this.temperature = -100;

		this.theBiomeDecorator.treesPerChunk = 0;
		this.theBiomeDecorator.flowersPerChunk = 0;
		this.theBiomeDecorator.grassPerChunk = 0;

		this.spawnableMonsterList.clear();
		this.spawnableCreatureList.clear();
		this.spawnableWaterCreatureList.clear();
		this.spawnableCaveCreatureList.clear();
	}
}
