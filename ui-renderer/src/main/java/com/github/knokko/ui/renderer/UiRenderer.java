package com.github.knokko.ui.renderer;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.buffers.MappedVkbBuffer;
import com.github.knokko.boiler.buffers.MappedVkbBufferRange;
import com.github.knokko.boiler.buffers.PerFrameBuffer;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.commands.SingleTimeCommands;
import com.github.knokko.boiler.descriptors.GrowingDescriptorBank;
import com.github.knokko.boiler.images.VkbImage;
import com.github.knokko.boiler.synchronization.ResourceUsage;
import com.github.knokko.text.bitmap.*;
import com.github.knokko.text.font.FontData;
import com.github.knokko.text.placement.TextAlignment;
import com.github.knokko.text.placement.TextPlaceRequest;
import com.github.knokko.text.placement.TextPlacer;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Math.toIntExact;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class UiRenderer {

	private static final long QUAD_SIZE = 6 * Integer.BYTES;

	private final BoilerInstance boiler;
	private final GrowingDescriptorBank baseDescriptorBank, imageDescriptorBank;
	private final long pipelineLayout, graphicsPipeline, baseDescriptorSet;
	private final Map<FontData, FontResources> fonts = new HashMap<>();
	private final Map<VkbImage, Long> imageDescriptorSets = new HashMap<>();

	private final VkbImage dummyImage;

	private final PerFrameBuffer perFrame;
	private BitmapGlyphsBuffer glyphsBuffer;
	private MappedVkbBuffer glyphsVkBuffer;
	private long currentDescriptorSet;

	private CommandRecorder recorder;

	UiRenderer(
			BoilerInstance boiler, long imageSampler, long pipelineLayout, long graphicsPipeline,
			GrowingDescriptorBank baseDescriptorBank, GrowingDescriptorBank imageDescriptorBank, PerFrameBuffer perFrame
	) {
		this.boiler = boiler;
		this.pipelineLayout = pipelineLayout;
		this.graphicsPipeline = graphicsPipeline;
		this.baseDescriptorBank = baseDescriptorBank;
		this.baseDescriptorSet = baseDescriptorBank.borrowDescriptorSet("Ui");
		this.imageDescriptorBank = imageDescriptorBank;
		this.perFrame = perFrame;

		this.dummyImage = boiler.images.createSimple(
				1, 1, VK_FORMAT_R8_UNORM, VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT,
				VK_IMAGE_ASPECT_COLOR_BIT, "DummyUiImage"
		);

		var commands = new SingleTimeCommands(boiler);
		commands.submit("DummyUiImageTransition", recorder -> {
			recorder.transitionLayout(dummyImage, null, ResourceUsage.TRANSFER_DEST);
			recorder.clearColorImage(dummyImage.vkImage(), 1f, 0.1f, 0.8f, 1f);
			recorder.transitionLayout(dummyImage, ResourceUsage.TRANSFER_DEST, ResourceUsage.shaderRead(VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT));
		});

		this.glyphsVkBuffer = boiler.buffers.createMapped(100_000, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, "GlyphsBuffer");
		this.glyphsBuffer = new BitmapGlyphsBuffer(glyphsVkBuffer.hostAddress(), (int) glyphsVkBuffer.size());

		try (var stack = stackPush()) {
			var samplerWrite = VkDescriptorImageInfo.calloc(1, stack);
			//noinspection resource
			samplerWrite.get(0).set(imageSampler, VK_NULL_HANDLE, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

			var descriptorWrites = VkWriteDescriptorSet.calloc(4, stack);
			boiler.descriptors.writeBuffer(
					stack, descriptorWrites, baseDescriptorSet, 0,
					VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, perFrame.range.range()
			);
			boiler.descriptors.writeBuffer(
					stack, descriptorWrites, baseDescriptorSet, 1,
					VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, glyphsVkBuffer.fullRange()
			);
			boiler.descriptors.writeBuffer(
					stack, descriptorWrites, baseDescriptorSet, 2,
					VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, perFrame.range.range()
			);
			boiler.descriptors.writeImage(descriptorWrites, baseDescriptorSet, 3, VK_DESCRIPTOR_TYPE_SAMPLER, samplerWrite);
			vkUpdateDescriptorSets(boiler.vkDevice(), descriptorWrites, null);
		}

		commands.destroy();
	}

	private VkbImage targetImage;

	public void begin(CommandRecorder recorder, VkbImage targetImage) {
		this.recorder = recorder;

		if (glyphsBuffer != null) glyphsBuffer.startFrame();
		imageDescriptorSets.values().forEach(imageDescriptorBank::returnDescriptorSet);
		imageDescriptorSets.clear();

		this.targetImage = targetImage;
	}

	public void beginBatch() {
		this.currentDescriptorSet = VK_NULL_HANDLE;
		vkCmdBindPipeline(recorder.commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline);
		recorder.bindGraphicsDescriptors(pipelineLayout, baseDescriptorSet);
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

	private void flushImage(VkbImage image) {
		currentDescriptorSet = getImageDescriptorSet(image);
	}

	public void drawImage(VkbImage image, int minX, int minY, int width, int height) {
		if (height <= 0) return;
		flushImage(image);

		var quadRange = perFrame.allocate(QUAD_SIZE, 4);
		var renderQuads = quadRange.intBuffer();
		renderQuads.put(minX);
		renderQuads.put(minY);
		renderQuads.put(width);
		renderQuads.put(height);
		renderQuads.put(1);
		renderQuads.put(-1);

		vkCmdBindDescriptorSets(
				recorder.commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineLayout, 1,
				recorder.stack.longs(currentDescriptorSet), null
		);
		draw(quadRange);
	}

	public void drawString(
			FontData fontData, String text, int color, int[] outlineColors,
			int minX, int minY, int maxX, int maxY, int baseY, int heightA,
			int minScale, TextAlignment alignment, Gradient... gradients
	) {
		if (heightA <= 0 || maxY < minY) return;
		FontResources font = fonts.computeIfAbsent(fontData, f -> new FontResources(
				f, new TextPlacer(f), new OutlineGlyphRasterizer(f))
		);
		var requests = new ArrayList<TextPlaceRequest>(1);
		requests.add(new TextPlaceRequest(
				text, minX, minY, maxX, maxY, baseY,
				heightA, minScale, alignment, new UserData(color, outlineColors.length)
		));
		var placedGlyphs = font.placer.place(requests);

		List<GlyphQuad> glyphQuads;
		try {
			glyphQuads = glyphsBuffer.bufferGlyphs(font.rasterizer, placedGlyphs);
		} catch (GlyphBufferCapacityException glyphsDoNotFit) {
			throw new RuntimeException("TODO");
		}

		var sharedExtraRange = perFrame.allocate(4L * (5 + outlineColors.length + 7L * gradients.length), 4);
		var sharedExtra = sharedExtraRange.intBuffer();
		sharedExtra.put(color);
		sharedExtra.put(outlineColors.length);
		for (int oc : outlineColors) sharedExtra.put(oc);

		sharedExtra.put(minX);
		sharedExtra.put(minY);
		sharedExtra.put(gradients.length);
		for (var gradient : gradients) putGradient(gradient, sharedExtra);

		var quadRange = perFrame.allocate(glyphQuads.size() * QUAD_SIZE, 4);
		var renderQuads = quadRange.intBuffer();
		for (var quad : glyphQuads) {
			renderQuads.put(quad.minX);
			renderQuads.put(quad.minY);
			renderQuads.put(quad.getWidth());
			renderQuads.put(quad.getHeight());
			renderQuads.put(2);

			var extraRange = perFrame.allocate(16L, 4L);
			var extra = extraRange.intBuffer();
			extra.put(quad.bufferIndex);
			extra.put(quad.sectionWidth);
			extra.put(quad.scale);
			extra.put(toIntExact(sharedExtraRange.offset() / 4));

			renderQuads.put(toIntExact(extraRange.offset() / 4));
		}
		draw(quadRange);
	}

	private void putGradient(Gradient gradient, IntBuffer extra) {
		extra.put(gradient.minX());
		extra.put(gradient.minY());
		extra.put(gradient.width());
		extra.put(gradient.height());
		extra.put(gradient.baseColor());
		extra.put(gradient.rightColor());
		extra.put(gradient.upColor());
	}

	public void fillColor(int minX, int minY, int maxX, int maxY, int color, Gradient... gradients) {
		if (maxX < minX || maxY < minY) return;
		var quadRange = perFrame.allocate(QUAD_SIZE, 4);
		var renderQuad = quadRange.intBuffer();

		var extraRange = perFrame.allocate(4L * (2 + 7L * gradients.length), 4L);
		var extra = extraRange.intBuffer();

		renderQuad.put(minX);
		renderQuad.put(minY);
		renderQuad.put(1 + maxX - minX);
		renderQuad.put(1 + maxY - minY);
		renderQuad.put(3);
		renderQuad.put(toIntExact(extraRange.offset() / 4));

		extra.put(color);
		extra.put(gradients.length);
		for (var gradient : gradients) putGradient(gradient, extra);

		draw(quadRange);
	}

	public void fillColorUnaligned(
			int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4,
			int color, Gradient... gradients
	) {
		var quadRange = perFrame.allocate(QUAD_SIZE, 4);
		var renderQuad = quadRange.intBuffer();

		var extraRange = perFrame.allocate(4L * (10 + 7L * gradients.length), 4L);
		var extra = extraRange.intBuffer();

		renderQuad.put(0);
		renderQuad.put(0);
		renderQuad.put(0);
		renderQuad.put(0);
		renderQuad.put(1003);
		renderQuad.put(toIntExact(extraRange.offset() / 4));

		extra.put(x1).put(y1);
		extra.put(x2).put(y2);
		extra.put(x3).put(y3);
		extra.put(x4).put(y4);
		extra.put(color);
		extra.put(gradients.length);
		for (var gradient : gradients) putGradient(gradient, extra);

		draw(quadRange);
	}

	private void draw(MappedVkbBufferRange quads) {
		// TODO Batch draws?
		if (currentDescriptorSet == VK_NULL_HANDLE) {
			vkCmdBindDescriptorSets(
					recorder.commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineLayout, 1,
					recorder.stack.longs(getImageDescriptorSet(dummyImage)), null
			);
		}
		vkCmdPushConstants(
				recorder.commandBuffer, pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
				0, recorder.stack.ints(toIntExact(quads.offset() / 4), targetImage.width(), targetImage.height())
		);
		vkCmdDraw(recorder.commandBuffer, 6 * toIntExact(quads.size() / QUAD_SIZE), 1, 0, 0);
	}

	public void endBatch() {

	}

	public void end() {

	}

	public void destroy() {
		glyphsVkBuffer.destroy(boiler);
		dummyImage.destroy(boiler);
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
