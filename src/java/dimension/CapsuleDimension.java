package dimension;

import net.minecraftforge.common.DimensionManager;

public class CapsuleDimension {

	public static int providerId;
	public static int dimensionId;

	public CapsuleDimension() {
		
	}
	
	public static void registerDimension(){
		
		providerId = 7;
		while(providerId < 10000 && !DimensionManager.registerProviderType(providerId, CapsuleWorldProvider.class, true)){
			providerId++;
		}
		
		dimensionId = DimensionManager.getNextFreeDimId();
		DimensionManager.registerDimension(dimensionId, providerId);
	}

}
