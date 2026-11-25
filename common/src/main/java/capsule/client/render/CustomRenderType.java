package capsule.client.render;

import capsule.CapsuleMod;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * Ghost blocks are drawn twice: GHOST_DEPTH keeps the nearest ghost surface of each pixel, then GHOST blends only that
 * surface over the world, so the result does not depend on the drawing order.
 * <p>
 * Both are built like vanilla's block pipelines (RenderPipelines#CUTOUT_BLOCK and TRANSLUCENT_BLOCK: the core/block
 * shader, the GLOBALS, FOG, SAMPLER0, SAMPLER2, PROJECTION and DYNAMIC_TRANSFORMS bind groups, the BLOCK vertex format, quads)
 * with the 1.21.1 states of Capsule's render types:
 * <ul>
 * <li>GHOST_DEPTH: alpha cutout at 0.1 (the 1.21.1 cutout shader), depth test and depth write, no color write;</li>
 * <li>GHOST: no alpha cutout (the 1.21.1 translucent shader), translucent blending, color write, depth test without
 * depth write;</li>
 * <li>both: no culling, lightmap, mipmapped block atlas, view offset z layering.</li>
 * </ul>
 * Depth is reversed since 26.2 (DepthStencilState.DEFAULT is GREATER_THAN_OR_EQUAL): the 1.21.1 "less or equal" test
 * is GREATER_THAN_OR_EQUAL here.
 * <p>
 * SubmitNodeCollection#submitCustomGeometry sends a render type without blending to the solid phase and one with
 * blending to the translucent phase, which runs later in the frame: the depth pass is always complete before the color
 * pass, whatever the submit order. With Improved Transparency the translucent phase draws GHOST with
 * GHOST_OIT_PIPELINES.
 */
public final class CustomRenderType {
    private CustomRenderType() {
    }

    private static RenderPipeline.Builder ghostPipeline(String name) {
        return RenderPipeline.builder()
                .withLocation(pipelineId(name))
                .withVertexShader("core/block")
                .withFragmentShader("core/block")
                .withBindGroupLayout(BindGroupLayouts.GLOBALS)
                .withBindGroupLayout(BindGroupLayouts.FOG)
                .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                .withBindGroupLayout(BindGroupLayouts.SAMPLER2)
                .withBindGroupLayout(BindGroupLayouts.PROJECTION)
                .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
                .withVertexBinding(0, DefaultVertexFormat.BLOCK)
                .withPrimitiveTopology(PrimitiveTopology.QUADS)
                .withCull(false);
    }

    public static final RenderPipeline GHOST_DEPTH_PIPELINE = ghostPipeline("ghost_depth")
            .withShaderDefine("ALPHA_CUTOUT", 0.1f)
            .withColorTargetState(new ColorTargetState(Optional.empty(), GpuFormat.RGBA8_UNORM, ColorTargetState.WRITE_NONE))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true))
            .build();

    public static final RenderPipeline GHOST_PIPELINE = ghostPipeline("ghost")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .build();

    /**
     * GHOST with Improved Transparency (26.3): the translucent phase is then drawn by order independent transparency in
     * three stages, each with its own pipeline, and a blended render type without them crashes the client. Built like
     * vanilla's RenderPipelines#OIT_TRANSLUCENT_BLOCK (the core/block shader, the stage layouts added by the OIT
     * builder, SAMPLER2 for the lightmap in the accumulate stage) with GHOST_PIPELINE's states: no culling and no alpha
     * cutout. The stages test the depth without writing it, as GHOST does, so only the surface kept by GHOST_DEPTH shows.
     */
    public static final OitPipelineSet GHOST_OIT_PIPELINES = OitPipelineSet.builder("ghost", RenderPipeline.builder()
                    .withBindGroupLayout(BindGroupLayouts.GLOBALS)
                    .withBindGroupLayout(BindGroupLayouts.FOG)
                    .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                    .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
                    .withVertexShader("core/block")
                    .withFragmentShader("core/block")
                    .withVertexBinding(0, DefaultVertexFormat.BLOCK)
                    .withPrimitiveTopology(PrimitiveTopology.QUADS)
                    .withDepthStencilState(DepthStencilState.DEFAULT)
                    .withCull(false))
            // the builder names them minecraft:pipeline/oit_<stage>_<suffix>: Capsule's namespace instead
            .withDepthBoundsModifier(stage -> stage.withLocation(pipelineId("oit_depth_bounds_ghost")))
            .withTransmittanceModifier(stage -> stage.withLocation(pipelineId("oit_transmittance_ghost")))
            .withAccumulateModifier(stage -> stage.withLocation(pipelineId("oit_accumulate_ghost"))
                    .withBindGroupLayout(BindGroupLayouts.SAMPLER2))
            .build();

    /**
     * Registered by each loader so that they are compiled with the vanilla pipelines at resource reload.
     */
    public static final List<RenderPipeline> PIPELINES = List.of(GHOST_DEPTH_PIPELINE, GHOST_PIPELINE,
            GHOST_OIT_PIPELINES.depthBoundsPipeline(), GHOST_OIT_PIPELINES.transmittancePipeline(), GHOST_OIT_PIPELINES.accumulatePipeline());

    private static Identifier pipelineId(String name) {
        return Identifier.fromNamespaceAndPath(CapsuleMod.MODID, "pipeline/" + name);
    }

    private static RenderSetup ghostSetup(RenderPipeline pipeline, @Nullable OitPipelineSet oitPipelines) {
        RenderSetup.RenderSetupBuilder builder = RenderSetup.builder(pipeline)
                .useLightmap()
                // mipmapped block atlas: the sampler of vanilla's moving block render types
                .withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS, () -> RenderSystem.getSamplerCache()
                        .getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.NEAREST, true))
                .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING);
        if (oitPipelines != null) builder.setOitPipelines(oitPipelines);
        return builder.createRenderSetup();
    }

    public static final RenderType GHOST_DEPTH = RenderType.create("capsule:ghost_depth", ghostSetup(GHOST_DEPTH_PIPELINE, null));
    public static final RenderType GHOST = RenderType.create("capsule:ghost", ghostSetup(GHOST_PIPELINE, GHOST_OIT_PIPELINES));
}
