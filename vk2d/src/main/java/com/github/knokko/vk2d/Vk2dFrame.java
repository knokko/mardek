package com.github.knokko.vk2d;

import com.github.knokko.boiler.buffers.PerFrameBuffer;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.vk2d.batch.Vk2dBatch;

import java.util.ArrayList;
import java.util.List;

public class Vk2dFrame {

	public final PerFrameBuffer perFrameBuffer;
	public final List<Vk2dBatch<?>> batches = new ArrayList<>();

	public int width, height;

	public Vk2dFrame(PerFrameBuffer perFrameBuffer, int width, int height) {
		this.perFrameBuffer = perFrameBuffer;
		this.width = width;
		this.height = height;
	}

	public void record(CommandRecorder recorder) {
		recorder.bindVertexBuffers(0,  perFrameBuffer.buffer);
		for (Vk2dBatch<?> batch : batches) batch.record(recorder);
	}
}
