package capsule.dimension;

import capsule.CapsuleConfig;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.config.Property;

public class CapsuleDimension {

	public static int providerId;
	public static int dimensionId;

	public CapsuleDimension() {
		
	}
	
	public static void registerDimension(){
		
		Property providerIdProp = CapsuleConfig.config.get("Compatibility", "providerId", 7);
		providerIdProp.comment = "Provider id of the capsule dimension (where blocks are sent inside the capsule).\nChange needed only if there is conflict with an other mod using the same providerId.";
		CapsuleDimension.providerId = providerIdProp.getInt();
		CapsuleConfig.config.save();
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
