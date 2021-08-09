package capsule.client.render;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import org.lwjgl.opengl.GL11;

public class CustomRenderType extends RenderType {
	public CustomRenderType(String nameIn, VertexFormat formatIn, int drawModeIn, int bufferSizeIn, boolean useDelegateIn, boolean needsSortingIn, Runnable setupTaskIn, Runnable clearTaskIn) {
		super(nameIn, formatIn, drawModeIn, bufferSizeIn, useDelegateIn, needsSortingIn, setupTaskIn, clearTaskIn);
	}

	public static final RenderType VISUAL_BLOCK = create("structurevisualizer:block",
			DefaultVertexFormats.BLOCK, GL11.GL_QUADS, 256,
			RenderType.State.builder()
					.setShadeModelState(SMOOTH_SHADE)
					.setLightmapState(LIGHTMAP)
					.setTextureState(BLOCK_SHEET_MIPPED)
					.setLayeringState(VIEW_OFFSET_Z_LAYERING)
					.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
					.setDepthTestState(LEQUAL_DEPTH_TEST)
					.setCullState(NO_CULL)
					.setWriteMaskState(COLOR_DEPTH_WRITE)
					.setAlphaState(MIDWAY_ALPHA)
					.createCompositeState(false)
	);

}
