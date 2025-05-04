package com.github.knokko.ui.renderer;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.buffers.MappedVkbBufferRange;
import com.github.knokko.boiler.buffers.PerFrameBuffer;
import com.github.knokko.boiler.commands.SingleTimeCommands;
import com.github.knokko.boiler.descriptors.SharedDescriptorPool;
import com.github.knokko.boiler.descriptors.VkbDescriptorSetLayout;
import com.github.knokko.boiler.images.ImageBuilder;
import com.github.knokko.boiler.images.VkbImage;
import com.github.knokko.boiler.memory.SharedMemoryBuilder;
import com.github.knokko.boiler.pipelines.GraphicsPipelineBuilder;
import com.github.knokko.boiler.synchronization.ResourceUsage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkPushConstantRange;

import java.util.function.Supplier;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class UiRenderInstance {

	private static GraphicsPipelineBuilder basePipelineBuilder(BoilerInstance boiler, MemoryStack stack) {
		var builder = new GraphicsPipelineBuilder(boiler, stack);
		builder.simpleShaderStages(
				"UiShader", "com/github/knokko/ui/renderer/shader.vert.spv",
				"com/github/knokko/ui/renderer/shader.frag.spv"
		);
		builder.noVertexInput();
		builder.simpleInputAssembly();
		builder.dynamicViewports(1);
		builder.simpleRasterization(VK_CULL_MODE_NONE);
		builder.noMultisampling();
		builder.noDepthStencil();
		builder.simpleColorBlending(1);
		builder.dynamicStates(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR);
		return builder;
	}

	public static UiRenderInstance withRenderPass(
			BoilerInstance boiler, SharedMemoryBuilder sharedMemoryBuilder, long vkRenderPass, int subpass
	) {
		return new UiRenderInstance(boiler, sharedMemoryBuilder, vkRenderPass, subpass, 0, 0);
	}

	public static UiRenderInstance withDynamicRendering(
			BoilerInstance boiler, SharedMemoryBuilder sharedMemoryBuilder, int viewMask, int colorAttachmentFormat
	) {
		return new UiRenderInstance(boiler, sharedMemoryBuilder, VK_NULL_HANDLE, 0, viewMask, colorAttachmentFormat);
	}

	private final BoilerInstance boiler;
	private final long imageSampler, pipelineLayout, graphicsPipeline;
	public final VkbDescriptorSetLayout baseDescriptorSetLayout, imageDescriptorSetLayout;
	private final Supplier<VkbImage> getDummyImage;
	private VkbImage dummyImage;

	private UiRenderInstance(
			BoilerInstance boiler, SharedMemoryBuilder sharedMemoryBuilder,
			long vkRenderPass, int subpass, int drViewMask, int colorAttachmentFormat
	) {
		this.boiler = boiler;

		this.imageSampler = boiler.images.createSimpleSampler(
				VK_FILTER_NEAREST, VK_SAMPLER_MIPMAP_MODE_NEAREST,
				VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_BORDER, "UiSampler"
		);

		try (var stack = stackPush()) {

			var baseBindings = VkDescriptorSetLayoutBinding.calloc(4, stack);
			boiler.descriptors.binding(baseBindings, 0, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_VERTEX_BIT);
			boiler.descriptors.binding(baseBindings, 1, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_FRAGMENT_BIT);
			boiler.descriptors.binding(baseBindings, 2, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT);
			boiler.descriptors.binding(baseBindings, 3, VK_DESCRIPTOR_TYPE_SAMPLER, VK_SHADER_STAGE_FRAGMENT_BIT);
			this.baseDescriptorSetLayout = boiler.descriptors.createLayout(stack, baseBindings, "UiBaseDsLayout");

			var imageBindings = VkDescriptorSetLayoutBinding.calloc(1, stack);
			boiler.descriptors.binding(imageBindings, 0, VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE, VK_SHADER_STAGE_FRAGMENT_BIT);
			this.imageDescriptorSetLayout = boiler.descriptors.createLayout(stack, imageBindings, "UiImageDsLayout");

			var pushConstants = VkPushConstantRange.calloc(1, stack);
			//noinspection resource
			pushConstants.get(0).set(VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT, 0, 12);

			this.pipelineLayout = boiler.pipelines.createLayout(
					pushConstants, "UiPipelineLayout", baseDescriptorSetLayout.vkDescriptorSetLayout,
					imageDescriptorSetLayout.vkDescriptorSetLayout
			);

			var builder = basePipelineBuilder(boiler, stack);
			builder.ciPipeline.layout(pipelineLayout);
			if (vkRenderPass != VK_NULL_HANDLE) {
				builder.ciPipeline.renderPass(vkRenderPass);
				builder.ciPipeline.subpass(subpass);
			} else builder.dynamicRendering(drViewMask, VK_FORMAT_UNDEFINED, VK_FORMAT_UNDEFINED, colorAttachmentFormat);
			this.graphicsPipeline = builder.build("UiPipeline");
		}

		this.getDummyImage = sharedMemoryBuilder.add(new ImageBuilder(
				"DummyUiImage", 1, 1
		).texture().format(VK_FORMAT_R8_UNORM));
	}

	public synchronized UiRenderer createRenderer(
			PerFrameBuffer perFrame, MappedVkbBufferRange glyphsRange, SharedDescriptorPool descriptorPool
	) {
		if (dummyImage == null) {
			dummyImage = getDummyImage.get();
			var commands = new SingleTimeCommands(boiler);
			commands.submit("DummyUiImageTransition", recorder -> {
				recorder.transitionLayout(dummyImage, null, ResourceUsage.TRANSFER_DEST);
				recorder.clearColorImage(dummyImage.vkImage(), 1f, 0.1f, 0.8f, 1f);
				recorder.transitionLayout(dummyImage, ResourceUsage.TRANSFER_DEST, ResourceUsage.shaderRead(VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT));
			});
			commands.destroy();
		}
		return new UiRenderer(
				boiler, imageSampler, pipelineLayout, graphicsPipeline,
				baseDescriptorSetLayout, imageDescriptorSetLayout, descriptorPool,
				perFrame, glyphsRange, dummyImage
		);
	}

	public void destroy() {
		vkDestroyPipeline(boiler.vkDevice(), graphicsPipeline, null);
		vkDestroyPipelineLayout(boiler.vkDevice(), pipelineLayout, null);
		baseDescriptorSetLayout.destroy();
		imageDescriptorSetLayout.destroy();
		vkDestroySampler(boiler.vkDevice(), imageSampler, null);
	}
}
