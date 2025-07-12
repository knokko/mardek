package com.github.knokko.vk2d.batch;

import com.github.knokko.boiler.buffers.MappedVkbBuffer;
import com.github.knokko.boiler.buffers.PerFrameBuffer;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.vk2d.pipeline.Vk2dPipeline;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.max;

public class Vk2dBatch<P extends Vk2dPipeline<?>> {

	public final P pipeline;
	protected final PerFrameBuffer perFrameBuffer;
	private final List<MappedVkbBuffer> vertexBuffers = new ArrayList<>();
	private final List<ByteBuffer> vertexDataBuffers = new ArrayList<>();

	public final int width, height;

	public Vk2dBatch(P pipeline, PerFrameBuffer perFrameBuffer, int initialCapacity, int width, int height) {
		this.pipeline = pipeline;
		this.perFrameBuffer = perFrameBuffer;
		this.width = width;
		this.height = height;
		vertexBuffers.add(perFrameBuffer.allocate(
				(long) initialCapacity * pipeline.vertexSize, pipeline.vertexSize
		));
		vertexDataBuffers.add(vertexBuffers.get(0).byteBuffer());
	}

	public ByteBuffer putVertices(int amount) {
		int index = vertexBuffers.size() - 1;
		int requiredBytes = amount * pipeline.vertexSize;
		ByteBuffer last = vertexDataBuffers.get(index);
		if (last.remaining() >= requiredBytes) return vertexDataBuffers.get(index);

		int newBytes = max(requiredBytes, 2 * last.capacity());

		MappedVkbBuffer newMappedBuffer = perFrameBuffer.allocate(newBytes, pipeline.vertexSize);
		vertexBuffers.add(newMappedBuffer);
		ByteBuffer newDataBuffer = newMappedBuffer.byteBuffer();
		vertexDataBuffers.add(newDataBuffer);
		return newDataBuffer;
	}

	public void record(CommandRecorder recorder) {
		if (vertexDataBuffers.get(0).position() == 0) return;
		pipeline.prepareRecording(recorder, width, height);
		for (int index = 0; index < vertexBuffers.size(); index++) {
			int usedBytes = vertexDataBuffers.get(index).position();
			MappedVkbBuffer usedVertexBuffer = vertexBuffers.get(index).child(0L, usedBytes);
			pipeline.recordBatch(recorder, perFrameBuffer, usedVertexBuffer, this);
		}
	}

	public float normalizeX(int x) {
		return 2f * x / width - 1f;
	}

	public float normalizeY(int y) {
		return 2f * y / height - 1;
	}
}
