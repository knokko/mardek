package com.github.knokko.vk2d.frame;

import com.github.knokko.boiler.buffers.PerFrameBuffer;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.images.VkbImage;

import java.util.Map;

public class Vk2dSwapchainFrame extends Vk2dFrame {

	public final Vk2dRenderStage swapchainStage;

	public Vk2dSwapchainFrame(
			VkbImage swapchainImage, PerFrameBuffer perFrameBuffer, long renderPass,
			Map<Long, Long> imageViewToFramebuffer
	) {
		super(perFrameBuffer, renderPass, imageViewToFramebuffer);
		this.swapchainStage = new Vk2dRenderStage(swapchainImage, perFrameBuffer, null, null);
	}

	@Override
	public void record(CommandRecorder recorder) {
		if (!stages.contains(swapchainStage)) stages.add(swapchainStage);
		super.record(recorder);
	}
}
