package capsule.dimension;

import capsule.Config;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.config.Property;

public class CapsuleDimensionRegistrer {

	public static int dimensionId;

	public CapsuleDimensionRegistrer() {
		
	}
	
	public static void registerDimension(){
		
		Property providerIdProp = Config.config.get("Compatibility", "dimensionId", 2);
		providerIdProp.comment = "Provider id of the capsule dimension (where blocks are sent inside the capsule).\nChange needed only if there is conflict with an other mod using the same providerId.";
		CapsuleDimensionRegistrer.dimensionId = providerIdProp.getInt();
		Config.config.save();
		
		if(!DimensionManager.isDimensionRegistered(CapsuleDimensionRegistrer.dimensionId)){
			DimensionManager.registerProviderType(CapsuleDimensionRegistrer.dimensionId, CapsuleWorldProvider.class, true);
			DimensionManager.registerDimension(CapsuleDimensionRegistrer.dimensionId, CapsuleDimensionRegistrer.dimensionId);
		}
		
	}

}
