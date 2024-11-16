package com.github.knokko.ui.renderer;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.buffers.MappedVkbBuffer;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.descriptors.GrowingDescriptorBank;
import com.github.knokko.boiler.images.VkbImage;
import com.github.knokko.text.bitmap.*;
import com.github.knokko.text.font.FontData;
import com.github.knokko.text.placement.TextPlaceRequest;
import com.github.knokko.text.placement.TextPlacer;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class UiRenderer {

	private static final long QUAD_SIZE = 8 * Integer.BYTES;

	private final BoilerInstance boiler;
	private final GrowingDescriptorBank baseDescriptorBank, imageDescriptorBank;
	private final long pipelineLayout, graphicsPipeline, baseDescriptorSet;
	private final Map<FontData, FontResources> fonts = new HashMap<>();
	private final Map<VkbImage, Long> imageDescriptorSets = new HashMap<>();

	private BitmapGlyphsBuffer glyphsBuffer;
	private MappedVkbBuffer glyphsVkBuffer;
	private MappedVkbBuffer quadBuffer;
	private int nextQuad;

	private CommandRecorder recorder;

	UiRenderer(
			BoilerInstance boiler, long imageSampler, long pipelineLayout, long graphicsPipeline,
			GrowingDescriptorBank baseDescriptorBank, GrowingDescriptorBank imageDescriptorBank
	) {
		this.boiler = boiler;
		this.pipelineLayout = pipelineLayout;
		this.graphicsPipeline = graphicsPipeline;
		this.baseDescriptorBank = baseDescriptorBank;
		this.baseDescriptorSet = baseDescriptorBank.borrowDescriptorSet("Ui");
		this.imageDescriptorBank = imageDescriptorBank;

		this.glyphsVkBuffer = boiler.buffers.createMapped(100_000, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, "GlyphsBuffer");
		this.glyphsBuffer = new BitmapGlyphsBuffer(glyphsVkBuffer.hostAddress(), (int) glyphsVkBuffer.size());
		this.quadBuffer = boiler.buffers.createMapped(100_000, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, "UiQuadBuffer");

		try (var stack = stackPush()) {
			var samplerWrite = VkDescriptorImageInfo.calloc(1, stack);
			//noinspection resource
			samplerWrite.get(0).set(imageSampler, VK_NULL_HANDLE, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

			var descriptorWrites = VkWriteDescriptorSet.calloc(3, stack);
			boiler.descriptors.writeBuffer(
					stack, descriptorWrites, baseDescriptorSet, 0,
					VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, quadBuffer.fullRange()
			);
			boiler.descriptors.writeBuffer(
					stack, descriptorWrites, baseDescriptorSet, 1,
					VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, glyphsVkBuffer.fullRange()
			);
			boiler.descriptors.writeImage(descriptorWrites, baseDescriptorSet, 2, VK_DESCRIPTOR_TYPE_SAMPLER, samplerWrite);
			vkUpdateDescriptorSets(boiler.vkDevice(), descriptorWrites, null);
		}
	}

	public void begin(CommandRecorder recorder, VkbImage targetImage) {
		this.recorder = recorder;

		if (glyphsBuffer != null) glyphsBuffer.startFrame();
		imageDescriptorSets.values().forEach(imageDescriptorBank::returnDescriptorSet);
		imageDescriptorSets.clear();
		this.nextQuad = 0;

		vkCmdBindPipeline(recorder.commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline);
		recorder.bindGraphicsDescriptors(pipelineLayout, baseDescriptorSet);
		recorder.dynamicViewportAndScissor(targetImage.width(), targetImage.height());
		vkCmdPushConstants(
				recorder.commandBuffer, pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
				0, recorder.stack.ints(targetImage.width(), targetImage.height())
		);
	}

	private ByteBuffer reserveQuads(int amount) {
		if ((nextQuad + amount) * QUAD_SIZE > quadBuffer.size()) throw new RuntimeException("TODO");

		var range = quadBuffer.mappedRange(nextQuad * QUAD_SIZE, amount * QUAD_SIZE);
		nextQuad += amount;
		return range.byteBuffer();
	}

	private long getImageDescriptorSet(VkbImage image) {
		return imageDescriptorSets.computeIfAbsent(image, i -> {
			long descriptorSet = imageDescriptorBank.borrowDescriptorSet("Image");
			try (var stack = stackPush()) {
				// TODO Check if this can be combined with other images
				var imageWrite = VkDescriptorImageInfo.calloc(1, stack);
				//noinspection resource
				imageWrite.get(0).set(VK_NULL_HANDLE, image.vkImageView(), VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

				var writes = VkWriteDescriptorSet.calloc(1, stack);
				boiler.descriptors.writeImage(writes, descriptorSet, 0, VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE, imageWrite);

				vkUpdateDescriptorSets(boiler.vkDevice(), writes, null);
			}

			return descriptorSet;
		});
	}

	private void flushImage() {
		// TODO Support multiple images
	}

	public void drawImage(VkbImage image, int minX, int minY, int width, int height) {
		flushImage();

		var renderQuads = reserveQuads(1);
		renderQuads.putInt(minX);
		renderQuads.putInt(minY);
		renderQuads.putInt(width);
		renderQuads.putInt(height);
		renderQuads.putInt(-1);
		renderQuads.putInt(-1);
		renderQuads.putInt(1);
		renderQuads.putInt(-1);

		vkCmdBindDescriptorSets(
				recorder.commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineLayout, 1,
				recorder.stack.longs(getImageDescriptorSet(image)), null
		);
	}

	public void drawString(
			FontData fontData, String text, int color,
			int minX, int minY, int maxX, int maxY, int baseY, int heightA
	) {
		FontResources font = fonts.computeIfAbsent(fontData, f -> new FontResources(
				f, new TextPlacer(f), new FreeTypeGlyphRasterizer(f))
		);
		var requests = new ArrayList<TextPlaceRequest>(1);
		requests.add(new TextPlaceRequest(text, minX, minY, maxX, maxY, baseY, heightA, color));
		var placedGlyphs = font.placer.place(requests);

		List<GlyphQuad> glyphQuads;
		try {
			glyphQuads = glyphsBuffer.bufferGlyphs(font.rasterizer, placedGlyphs);
		} catch (GlyphBufferCapacityException glyphsDoNotFit) {
			throw new RuntimeException("TODO");
		}

		var renderQuads = reserveQuads(glyphQuads.size());
		for (var quad : glyphQuads) {
			renderQuads.putInt(quad.minX);
			renderQuads.putInt(quad.minY);
			renderQuads.putInt(quad.getWidth());
			renderQuads.putInt(quad.getHeight());
			renderQuads.putInt(quad.bufferIndex);
			renderQuads.putInt(quad.sectionWidth);
			renderQuads.putInt(quad.scale);
			renderQuads.putInt(color);
		}
	}

	public void end() {
		vkCmdDraw(recorder.commandBuffer, 6 * nextQuad, 1, 0, 0);
	}

	public void destroy() {
		glyphsVkBuffer.destroy(boiler);
		quadBuffer.destroy(boiler);
		fonts.values().forEach(FontResources::destroy);
		baseDescriptorBank.returnDescriptorSet(baseDescriptorSet);
		imageDescriptorSets.values().forEach(imageDescriptorBank::returnDescriptorSet);
		imageDescriptorSets.clear();
	}

	private static class FontResources {

		final FontData font;
		final TextPlacer placer;
		final GlyphRasterizer rasterizer;

		FontResources(FontData font, TextPlacer placer, GlyphRasterizer rasterizer) {
			this.font = font;
			this.placer = placer;
			this.rasterizer = rasterizer;
		}

		void destroy() {
			placer.destroy();
			rasterizer.destroy();
		}
	}
}
