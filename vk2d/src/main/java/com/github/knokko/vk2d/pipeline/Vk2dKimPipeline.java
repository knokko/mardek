package com.github.knokko.vk2d.pipeline;

import com.github.knokko.boiler.buffers.PerFrameBuffer;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.vk2d.frame.Vk2dRenderStage;
import com.github.knokko.vk2d.Vk2dInstance;
import com.github.knokko.vk2d.batch.MiniBatch;
import com.github.knokko.vk2d.batch.Vk2dBatch;
import com.github.knokko.vk2d.batch.Vk2dKimBatch;
import com.github.knokko.vk2d.resource.Vk2dResourceBundle;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class Vk2dKimPipeline extends Vk2dPipeline {

	public static final int VERTEX_SIZE = 16;
	private static final int[] BYTES_PER_TRIANGLE = { 3 * VERTEX_SIZE };
	private static final int[] VERTEX_ALIGNMENTS = { VERTEX_SIZE };

	private final long vkPipelineLayout;

	@SuppressWarnings("resource")
	public Vk2dKimPipeline(Vk2dPipelineContext context, Vk2dInstance instance, int version) {
		super();

		try (MemoryStack stack = stackPush()) {
			var vertexAttributes = VkVertexInputAttributeDescription.calloc(3, stack);
			vertexAttributes.get(0).set(0, 0, VK_FORMAT_R32_UINT, 0);
			vertexAttributes.get(1).set(1, 0, VK_FORMAT_R32G32_SFLOAT, 4);
			vertexAttributes.get(2).set(2, 0, VK_FORMAT_R32_UINT, 12);

			var builder = pipelineBuilder(context, stack);
			builder.simpleShaderStages(
					"Kim" + version, "com/github/knokko/vk2d/",
					"kim" + version + ".vert.spv", "kim" + version + ".frag.spv"
			);
			simpleVertexInput(builder, stack, vertexAttributes, VERTEX_SIZE);
			builder.ciPipeline.layout(instance.kimPipelineLayout);

			this.vkPipeline = builder.build("Vk2dKim" + version + "Pipeline");
			this.vkPipelineLayout = instance.kimPipelineLayout;
		}
	}

	public Vk2dKimBatch addBatch(Vk2dRenderStage stage, int initialCapacity, Vk2dResourceBundle bundle) {
		return new Vk2dKimBatch(this, stage, initialCapacity, bundle);
	}

	@Override
	protected int[] getBytesPerTriangle() {
		return BYTES_PER_TRIANGLE;
	}

	@Override
	protected int[] getVertexAlignments() {
		return VERTEX_ALIGNMENTS;
	}

	@Override
	public void prepareRecording(CommandRecorder recorder, Vk2dBatch batch) {
		super.prepareRecording(recorder, batch);
		recorder.bindGraphicsDescriptors(vkPipelineLayout, ((Vk2dKimBatch) batch).bundle.fakeImageDescriptorSet);
		vkCmdPushConstants(
				recorder.commandBuffer, vkPipelineLayout, VK_SHADER_STAGE_VERTEX_BIT,
				0, recorder.stack.ints(batch.width, batch.height)
		);
	}

	@Override
	public void recordBatch(CommandRecorder recorder, PerFrameBuffer perFrameBuffer, MiniBatch miniBatch, Vk2dBatch batch) {
		recordNonIndexedBatch(recorder, perFrameBuffer, miniBatch);
	}
}
