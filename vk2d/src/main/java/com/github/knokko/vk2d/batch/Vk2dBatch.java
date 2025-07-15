package com.github.knokko.vk2d.batch;

import com.github.knokko.boiler.buffers.MappedVkbBuffer;
import com.github.knokko.boiler.buffers.PerFrameBuffer;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.vk2d.Vk2dFrame;
import com.github.knokko.vk2d.pipeline.Vk2dPipeline;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.max;

public class Vk2dBatch {

	private final Vk2dPipeline pipeline;
	protected final PerFrameBuffer perFrameBuffer;
	private final List<MappedVkbBuffer> vertexBuffers = new ArrayList<>();
	private final List<ByteBuffer> vertexDataBuffers = new ArrayList<>();

	public final int width, height;

	public Vk2dBatch(Vk2dPipeline pipeline, Vk2dFrame frame, int initialCapacity) {
		this.pipeline = pipeline;
		this.perFrameBuffer = frame.perFrameBuffer;
		this.width = frame.width;
		this.height = frame.height;
		vertexBuffers.add(perFrameBuffer.allocate(
				(long) initialCapacity * pipeline.vertexSize, pipeline.vertexSize
		));
		vertexDataBuffers.add(vertexBuffers.get(0).byteBuffer());
		frame.batches.add(this);
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
		pipeline.prepareRecording(recorder, this);
		for (int index = 0; index < vertexBuffers.size(); index++) {
			int usedBytes = vertexDataBuffers.get(index).position();
			MappedVkbBuffer usedVertexBuffer = vertexBuffers.get(index).child(0L, usedBytes);
			pipeline.recordBatch(recorder, perFrameBuffer, usedVertexBuffer, this);
		}
	}

	public float normalizeX(float x) {
		return 2f * x / width - 1f;
	}

	public float normalizeY(float y) {
		return 2f * y / height - 1;
	}
}
