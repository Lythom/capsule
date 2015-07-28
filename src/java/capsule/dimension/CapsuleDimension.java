package capsule.dimension;

import net.minecraftforge.common.DimensionManager;

public class CapsuleDimension {

	public static int providerId;
	public static int dimensionId;

	public CapsuleDimension() {
		
	}
	
	public static void registerDimension(){
		
		// TODO : have in a config file
		CapsuleDimension.providerId = 7;
		DimensionManager.registerProviderType(CapsuleDimension.providerId, CapsuleWorldProvider.class, true);
		
		CapsuleDimension.dimensionId = -1;
		for (Integer id : DimensionManager.getStaticDimensionIDs()) {
			if(DimensionManager.getProviderType(id) == CapsuleDimension.providerId){
				CapsuleDimension.dimensionId = id;
				break;
			}
		}
		
		if(CapsuleDimension.dimensionId == -1){
			CapsuleDimension.dimensionId = DimensionManager.getNextFreeDimId();
			DimensionManager.registerDimension(CapsuleDimension.dimensionId, CapsuleDimension.providerId);
		}
	}

}
