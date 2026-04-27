package com.github.knokko.vk2d.slug;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.buffers.MappedVkbBuffer;
import com.github.knokko.boiler.builders.BoilerBuilder;
import com.github.knokko.boiler.builders.WindowBuilder;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.descriptors.DescriptorCombiner;
import com.github.knokko.boiler.descriptors.DescriptorSetLayoutBuilder;
import com.github.knokko.boiler.descriptors.VkbDescriptorSetLayout;
import com.github.knokko.boiler.memory.MemoryBlock;
import com.github.knokko.boiler.memory.MemoryCombiner;
import com.github.knokko.boiler.pipelines.GraphicsPipelineBuilder;
import com.github.knokko.boiler.synchronization.ResourceUsage;
import com.github.knokko.boiler.window.AcquiredImage;
import com.github.knokko.boiler.window.SimpleWindowRenderLoop;
import com.github.knokko.boiler.window.VkbWindow;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.harfbuzz.HarfBuzz;
import org.lwjgl.util.harfbuzz.HarfBuzzGPU;
import org.lwjgl.util.harfbuzz.hb_glyph_extents_t;
import org.lwjgl.vulkan.*;

import static com.github.knokko.boiler.exceptions.VulkanFailureException.assertVkSuccess;
import static org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_FIFO_KHR;
import static org.lwjgl.vulkan.VK10.*;

public class SlugPlayground extends SimpleWindowRenderLoop {

	static long assertNonZero(long result, String functionName) {
		if (result == 0L) throw new RuntimeException(functionName + " returned 0");
		return result;
	}

	static <T> T assertNonNull(T result, String functionName) {
		if (result == null) throw new RuntimeException(functionName + " returned null");
		return result;
	}

	public static void main(String[] args) {
		var boiler = new BoilerBuilder(
				VK_API_VERSION_1_0, "SlugPlayground", 1
		)
				.useSDL()
				.doNotUseVma()
				.validation()
				.forbidValidationErrors()
				.enableDynamicRendering()
				.addWindow(new WindowBuilder(
					1000, 800, 1
				))
				.build();
		var loop = new SlugPlayground(boiler.window());
		loop.start();
		boiler.destroyInitialObjects();
	}

	private long hbFontBlob, hbFace, hbFont, hbDraw, hbBuffer;
	private VkbDescriptorSetLayout descriptorSetLayout;
	private long descriptorSet, descriptorPool;
	private long graphicsPipeline, pipelineLayout;
	private MappedVkbBuffer vertexBuffer, indexBuffer, glyphBuffer;
	private long glyphBufferView;
	private MemoryBlock memoryBlock;

	public SlugPlayground(VkbWindow window) {
		super(
				window, true, VK_PRESENT_MODE_FIFO_KHR,
				ResourceUsage.COLOR_ATTACHMENT_WRITE, ResourceUsage.COLOR_ATTACHMENT_WRITE
		);
	}

	@SuppressWarnings("resource")
	@Override
	protected void setup(BoilerInstance boiler, MemoryStack stack) {
		super.setup(boiler, stack);

		hbFontBlob = assertNonZero(HarfBuzz.hb_blob_create_from_file_or_fail(
				"importer/src/main/resources/mardek/importer/fonts/274_Nyala.ttf"
		), "hb_blob_create_from_file_or_fail");
		hbFace = assertNonZero(HarfBuzz.hb_face_create_or_fail(hbFontBlob, 0), "hb_face_create_or_fail");
		hbFont = assertNonZero(HarfBuzz.hb_font_create(hbFace), "hb_font_create");
		hbDraw = assertNonZero(HarfBuzzGPU.hb_gpu_draw_create_or_fail(), "hb_gpu_draw_create_or_fail");
		hbBuffer = assertNonZero(HarfBuzz.hb_buffer_create(), "hb_buffer_create");

		var combiner = new MemoryCombiner(boiler, "SlugMemory");
		vertexBuffer = combiner.addMappedDeviceLocalBuffer(
				1000L, 4L, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, 0.5f
		);
		indexBuffer = combiner.addMappedDeviceLocalBuffer(
				24L, 4L, VK_BUFFER_USAGE_INDEX_BUFFER_BIT, 0.5f
		);
		glyphBuffer = combiner.addMappedDeviceLocalBuffer(
				10_000L, 4L, VK_BUFFER_USAGE_STORAGE_TEXEL_BUFFER_BIT, 0.5f
		);
		memoryBlock = combiner.build(false);

		var ciBufferView = VkBufferViewCreateInfo.calloc(stack);
		ciBufferView.sType$Default();
		ciBufferView.buffer(glyphBuffer.vkBuffer);
		ciBufferView.format(VK_FORMAT_R16G16B16A16_SINT);
		ciBufferView.offset(glyphBuffer.offset);
		ciBufferView.range(glyphBuffer.size);

		var pBufferView = stack.callocLong(1);
		assertVkSuccess(vkCreateBufferView(
				boiler.vkDevice(), ciBufferView, null, pBufferView
		), "CreateBufferView", "glyph buffer");
		glyphBufferView = pBufferView.get(0);

		indexBuffer.intBuffer().put(0).put(1).put(2).put(2).put(3).put(0);

		var layoutBuilder = new DescriptorSetLayoutBuilder(stack, 1);
		layoutBuilder.set(0, 0, VK_DESCRIPTOR_TYPE_STORAGE_TEXEL_BUFFER, VK_SHADER_STAGE_FRAGMENT_BIT);
		descriptorSetLayout = layoutBuilder.build(boiler, "SlugDescriptorSetLayout");

		var pushConstants = VkPushConstantRange.calloc(1, stack);
		pushConstants.get(0).set(VK_SHADER_STAGE_VERTEX_BIT, 0, 72);
		pipelineLayout = boiler.pipelines.createLayout(
				pushConstants, "SlugPipelineLayout", descriptorSetLayout.vkDescriptorSetLayout
		);

		var vertexAttributes = VkVertexInputAttributeDescription.calloc(5, stack);
		vertexAttributes.get(0).set(0, 0, VK_FORMAT_R32G32_SFLOAT, 0);
		vertexAttributes.get(1).set(1, 0, VK_FORMAT_R32G32_SFLOAT, 8);
		vertexAttributes.get(2).set(2, 0, VK_FORMAT_R32G32_SFLOAT, 16);
		vertexAttributes.get(3).set(3, 0, VK_FORMAT_R32_SFLOAT, 24);
		vertexAttributes.get(4).set(4, 0, VK_FORMAT_R32_UINT, 28);

		var vertexBindings = VkVertexInputBindingDescription.calloc(1, stack);
		vertexBindings.get(0).set(0, 32, VK_VERTEX_INPUT_RATE_VERTEX);

		var ciVertexInput = VkPipelineVertexInputStateCreateInfo.calloc(stack);
		ciVertexInput.sType$Default();
		ciVertexInput.pVertexAttributeDescriptions(vertexAttributes);
		ciVertexInput.pVertexBindingDescriptions(vertexBindings);

		var pipelineBuilder = new GraphicsPipelineBuilder(boiler, stack);
		pipelineBuilder.simpleShaderStages(
				"slug", "com/github/knokko/vk2d/",
				"slug.vert.spv", "slug.frag.spv"
		);
		pipelineBuilder.ciPipeline.pVertexInputState(ciVertexInput);
		pipelineBuilder.simpleInputAssembly();
		pipelineBuilder.dynamicViewports(1);
		pipelineBuilder.simpleRasterization(VK_CULL_MODE_NONE);
		pipelineBuilder.noMultisampling();
		pipelineBuilder.noDepthStencil();
		pipelineBuilder.simpleColorBlending(1);
		pipelineBuilder.dynamicStates(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR);
		pipelineBuilder.ciPipeline.layout(pipelineLayout);
		pipelineBuilder.dynamicRendering(0, VK_FORMAT_UNDEFINED, VK_FORMAT_UNDEFINED, window.properties.surfaceFormat());
		graphicsPipeline = pipelineBuilder.build("SlugPipeline");

		var descriptorCombiner = new DescriptorCombiner(boiler);
		descriptorCombiner.addSingle(descriptorSetLayout, set -> descriptorSet = set);
		descriptorPool = descriptorCombiner.build("SlugDescriptorPool");

		var writes = VkWriteDescriptorSet.calloc(1, stack);
		writes.get(0).sType$Default();
		writes.get(0).dstSet(descriptorSet);
		writes.get(0).descriptorCount(1);
		writes.get(0).descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_TEXEL_BUFFER);
		writes.get(0).pTexelBufferView(stack.longs(glyphBufferView));
		vkUpdateDescriptorSets(boiler.vkDevice(), writes, null);
	}

	@Override
	protected void recordFrame(
			MemoryStack stack, int frameIndex, CommandRecorder recorder,
			AcquiredImage swapchainImage, BoilerInstance boiler
	) {
		HarfBuzz.hb_buffer_clear_contents(hbBuffer);

		var testString = "ABC";
		// TODO More accurate length (in bytes)
		HarfBuzz.hb_buffer_add_utf8(hbBuffer, stack.UTF8(testString), 0, testString.length());
		HarfBuzz.hb_buffer_guess_segment_properties(hbBuffer);
		HarfBuzz.hb_shape(hbFont, hbBuffer, null);

		var glyphInfos = assertNonNull(HarfBuzz.hb_buffer_get_glyph_infos(hbBuffer), "hb_buffer_get_glyph_infos");
		var glyphPositions = assertNonNull(HarfBuzz.hb_buffer_get_glyph_positions(hbBuffer), "hb_buffer_get_glyph_positions");

		int cursorX = 0;
		int cursorY = 0;
		var glyphBounds = hb_glyph_extents_t.calloc(stack);
		for (int index = 0; index < 1; index++) {
			var glyphInfo = glyphInfos.get(index);
			var glyphPosition = glyphPositions.get(index);

			var glyphID = glyphInfo.codepoint();
			HarfBuzzGPU.hb_gpu_draw_glyph(hbDraw, hbFont, glyphID);

			var hbDrawBlob = assertNonZero(HarfBuzzGPU.hb_gpu_draw_encode(hbDraw), "hb_gpu_draw_encode");
			var drawBuffer = assertNonNull(HarfBuzz.hb_blob_get_data(hbDrawBlob), "hb_blob_get_data");
			glyphBuffer.byteBuffer().put(drawBuffer);

			float magicScale = 1f / 1000f;
			int glyphLoc = 0;
			float emPerPos = 123f; // TODO Hm...

			int glyphBaseX = cursorX + glyphPosition.x_offset();
			int glyphBaseY = cursorY + glyphPosition.y_offset();

			HarfBuzz.hb_font_get_glyph_extents(hbFont, glyphID, glyphBounds);

			float glyphMinX = (glyphBaseX + glyphBounds.x_bearing()) * magicScale;
			float glyphMinY = (glyphBaseY - glyphBounds.y_bearing()) * magicScale;
			float glyphBoundX = glyphMinX + magicScale * glyphBounds.width();
			float glyphBoundY = glyphMinY - magicScale * glyphBounds.height();

			cursorX += glyphPosition.x_advance();
			cursorY += glyphPosition.y_advance();

			var vertexData = vertexBuffer.byteBuffer();

			// Vertex 0 (bottom left)
			vertexData.putFloat(glyphMinX).putFloat(glyphBoundY); // position
			vertexData.putFloat(glyphBounds.x_bearing()).putFloat(glyphBounds.y_bearing()); // texture coordinates
			vertexData.putFloat(-1f).putFloat(1f); // normal
			vertexData.putFloat(emPerPos);
			vertexData.putInt(glyphLoc);

			// Vertex 1 (bottom right)
			vertexData.putFloat(glyphBoundX).putFloat(glyphBoundY); // position
			vertexData.putFloat(glyphBounds.x_bearing() + glyphBounds.width()).putFloat(glyphBounds.y_bearing()); // texture coordinates
			vertexData.putFloat(1f).putFloat(1f); // normal
			vertexData.putFloat(emPerPos);
			vertexData.putInt(glyphLoc);

			// Vertex 2 (top right)
			vertexData.putFloat(glyphBoundX).putFloat(glyphMinY); // position
			vertexData.putFloat(glyphBounds.x_bearing() + glyphBounds.width()).putFloat(glyphBounds.y_bearing() + glyphBounds.height()); // texture coordinates
			vertexData.putFloat(1f).putFloat(-1f); // normal
			vertexData.putFloat(emPerPos);
			vertexData.putInt(glyphLoc);

			// Vertex 3 (top left)
			vertexData.putFloat(glyphMinX).putFloat(glyphMinY); // position
			vertexData.putFloat(glyphBounds.x_bearing()).putFloat(glyphBounds.y_bearing() + glyphBounds.height()); // texture coordinates
			vertexData.putFloat(-1f).putFloat(-1f); // normal
			vertexData.putFloat(emPerPos);
			vertexData.putInt(glyphLoc);

			HarfBuzzGPU.hb_gpu_draw_reset(hbDraw);
			HarfBuzzGPU.hb_gpu_draw_recycle_blob(hbDraw, hbDrawBlob);
		}

		var colorAttachments = recorder.singleColorRenderingAttachment(
				swapchainImage.getImage().vkImageView, VK_ATTACHMENT_LOAD_OP_CLEAR,
				VK_ATTACHMENT_STORE_OP_STORE, 0
		);
		recorder.beginSimpleDynamicRendering(
				swapchainImage.getWidth(), swapchainImage.getHeight(),
				colorAttachments, null, null
		);
		vkCmdBindPipeline(recorder.commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline);
		recorder.bindGraphicsDescriptors(pipelineLayout, descriptorSet);
		recorder.dynamicViewportAndScissor(swapchainImage.getWidth(), swapchainImage.getHeight());

		var pushConstants = stack.calloc(72);
		new Matrix4f().get(pushConstants);
		pushConstants.putFloat(64, swapchainImage.getWidth());
		pushConstants.putFloat(68, swapchainImage.getHeight());

		vkCmdPushConstants(recorder.commandBuffer, pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT, 0, pushConstants);
		recorder.bindIndexBuffer(indexBuffer, VK_INDEX_TYPE_UINT32);
		recorder.bindVertexBuffers(0, vertexBuffer);

		vkCmdDrawIndexed(recorder.commandBuffer, 6, 1, 0, 0, 0);
		recorder.endDynamicRendering();
	}

	@Override
	protected void cleanUp(BoilerInstance boiler) {
		super.cleanUp(boiler);
		vkDestroyDescriptorPool(boiler.vkDevice(), descriptorPool, null);
		vkDestroyPipeline(boiler.vkDevice(), graphicsPipeline, null);
		vkDestroyPipelineLayout(boiler.vkDevice(), pipelineLayout, null);
		vkDestroyDescriptorSetLayout(boiler.vkDevice(), descriptorSetLayout.vkDescriptorSetLayout, null);
		vkDestroyBufferView(boiler.vkDevice(), glyphBufferView, null);
		memoryBlock.destroy(boiler);
		HarfBuzz.hb_buffer_destroy(hbBuffer);
		HarfBuzzGPU.hb_gpu_draw_destroy(hbDraw);
		HarfBuzz.hb_font_destroy(hbFont);
		HarfBuzz.hb_face_destroy(hbFace);
		HarfBuzz.hb_blob_destroy(hbFontBlob);
	}
}
