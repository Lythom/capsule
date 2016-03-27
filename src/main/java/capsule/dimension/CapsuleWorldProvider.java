package capsule.dimension;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.BiomeProviderSingle;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkGenerator;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class CapsuleWorldProvider extends WorldProvider {

	public CapsuleWorldProvider() {
		this.hasNoSky = true;
	}
	
	@Override
	protected void registerWorldChunkManager()
	{
		BiomeGenBase.BiomeProperties props = new BiomeGenBase.BiomeProperties("Capsule Biome");
		props.setRainDisabled();
		props.setTemperature(-100);
		props.setRainfall(0);
		
		super.worldChunkMgr = new BiomeProviderSingle(new CapsuleBiomeGen(props));
	}

	@Override
	public IChunkGenerator createChunkGenerator()
	{
		return new CapsuleChunkProvider(this.worldObj);
	}

	@Override
	public float calculateCelestialAngle( long par1, float par3 )
	{
		return 0;
	}

	@Override
	public boolean isSurfaceWorld()
	{
		return false;
	}

	@Override
	@SideOnly( Side.CLIENT )
	public float[] calcSunriseSunsetColors( float celestialAngle, float partialTicks )
	{
        return null;
	}
	
	@Override
	public float getSunBrightnessFactor(float par1) {
		return 1.0F;
	}

	@Override
	public boolean canRespawnHere()
	{
		return false;
	}

	@Override
	@SideOnly( Side.CLIENT )
	public boolean isSkyColored()
	{
		return true;
	}

	@Override
	public boolean doesXZShowFog( int par1, int par2 )
	{
		return false;
	}

	@Override
	public boolean isDaytime()
	{
		return true;
	}

	@Override
	public float getStarBrightness( float par1 )
	{
		return 0;
	}

	@Override
	public BlockPos getSpawnPoint()
	{
		return new BlockPos( 0, 0, 0 );
	}

	@Override
	public boolean isBlockHighHumidity(BlockPos pos)
	{
		return false;
	}

	@Override
	public boolean canDoLightning( Chunk chunk )
	{
		return false;
	}
	
	@Override
	public boolean canDoRainSnowIce(Chunk chunk) {
		return false;
	}
	
	@Override
	public DimensionType getDimensionType() {
		return CapsuleDimensionRegistrer.capsuleDimension;
	}
	
	@Override
	protected void generateLightBrightnessTable() {
        for (int i = 0; i <= 15; ++i)
        {
            this.lightBrightnessTable[i] = 8;
        }
	}

}
