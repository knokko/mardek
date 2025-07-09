package com.github.knokko.vk2d.pipeline;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.buffers.PerFrameBuffer;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.memory.callbacks.CallbackUserData;
import com.github.knokko.boiler.pipelines.GraphicsPipelineBuilder;
import com.github.knokko.vk2d.batch.MiniBatch;
import com.github.knokko.vk2d.batch.Vk2dBatch;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

import java.util.Objects;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public abstract class Vk2dPipeline {

	public static GraphicsPipelineBuilder pipelineBuilder(Vk2dPipelineContext context, MemoryStack stack) {
		var builder = new GraphicsPipelineBuilder(context.boiler(), stack);
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

	public static void simpleVertexInput(
			GraphicsPipelineBuilder builder, MemoryStack stack,
			VkVertexInputAttributeDescription.Buffer attributes, int vertexSize
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

	protected long vkPipeline;
	private final int[] bytesPerTriangle;
	private final int[] vertexAlignments;

	public Vk2dPipeline() {
		this.bytesPerTriangle = Objects.requireNonNull(getBytesPerTriangle());
		this.vertexAlignments = Objects.requireNonNull(getVertexAlignments());
	}

	protected abstract int[] getBytesPerTriangle();

	protected abstract int[] getVertexAlignments();

	public int getVertexDimensions() {
		return bytesPerTriangle.length;
	}

	public int getBytesPerTriangle(int dimension) {
		return bytesPerTriangle[dimension];
	}

	public int getVertexAlignment(int dimension) {
		return vertexAlignments[dimension];
	}

	public void prepareRecording(CommandRecorder recorder, Vk2dBatch batch) {
		vkCmdBindPipeline(recorder.commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, vkPipeline);
	}

	public abstract void recordBatch(
			CommandRecorder recorder, PerFrameBuffer perFrameBuffer,
			MiniBatch miniBatch, Vk2dBatch batch
	);

	public void recordNonIndexedBatch(
			CommandRecorder recorder, PerFrameBuffer perFrameBuffer, MiniBatch miniBatch
	) {
		int numTriangles = Math.toIntExact(miniBatch.vertexData()[0].position() / bytesPerTriangle[0]);
		if (numTriangles == 0) return;
		int byteOffset = Math.toIntExact(miniBatch.vertexBuffers()[0].offset - perFrameBuffer.buffer.offset);
		int firstVertex = byteOffset / (bytesPerTriangle[0] / 3);
		vkCmdDraw(recorder.commandBuffer, 3 * numTriangles, 1, firstVertex, 0);
	}

	public void destroy(BoilerInstance boiler) {
		try (MemoryStack stack = stackPush()) {
			vkDestroyPipeline(boiler.vkDevice(), vkPipeline, CallbackUserData.PIPELINE.put(stack, boiler));
		}
	}
}
