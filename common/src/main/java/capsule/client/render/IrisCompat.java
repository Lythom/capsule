package capsule.client.render;

import capsule.platform.Services;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;

/**
 * Iris draws a pipeline it does not know with no shader pack program, so the ghost preview of Capsule's own pipelines
 * came out nearly invisible with a shader pack. Iris API v0 (IrisApi#assignPipeline) maps them to the programs of the
 * vanilla block pipelines they copy: GHOST to the translucent terrain program, GHOST_DEPTH to the cutout terrain one.
 * Called through reflection: Iris is optional and Capsule does not compile against it.
 */
public final class IrisCompat {
    private static final Logger LOGGER = LogManager.getLogger(IrisCompat.class);

    private IrisCompat() {
    }

    public static void assignPipelines() {
        if (!Services.PLATFORM.isModLoaded("iris")) return;
        try {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            @SuppressWarnings({"unchecked", "rawtypes"})
            Class<Enum> programClass = (Class<Enum>) Class.forName("net.irisshaders.iris.api.v0.IrisProgram");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            Method assign = apiClass.getMethod("assignPipeline", RenderPipeline.class, programClass);
            assign.invoke(api, CustomRenderType.GHOST_DEPTH_PIPELINE, Enum.valueOf(programClass, "TERRAIN_CUTOUT"));
            assign.invoke(api, CustomRenderType.GHOST_PIPELINE, Enum.valueOf(programClass, "TRANSLUCENT"));
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.warn("Could not assign the preview pipelines to Iris programs: the preview may not show with a shader pack", e);
        }
    }
}
