package com.github.knokko.vk2d;

import com.github.knokko.boiler.buffers.PerFrameBuffer;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.vk2d.pipeline.Vk2dPipeline;

import java.util.ArrayList;
import java.util.List;

public class Vk2dFrame {

	private final PerFrameBuffer perFrameBuffer;
	private final List<Vk2dBatch> batches = new ArrayList<>();

	public int width, height;

	public Vk2dFrame(PerFrameBuffer perFrameBuffer, int width, int height) {
		this.perFrameBuffer = perFrameBuffer;
		this.width = width;
		this.height = height;
	}

	public Vk2dBatch addBatch(Vk2dPipeline pipeline, int initialCapacity) {
		Vk2dBatch batch = new Vk2dBatch(pipeline, perFrameBuffer, initialCapacity, width, height);
		batches.add(batch);
		return batch;
	}

	public void record(CommandRecorder recorder) {
		recorder.bindVertexBuffers(0,  perFrameBuffer.buffer);
		for (Vk2dBatch batch : batches) batch.record(recorder);
	}
}
