package trial.common;

import net.minecraft.resources.Identifier;

public final class TrialCommon {
    public static final String MODID = "trial";

    private TrialCommon() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
