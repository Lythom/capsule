package capsule.client.render.vbo;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Matrix4f;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

/**
 * Credits to Customwolf20's (and the team of) Building Gadgets which is under the MIT license as of writing 16/06/2021 (d/m/y)
 * https://github.com/Customwolf20-MC/BuildingGadgets/blob/1.16/src/main/java/com/direwolf20/buildinggadgets/client/renderer/CustomVertexBuffer.java
 */
public class CustomVertexBuffer implements AutoCloseable {

	private int glBufferId;
	private final VertexFormat vertexFormat;
	private int count;

	public CustomVertexBuffer(VertexFormat vertexFormatIn) {
		this.vertexFormat = vertexFormatIn;
		RenderSystem.glGenBuffers((p_227876_1_) -> {
			this.glBufferId = p_227876_1_;
		});
	}

	public void bindBuffer() {
		RenderSystem.glBindBuffer(34962, () -> this.glBufferId);
	}

	public void upload(CustomBufferBuilder bufferIn) {
		if (!RenderSystem.isOnRenderThread()) {
			RenderSystem.recordRenderCall(() -> {
				this.uploadRaw(bufferIn);
			});
		} else {
			this.uploadRaw(bufferIn);
		}

	}

	public CompletableFuture<Void> uploadLater(CustomBufferBuilder bufferIn) {
		if (!RenderSystem.isOnRenderThread()) {
			return CompletableFuture.runAsync(() -> {
				this.uploadRaw(bufferIn);
			}, (p_227877_0_) -> {
				RenderSystem.recordRenderCall(p_227877_0_::run);
			});
		} else {
			this.uploadRaw(bufferIn);
			return CompletableFuture.completedFuture((Void) null);
		}
	}

	private void uploadRaw(CustomBufferBuilder bufferIn) {
		Pair<CustomBufferBuilder.DrawState, ByteBuffer> pair = bufferIn.getNextBuffer();
		if (this.glBufferId != -1) {
			ByteBuffer bytebuffer = pair.getSecond();
			this.count = bytebuffer.remaining() / this.vertexFormat.getVertexSize();
			this.bindBuffer();
			RenderSystem.glBufferData(34962, bytebuffer, 35044);
			unbindBuffer();
		}
	}

	public void draw(Matrix4f matrixIn, int modeIn) {
//        Render
//        RenderSystem.pushMatrix();
//        RenderSystem.loadIdentity();
//        RenderSystem.multMatrix(matrixIn);
//        RenderSystem.drawArrays(modeIn, 0, this.count);
//        RenderSystem.popMatrix();
	}

	public static void unbindBuffer() {
		RenderSystem.glBindBuffer(34962, () -> 0);
	}

	public void close() {
		if (this.glBufferId >= 0) {
			RenderSystem.glDeleteBuffers(this.glBufferId);
			this.glBufferId = -1;
		}

	}
}