package com.github.knokko.vk2d.pipeline;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.memory.callbacks.CallbackUserData;
import com.github.knokko.vk2d.Vk2dBatch;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;

import java.nio.ByteBuffer;

import static com.github.knokko.boiler.utilities.ColorPacker.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class Vk2dColorPipeline extends Vk2dPipeline {

	public static final int VERTEX_SIZE = 24;

	private final long vkPipelineLayout;

	@SuppressWarnings("resource")
	public Vk2dColorPipeline(PipelineContext context) {
		super(VERTEX_SIZE);

		try (MemoryStack stack = stackPush()) {
			this.vkPipelineLayout = context.boiler().pipelines.createLayout(
					null, "Vk2dColorPipelineLayout"
			);

			var vertexAttributes = VkVertexInputAttributeDescription.calloc(2, stack);
			vertexAttributes.get(0).set(0, 0, VK_FORMAT_R32G32_SFLOAT, 0);
			vertexAttributes.get(1).set(1, 0, VK_FORMAT_R32G32B32A32_SFLOAT, 8);

			var builder = pipelineBuilder(context);
			builder.simpleShaderStages(
					"Color/Gradient", "com/github/knokko/vk2d/",
					"color.vert.spv", "color.frag.spv"
			);
			simpleVertexInput(builder, stack, vertexAttributes);
			builder.ciPipeline.layout(vkPipelineLayout);

			this.vkPipeline = builder.build("Vk2dColorPipeline");
		}
	}

	@Override
	public void destroy(BoilerInstance boiler) {
		super.destroy(boiler);
		try (MemoryStack stack = stackPush()) {
			vkDestroyPipelineLayout(
					boiler.vkDevice(), vkPipelineLayout,
					CallbackUserData.PIPELINE_LAYOUT.put(stack, boiler)
			);
		}
	}

	private void putColor(ByteBuffer vertices, int color) {
		vertices.putFloat(normalize(red(color))).putFloat(normalize(green(color)));
		vertices.putFloat(normalize(blue(color))).putFloat(normalize(alpha(color)));
	}

	public void fill(Vk2dBatch batch, int minX, int minY, int maxX, int maxY, int color) {
		ByteBuffer vertices = batch.putVertices(6);
		vertices.putFloat(batch.normalizeX(minX)).putFloat(batch.normalizeY(maxY));
		putColor(vertices, color);
		vertices.putFloat(batch.normalizeX(maxX)).putFloat(batch.normalizeY(maxY));
		putColor(vertices, color);
		vertices.putFloat(batch.normalizeX(maxX)).putFloat(batch.normalizeY(minY));
		putColor(vertices, color);

		vertices.putFloat(batch.normalizeX(maxX)).putFloat(batch.normalizeY(minY));
		putColor(vertices, color);
		vertices.putFloat(batch.normalizeX(minX)).putFloat(batch.normalizeY(minY));
		putColor(vertices, color);
		vertices.putFloat(batch.normalizeX(minX)).putFloat(batch.normalizeY(maxY));
		putColor(vertices, color);
	}
}
