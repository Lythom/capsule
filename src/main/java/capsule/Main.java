package capsule;

import capsule.client.ClientProxy;
import capsule.tabs.CapsuleTabs;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod(Main.MODID)
public class Main {
    public static final String MODID = "capsule";
    public static ItemGroup tabCapsule = new CapsuleTabs(ItemGroup.getGroupCountSafe(), "capsule");

    public static CommonProxy proxy = DistExecutor.runForDist(() -> ClientProxy::new, () -> CommonProxy::new);;
}
