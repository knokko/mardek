package com.github.knokko.vk2d.batch;

import com.github.knokko.boiler.buffers.MappedVkbBuffer;
import com.github.knokko.boiler.buffers.PerFrameBuffer;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.vk2d.frame.Vk2dRenderStage;
import com.github.knokko.vk2d.pipeline.Vk2dPipeline;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.max;

/**
 * <p>
 *     Represents a consecutive batch of vertices using the same graphics pipeline, which should be rendered without
 *     being interrupted by vertices of another pipeline. Any non-empty batch will be rendered using exactly 1 call to
 *     <i>vkCmdBindPipeline</i> followed by 1 or more draw calls (usually 1 per {@link MiniBatch}).
 *     Empty batches are simply ignored, and no draw calls or pipeline bindings are wasted on them.
 * </p>
 */
public abstract class Vk2dBatch {

	protected final Vk2dPipeline pipeline;
	protected final PerFrameBuffer perFrameBuffer;
	protected final List<MiniBatch> vertices = new ArrayList<>();

	/**
	 * The size (in pixels) of the {@link Vk2dRenderStage} during which this batch is rendered.
	 */
	public final int width, height;

	/**
	 * @param stage The stage during which this batch is rendered
	 * @param initialCapacity The capacity (in triangles) of the first {@link MiniBatch}
	 */
	protected Vk2dBatch(Vk2dPipeline pipeline, Vk2dRenderStage stage, int initialCapacity) {
		this.pipeline = pipeline;
		this.perFrameBuffer = stage.perFrameBuffer;
		this.width = stage.width;
		this.height = stage.height;
		addVertexBatch(initialCapacity);
		stage.batches.add(this);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	private void addVertexBatch(int numTriangles) {
		int dimensions = pipeline.getVertexDimensions();
		MiniBatch vertices = new MiniBatch(
				new MappedVkbBuffer[dimensions], new ByteBuffer[dimensions]
		);
		for (int dimension = 0; dimension < dimensions; dimension++) {
			vertices.vertexBuffers()[dimension] = perFrameBuffer.allocate(
					(long) numTriangles * pipeline.getBytesPerTriangle(dimension),
					pipeline.getVertexAlignment(dimension)
			);
			vertices.vertexData()[dimension] = vertices.vertexBuffers()[dimension].byteBuffer();
		}
		this.vertices.add(vertices);
	}

	/**
	 * Gets a {@link MiniBatch} that has enough capacity to render {@code amount} triangles (but possibly more).
	 * @param amount The number of required triangles
	 */
	public MiniBatch putTriangles(int amount) {
		int index = vertices.size() - 1;
		MiniBatch last = vertices.get(index);
		if (last.vertexData()[0].remaining() / pipeline.getBytesPerTriangle(0) >= amount) return last;

		int newTriangles = max(amount, 2 * last.vertexData()[0].capacity() / pipeline.getBytesPerTriangle(0));
		addVertexBatch(newTriangles);
		return vertices.get(index + 1);
	}

	/**
	 * Checks whether this batch is empty. A batch is empty when it doesn't have a single vertex.
	 */
	public boolean isEmpty() {
		if (pipeline.getVertexDimensions() == 0 || pipeline.getBytesPerTriangle(0) == 0) {
			throw new UnsupportedOperationException("Please override isEmpty()");
		}
		for (MiniBatch miniBatch : vertices) {
			if (miniBatch.vertexData()[0].position() != 0) return false;
		}
		return true;
	}

	/**
	 * Records the commands to render this batch. <b>Default</b> implementation:
	 * <ul>
	 *     <li>If this batch is empty, no commands will be recorded.</li>
	 *     <li>
	 *         If this batch is non-empty, 1 call to <i>vkCmdBindPipeline</i> is made, followed by 1 call to
	 *         <i>vkCmdDraw</i> <b>per mini batch</b>.
	 *     </li>
	 * </ul>
	 */
	public void record(CommandRecorder recorder) {
		if (isEmpty()) return;
		pipeline.prepareRecording(recorder, this);
		if (pipeline.printBatchSizes) System.out.println("Drawing " + vertices.size() + " batches: " + this);
		for (MiniBatch miniBatch : vertices) {
			if (pipeline.printBatchSizes && miniBatch.vertexData().length > 0) {
				int renderedTriangles = miniBatch.vertexData()[0].position() / pipeline.getBytesPerTriangle(0);
				long reservedTriangles = miniBatch.vertexBuffers()[0].size / pipeline.getBytesPerTriangle(0);
				System.out.println("  " + renderedTriangles + " / " + reservedTriangles + " triangles");
			}
			pipeline.recordBatch(recorder, perFrameBuffer, miniBatch, this);
		}
		if (pipeline.printBatchSizes) System.out.println("Finished " + this);
	}

	public boolean shouldPrintBatchSizes() {
		return pipeline.printBatchSizes;
	}

	/**
	 * Stores two integers in 32 bits, using a format that some of the built-in vk2d shaders use.
	 */
	protected void putCompressedPosition(ByteBuffer vertices, int x, int y) {
		vertices.putInt(max(x + 10_000, 0) | (max(y + 10_000, 0) << 16));
	}

	/**
	 * Translates the given X-coordinate (in pixels) to normalized device coordinates (in range [-1, 1]).
	 */
	public float normalizeX(float x) {
		return 2f * x / width - 1f;
	}

	/**
	 * Translates the given Y-coordinate (in pixels) to normalized device coordinates (in range[-1, 1])
	 */
	public float normalizeY(float y) {
		return 2f * y / height - 1;
	}
}
