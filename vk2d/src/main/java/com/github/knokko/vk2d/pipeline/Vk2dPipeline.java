package com.github.knokko.vk2d.pipeline;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.buffers.MappedVkbBuffer;
import com.github.knokko.boiler.buffers.PerFrameBuffer;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.memory.callbacks.CallbackUserData;
import com.github.knokko.boiler.pipelines.GraphicsPipelineBuilder;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public abstract class Vk2dPipeline {

	public static GraphicsPipelineBuilder pipelineBuilder(PipelineContext context) {
		var builder = new GraphicsPipelineBuilder(context.boiler(), context.stack());
		builder.simpleInputAssembly();
		builder.dynamicViewports(1);
		builder.simpleRasterization(VK_CULL_MODE_NONE);
		builder.noMultisampling();
		builder.noDepthStencil();
		builder.simpleColorBlending(1);
		builder.dynamicStates(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR);

		if (context.vkRenderPass() == VK_NULL_HANDLE) {
			builder.dynamicRendering(context.viewMask(), VK_FORMAT_UNDEFINED, VK_FORMAT_UNDEFINED, context.colorFormat());
		} else {
			builder.ciPipeline.renderPass(context.vkRenderPass());
		}

		return builder;
	}

	protected long vkPipeline;
	public final int vertexSize;

	public Vk2dPipeline(int vertexSize) {
		this.vertexSize = vertexSize;
	}

	protected void simpleVertexInput(
			GraphicsPipelineBuilder builder, MemoryStack stack,
			VkVertexInputAttributeDescription.Buffer attributes
	) {
		var bindings = VkVertexInputBindingDescription.calloc(1, stack);
		//noinspection resource
		bindings.get(0).set(0, vertexSize, VK_VERTEX_INPUT_RATE_VERTEX);

		var ciVertexInput = VkPipelineVertexInputStateCreateInfo.calloc(stack);
		ciVertexInput.sType$Default();
		ciVertexInput.pVertexBindingDescriptions(bindings);
		ciVertexInput.pVertexAttributeDescriptions(attributes);

		builder.ciPipeline.pVertexInputState(ciVertexInput);
	}

	public void prepareRecording(CommandRecorder recorder) {
		vkCmdBindPipeline(recorder.commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, vkPipeline);
	}

	public void recordBatch(CommandRecorder recorder, PerFrameBuffer perFrameBuffer, MappedVkbBuffer vertexData) {
		int vertexCount = (int) (vertexData.size / vertexSize);
		int firstVertex = Math.toIntExact((vertexData.offset - perFrameBuffer.buffer.offset) / vertexSize);
		vkCmdDraw(recorder.commandBuffer, vertexCount, 1, firstVertex, 0);
	}

	public void destroy(BoilerInstance boiler) {
		try (MemoryStack stack = stackPush()) {
			vkDestroyPipeline(boiler.vkDevice(), vkPipeline, CallbackUserData.PIPELINE.put(stack, boiler));
		}
	}
}
