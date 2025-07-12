package com.github.knokko.vk2d.pipeline;

import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.vk2d.Vk2dFrame;
import com.github.knokko.vk2d.Vk2dShared;
import com.github.knokko.vk2d.batch.Vk2dBatch;
import com.github.knokko.vk2d.batch.Vk2dKimBatch;
import com.github.knokko.vk2d.resource.Vk2dResourceBundle;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R32G32_SFLOAT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R32_UINT;

public class Vk2dKimPipeline extends Vk2dPipeline {

	public static final int VERTEX_SIZE = 20;

	private final long vkPipelineLayout;

	@SuppressWarnings("resource")
	public Vk2dKimPipeline(Vk2dPipelineContext context, Vk2dShared shared, int version) {
		super(VERTEX_SIZE);

		try (MemoryStack stack = stackPush()) {
			var vertexAttributes = VkVertexInputAttributeDescription.calloc(3, stack);
			vertexAttributes.get(0).set(0, 0, VK_FORMAT_R32G32_SFLOAT, 0);
			vertexAttributes.get(1).set(1, 0, VK_FORMAT_R32G32_SFLOAT, 8);
			vertexAttributes.get(2).set(2, 0, VK_FORMAT_R32_UINT, 16);

			var builder = pipelineBuilder(context);
			builder.simpleShaderStages(
					"Kim" + version, "com/github/knokko/vk2d/",
					"kim" + version + ".vert.spv", "kim" + version + ".frag.spv"
			);
			simpleVertexInput(builder, stack, vertexAttributes);
			builder.ciPipeline.layout(shared.kimPipelineLayout);

			this.vkPipeline = builder.build("Vk2dKim" + version + "Pipeline");
			this.vkPipelineLayout = shared.kimPipelineLayout;
		}
	}

	public Vk2dKimBatch addBatch(Vk2dFrame frame, int initialCapacity, Vk2dResourceBundle bundle) {
		return new Vk2dKimBatch(this, frame, initialCapacity, bundle);
	}

	@Override
	public void prepareRecording(CommandRecorder recorder, Vk2dBatch batch) {
		super.prepareRecording(recorder, batch);
		recorder.bindGraphicsDescriptors(vkPipelineLayout, ((Vk2dKimBatch) batch).bundle.fakeImageDescriptorSet);
	}
}
