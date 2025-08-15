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

public class Vk2dBatch {

	protected final Vk2dPipeline pipeline;
	protected final PerFrameBuffer perFrameBuffer;
	protected final List<BatchVertexData> vertices = new ArrayList<>();

	public final int width, height;

	public Vk2dBatch(Vk2dPipeline pipeline, Vk2dRenderStage stage, int initialCapacity) {
		this.pipeline = pipeline;
		this.perFrameBuffer = stage.perFrameBuffer;
		this.width = stage.width;
		this.height = stage.height;
		addVertexBatch(initialCapacity);
		stage.batches.add(this);
	}

	protected void addVertexBatch(int numTriangles) {
		int dimensions = pipeline.getVertexDimensions();
		BatchVertexData vertices = new BatchVertexData(
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

	public BatchVertexData putTriangles(int amount) {
		int index = vertices.size() - 1;
		BatchVertexData last = vertices.get(index);
		if (last.vertexData()[0].remaining() / pipeline.getBytesPerTriangle(0) >= amount) return last;

		int newTriangles = max(amount, 2 * last.vertexData()[0].capacity() / pipeline.getBytesPerTriangle(0));
		addVertexBatch(newTriangles);
		return vertices.get(index + 1);
	}

	public boolean isEmpty() {
		if (pipeline.getVertexDimensions() == 0 || pipeline.getBytesPerTriangle(0) == 0) {
			throw new UnsupportedOperationException("Please override isEmpty()");
		}
		for (BatchVertexData miniBatch : vertices) {
			if (miniBatch.vertexData()[0].position() != 0) return false;
		}
		return true;
	}

	public void record(CommandRecorder recorder) {
		if (isEmpty()) return;
		pipeline.prepareRecording(recorder, this);
		for (BatchVertexData miniBatch : vertices) pipeline.recordBatch(recorder, perFrameBuffer, miniBatch, this);
	}

	protected void putCompressedPosition(ByteBuffer vertices, int x, int y) {
		vertices.putInt(max(x + 10_000, 0) | (max(y + 10_000, 0) << 16));
	}

	public float normalizeX(float x) {
		return 2f * x / width - 1f;
	}

	public float normalizeY(float y) {
		return 2f * y / height - 1;
	}
}
