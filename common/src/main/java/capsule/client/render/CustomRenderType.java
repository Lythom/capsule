package capsule.client.render;

import capsule.CapsuleMod;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;

/**
 * Ghost blocks are drawn twice: GHOST_DEPTH keeps the nearest ghost surface of each pixel, then GHOST blends only that
 * surface over the world, so the result does not depend on the drawing order.
 * <p>
 * Both are built like vanilla's block pipelines (RenderPipelines#CUTOUT_BLOCK and TRANSLUCENT_BLOCK: the core/block
 * shader, the GLOBALS, FOG, SAMPLER0_SAMPLER2 and MATRICES_PROJECTION bind groups, the BLOCK vertex format, quads)
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
 * pass, whatever the submit order.
 */
public final class CustomRenderType {
    private CustomRenderType() {
    }

    private static RenderPipeline.Builder ghostPipeline(String name) {
        return RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath(CapsuleMod.MODID, "pipeline/" + name))
                .withVertexShader("core/block")
                .withFragmentShader("core/block")
                .withBindGroupLayout(BindGroupLayouts.GLOBALS)
                .withBindGroupLayout(BindGroupLayouts.FOG)
                .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
                .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
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
     * Registered by each loader so that they are compiled with the vanilla pipelines at resource reload.
     */
    public static final List<RenderPipeline> PIPELINES = List.of(GHOST_DEPTH_PIPELINE, GHOST_PIPELINE);

    private static RenderSetup ghostSetup(RenderPipeline pipeline) {
        return RenderSetup.builder(pipeline)
                .useLightmap()
                // mipmapped block atlas: the sampler of vanilla's moving block render types
                .withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS, () -> RenderSystem.getSamplerCache()
                        .getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.NEAREST, true))
                .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .createRenderSetup();
    }

    public static final RenderType GHOST_DEPTH = RenderType.create("capsule:ghost_depth", ghostSetup(GHOST_DEPTH_PIPELINE));
    public static final RenderType GHOST = RenderType.create("capsule:ghost", ghostSetup(GHOST_PIPELINE));
}
