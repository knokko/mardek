package com.github.knokko.ui.renderer;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.buffers.PerFrameBuffer;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.descriptors.DescriptorCombiner;
import com.github.knokko.boiler.descriptors.DescriptorSetLayoutBuilder;
import com.github.knokko.boiler.descriptors.DescriptorUpdater;
import com.github.knokko.boiler.descriptors.VkbDescriptorSetLayout;
import com.github.knokko.boiler.images.ImageBuilder;
import com.github.knokko.boiler.images.VkbImage;
import com.github.knokko.boiler.memory.MemoryCombiner;
import com.github.knokko.boiler.pipelines.GraphicsPipelineBuilder;
import com.github.knokko.boiler.synchronization.ResourceUsage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkPushConstantRange;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class UiRenderInstance {

	private static GraphicsPipelineBuilder basePipelineBuilder(BoilerInstance boiler, MemoryStack stack) {
		var builder = new GraphicsPipelineBuilder(boiler, stack);
		builder.simpleShaderStages(
				"UiShader", "com/github/knokko/ui/renderer/",
				"shader.vert.spv", "shader.frag.spv"
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
			BoilerInstance boiler, long vkRenderPass, int subpass,
			MemoryCombiner memoryCombiner, DescriptorCombiner descriptorCombiner, int maxImages
	) {
		return new UiRenderInstance(
				boiler, memoryCombiner, descriptorCombiner, maxImages,
				vkRenderPass, subpass, 0, 0
		);
	}

	public static UiRenderInstance withDynamicRendering(
			BoilerInstance boiler, int viewMask, int colorAttachmentFormat,
			MemoryCombiner memoryCombiner, DescriptorCombiner descriptorCombiner, int maxImages
	) {
		return new UiRenderInstance(
				boiler, memoryCombiner, descriptorCombiner, maxImages,
				VK_NULL_HANDLE, 0, viewMask, colorAttachmentFormat
		);
	}

	private final BoilerInstance boiler;
	private final long imageSampler, pipelineLayout, graphicsPipeline;
	public final VkbDescriptorSetLayout baseDescriptorSetLayout, imageDescriptorSetLayout;
	private final VkbImage dummyImage;
	private final long[] imageDescriptorSets;
	private int nextDescriptorSet;
	private final Map<VkbImage, Long> imageDescriptorMap = new HashMap<>();

	private UiRenderInstance(
			BoilerInstance boiler, MemoryCombiner memoryCombiner, DescriptorCombiner descriptorCombiner, int maxImages,
			long vkRenderPass, int subpass, int drViewMask, int colorAttachmentFormat
	) {
		this.boiler = boiler;

		this.imageSampler = boiler.images.createSimpleSampler(
				VK_FILTER_NEAREST, VK_SAMPLER_MIPMAP_MODE_NEAREST,
				VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_BORDER, "UiSampler"
		);
		this.dummyImage = memoryCombiner.addImage(new ImageBuilder(
				"DummyImage", 1, 1
		).texture().format(VK_FORMAT_R8_UNORM));

		try (var stack = stackPush()) {

			var baseBuilder = new DescriptorSetLayoutBuilder(stack, 4);
			baseBuilder.set(0, 0, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_VERTEX_BIT);
			baseBuilder.set(1, 1, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_FRAGMENT_BIT);
			baseBuilder.set(2, 2, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT);
			baseBuilder.set(3, 3, VK_DESCRIPTOR_TYPE_SAMPLER, VK_SHADER_STAGE_FRAGMENT_BIT);
			this.baseDescriptorSetLayout = baseBuilder.build(boiler, "UiBaseDescriptorLayout");

			var imageBuilder = new DescriptorSetLayoutBuilder(stack, 1);
			imageBuilder.set(0, 0, VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE, VK_SHADER_STAGE_FRAGMENT_BIT);
			this.imageDescriptorSetLayout = imageBuilder.build(boiler, "UiImageDescriptorLayout");
			this.imageDescriptorSets = descriptorCombiner.addMultiple(imageDescriptorSetLayout, maxImages);

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
	}

	private synchronized long getImageDescriptorSet(VkbImage image) {
		return imageDescriptorMap.computeIfAbsent(image, i -> {
			long descriptorSet = imageDescriptorSets[nextDescriptorSet++];
			try (var stack = stackPush()) {
				// TODO Check if this can be combined with other images
				var imageWrite = VkDescriptorImageInfo.calloc(1, stack);
				//noinspection resource
				imageWrite.get(0).set(VK_NULL_HANDLE, image.vkImageView, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

				var updater = new DescriptorUpdater(stack, 1);
				updater.writeImage(0, descriptorSet, 0, image.vkImageView, VK_NULL_HANDLE);
				updater.update(boiler);
			}

			return descriptorSet;
		});
	}

	public void prepare(CommandRecorder recorder) { // TODO use this
		recorder.transitionLayout(dummyImage, null, ResourceUsage.TRANSFER_DEST);
		recorder.clearColorImage(dummyImage.vkImage, 1f, 0.1f, 0.8f, 1f);
		recorder.transitionLayout(dummyImage, ResourceUsage.TRANSFER_DEST, ResourceUsage.shaderRead(VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT));
	}

	public UiRenderer createRenderer(
			PerFrameBuffer perFrame, int glyphBufferSize,
			MemoryCombiner memoryCombiner, DescriptorCombiner descriptorCombiner
	) {
		var glyphsBuffer = memoryCombiner.addMappedBuffer(
				glyphBufferSize, boiler.deviceProperties.limits().minStorageBufferOffsetAlignment(),
				VK_BUFFER_USAGE_STORAGE_BUFFER_BIT
		);
		var renderer = new UiRenderer(
				boiler, imageSampler, pipelineLayout, graphicsPipeline,
				perFrame, glyphsBuffer, dummyImage, this::getImageDescriptorSet
		);
		descriptorCombiner.addSingle(baseDescriptorSetLayout, set -> renderer.baseDescriptorSet = set);
		return renderer;
	}

	public void destroy() {
		vkDestroyPipeline(boiler.vkDevice(), graphicsPipeline, null);
		vkDestroyPipelineLayout(boiler.vkDevice(), pipelineLayout, null);
		vkDestroyDescriptorSetLayout(boiler.vkDevice(), baseDescriptorSetLayout.vkDescriptorSetLayout, null);
		vkDestroyDescriptorSetLayout(boiler.vkDevice(), imageDescriptorSetLayout.vkDescriptorSetLayout, null);
		vkDestroySampler(boiler.vkDevice(), imageSampler, null);
	}
}
