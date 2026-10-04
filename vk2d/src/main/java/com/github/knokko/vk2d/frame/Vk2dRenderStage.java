package com.github.knokko.vk2d.frame;

import com.github.knokko.boiler.buffers.PerFrameBuffer;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.images.VkbImage;
import com.github.knokko.boiler.synchronization.ResourceUsage;
import com.github.knokko.vk2d.batch.Vk2dBatch;

import java.util.ArrayList;
import java.util.List;

public class Vk2dRenderStage extends Vk2dStage {

	public final PerFrameBuffer perFrameBuffer;
	public final List<Vk2dBatch> batches = new ArrayList<>();

	public final VkbImage targetImage;
	public final ResourceUsage priorUsage, nextUsage;
	public final int offsetX, offsetY, width, height;

	/**
	 * @param targetImage The image/color attachment onto which this stage will render
	 * @param perFrameBuffer The per-frame buffer that should be used for all short-lived draw/compute memory
	 * @param priorUsage The previous/src usage before starting the rendering, but it will be ignored when
	 *                   {@code nextUsage == null}
	 * @param nextUsage The next/dst usage after ending the rendering, or {@code null} to skip the
	 *                  barriers before/after the rendering
	 */
	public Vk2dRenderStage(
			VkbImage targetImage, PerFrameBuffer perFrameBuffer,
			ResourceUsage priorUsage, ResourceUsage nextUsage
	) {
		this.targetImage = targetImage;
		this.perFrameBuffer = perFrameBuffer;
		this.priorUsage = priorUsage;
		this.nextUsage = nextUsage;
		this.offsetX = 0;
		this.offsetY = 0;
		this.width = targetImage.width;
		this.height = targetImage.height;
	}

	/**
	 * @param targetImage The image/color attachment onto which this stage will render
	 * @param perFrameBuffer The per-frame buffer that should be used for all short-lived draw/compute memory
	 * @param priorUsage The previous/src usage before starting the rendering, but it will be ignored when
	 *                   {@code nextUsage == null}
	 * @param nextUsage The next/dst usage after ending the rendering, or {@code null} to skip the
	 *                  barriers before/after the rendering
	 */
	public Vk2dRenderStage(
			VkbImage targetImage, PerFrameBuffer perFrameBuffer,
			ResourceUsage priorUsage, ResourceUsage nextUsage,
			int offsetX, int offsetY, int width, int height
	) {
		this.targetImage = targetImage;
		this.perFrameBuffer = perFrameBuffer;
		this.priorUsage = priorUsage;
		this.nextUsage = nextUsage;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
		this.width = width;
		this.height = height;
	}

	public void record(CommandRecorder recorder) {
		if (batches.isEmpty()) return;
		if (batches.getFirst().shouldPrintBatchSizes()) {
			System.out.println("Starting render stage with " + batches.size() + " batches");
		}
		recorder.bindVertexBuffers(0,  perFrameBuffer.buffer);
		for (Vk2dBatch batch : batches) batch.record(recorder);
	}

	public boolean isEmpty() {
		for (Vk2dBatch batch : batches) {
			if (!batch.isEmpty()) return false;
		}
		return true;
	}
}
