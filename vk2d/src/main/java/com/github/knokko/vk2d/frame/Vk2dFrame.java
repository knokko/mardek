package com.github.knokko.vk2d.frame;

import com.github.knokko.boiler.buffers.PerFrameBuffer;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.synchronization.ResourceUsage;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;
import org.lwjgl.vulkan.VkRenderingAttachmentInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.lwjgl.vulkan.VK10.*;

public class Vk2dFrame {

	public final PerFrameBuffer perFrameBuffer;
	public final long renderPass;
	public final Map<Long, Long> imageViewToFramebuffer;
	public final List<Vk2dStage> stages = new ArrayList<>();

	public Vk2dFrame(
			PerFrameBuffer perFrameBuffer, long renderPass,
			Map<Long, Long> imageViewToFramebuffer
	) {
		this.perFrameBuffer = perFrameBuffer;
		this.renderPass = renderPass;
		this.imageViewToFramebuffer = imageViewToFramebuffer;
	}

	public void record(CommandRecorder recorder) {
		VkRenderPassBeginInfo biRenderPass = VkRenderPassBeginInfo.calloc(recorder.stack);
		biRenderPass.sType$Default();
		biRenderPass.renderPass(renderPass);
		biRenderPass.pClearValues(VkClearValue.calloc(1, recorder.stack));
		biRenderPass.clearValueCount(1);

		var dynamicColorAttachments = VkRenderingAttachmentInfo.calloc(1, recorder.stack);

		for (Vk2dStage stage : stages) {
			if (stage instanceof Vk2dRenderStage) {
				Vk2dRenderStage renderStage = (Vk2dRenderStage) stage;
				if (renderStage.isEmpty()) {
					if (renderStage.nextUsage != null && !renderStage.nextUsage.equals(renderStage.priorUsage)) {
						recorder.transitionLayout(renderStage.targetImage, renderStage.priorUsage, renderStage.nextUsage);
					}
					continue;
				}

				if (renderStage.nextUsage != null && !ResourceUsage.COLOR_ATTACHMENT_WRITE.equals(renderStage.priorUsage)) {
					recorder.transitionLayout(renderStage.targetImage, renderStage.priorUsage, ResourceUsage.COLOR_ATTACHMENT_WRITE);
				}
				if (renderPass != VK_NULL_HANDLE) {
					Long framebuffer = imageViewToFramebuffer.get(renderStage.targetImage.vkImageView);
					if (framebuffer == null) {
						throw new IllegalArgumentException(
								"Can't find framebuffer for " + renderStage.targetImage.vkImageView +
										": framebuffers are " + imageViewToFramebuffer
						);
					}
					biRenderPass.framebuffer(framebuffer);
					biRenderPass.renderArea().extent().set(renderStage.width, renderStage.height);

					vkCmdBeginRenderPass(recorder.commandBuffer, biRenderPass, VK_SUBPASS_CONTENTS_INLINE);
					recorder.dynamicViewportAndScissor(renderStage.width, renderStage.height);
					renderStage.record(recorder);
					vkCmdEndRenderPass(recorder.commandBuffer);
				} else {
					recorder.simpleColorRenderingAttachment(
							dynamicColorAttachments.get(0), renderStage.targetImage.vkImageView,
							VK_ATTACHMENT_LOAD_OP_CLEAR, VK_ATTACHMENT_STORE_OP_STORE,
							0f, 0f, 0f, 0f
					);
					recorder.beginSimpleDynamicRendering(
							renderStage.width, renderStage.height,
							dynamicColorAttachments, null, null
					);
					recorder.dynamicViewportAndScissor(renderStage.width, renderStage.height);
					renderStage.record(recorder);
					recorder.endDynamicRendering();
				}

				if (renderStage.nextUsage != null && !renderStage.nextUsage.equals(ResourceUsage.COLOR_ATTACHMENT_WRITE)) {
					recorder.transitionLayout(renderStage.targetImage, ResourceUsage.COLOR_ATTACHMENT_WRITE, renderStage.nextUsage);
				}
			} else {
				Vk2dComputeStage computeStage = (Vk2dComputeStage) stage;
				computeStage.record(recorder);
			}
		}
	}
}
