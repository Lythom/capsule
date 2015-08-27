package capsule.dimension;

import capsule.Config;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.config.Property;

public class CapsuleDimensionRegistrer {

	public static int providerId;
	public static int dimensionId;

	public CapsuleDimensionRegistrer() {
		
	}
	
	public static void registerDimension(){
		
		Property providerIdProp = Config.config.get("Compatibility", "providerId", 7);
		providerIdProp.comment = "Provider id of the capsule dimension (where blocks are sent inside the capsule).\nChange needed only if there is conflict with an other mod using the same providerId.";
		CapsuleDimensionRegistrer.providerId = providerIdProp.getInt();
		Config.config.save();
		DimensionManager.registerProviderType(CapsuleDimensionRegistrer.providerId, CapsuleWorldProvider.class, true);
		
		CapsuleDimensionRegistrer.dimensionId = -1;
		for (Integer id : DimensionManager.getStaticDimensionIDs()) {
			if(DimensionManager.getProviderType(id) == CapsuleDimensionRegistrer.providerId){
				CapsuleDimensionRegistrer.dimensionId = id;
				break;
			}
		}
		
		if(CapsuleDimensionRegistrer.dimensionId == -1){
			CapsuleDimensionRegistrer.dimensionId = DimensionManager.getNextFreeDimId();
			DimensionManager.registerDimension(CapsuleDimensionRegistrer.dimensionId, CapsuleDimensionRegistrer.providerId);
		}
	}

}
