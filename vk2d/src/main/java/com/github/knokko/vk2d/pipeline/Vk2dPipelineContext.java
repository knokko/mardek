package com.github.knokko.vk2d.pipeline;

import com.github.knokko.boiler.BoilerInstance;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkAttachmentDescription;
import org.lwjgl.vulkan.VkAttachmentReference;
import org.lwjgl.vulkan.VkRenderPassCreateInfo;
import org.lwjgl.vulkan.VkSubpassDescription;

import java.nio.LongBuffer;

import static com.github.knokko.boiler.exceptions.VulkanFailureException.assertVkSuccess;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public record Vk2dPipelineContext(
		BoilerInstance boiler, long vkRenderPass, int viewMask, int colorFormat
) {

	public static Vk2dPipelineContext renderPass(BoilerInstance boiler, long vkRenderPass) {
		return new Vk2dPipelineContext(boiler, vkRenderPass, 0, 0);
	}

	public static Vk2dPipelineContext dynamicRendering(
			BoilerInstance boiler, int viewMask, int colorFormat
	) {
		return new Vk2dPipelineContext(boiler, VK_NULL_HANDLE, viewMask, colorFormat);
	}

	public static Vk2dPipelineContext dynamicRendering(BoilerInstance boiler, int colorFormat) {
		return dynamicRendering(boiler, 0, colorFormat);
	}

	public static Vk2dPipelineContext renderPass(BoilerInstance boiler, int targetImageFormat) {
		try (MemoryStack stack = stackPush()) {
			VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.calloc(1, stack);
			VkAttachmentDescription colorAttachment = attachments.get(0);
			colorAttachment.format(targetImageFormat);
			colorAttachment.samples(VK_SAMPLE_COUNT_1_BIT);
			colorAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
			colorAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
			colorAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
			colorAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
			colorAttachment.initialLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
			colorAttachment.finalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

			VkAttachmentReference.Buffer colorReference = VkAttachmentReference.calloc(1, stack);
			colorReference.attachment(0);
			colorReference.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

			VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc(1, stack);
			subpass.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS);
			subpass.pInputAttachments(null);
			subpass.colorAttachmentCount(1);
			subpass.pColorAttachments(colorReference);
			subpass.pResolveAttachments(null);
			subpass.pDepthStencilAttachment(null);
			subpass.pPreserveAttachments(null);

			VkRenderPassCreateInfo ciRenderPass = VkRenderPassCreateInfo.calloc(stack);
			ciRenderPass.sType$Default();
			ciRenderPass.pAttachments(attachments);
			ciRenderPass.pSubpasses(subpass);

			LongBuffer pRenderPass = stack.callocLong(1);
			assertVkSuccess(vkCreateRenderPass(
					boiler.vkDevice(), ciRenderPass, null, pRenderPass
			), "CreateRenderPass", "Vk2dRenderPass");
			long vkRenderPass = pRenderPass.get(0);

			return renderPass(boiler, vkRenderPass);
		}
	}
}
