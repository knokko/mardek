package com.github.knokko.ui.renderer;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.buffers.MappedVkbBuffer;
import com.github.knokko.boiler.buffers.PerFrameBuffer;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.descriptors.DescriptorUpdater;
import com.github.knokko.boiler.images.VkbImage;
import com.github.knokko.text.bitmap.*;
import com.github.knokko.text.font.FontData;
import com.github.knokko.text.placement.TextAlignment;
import com.github.knokko.text.placement.TextPlaceRequest;
import com.github.knokko.text.placement.TextPlacer;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.lang.Math.toIntExact;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class UiRenderer {

	private static final long QUAD_SIZE = 6 * Integer.BYTES;

	private final BoilerInstance boiler;
	private final long pipelineLayout, graphicsPipeline;
	private final Map<FontData, FontResources> fonts = new HashMap<>();

	private final PerFrameBuffer perFrame;
	private final MappedVkbBuffer glyphStorageBuffer;
	private final VkbImage dummyImage;
	private final Function<VkbImage, Long> imageToDescriptorSet;
	private final long imageSampler;
	private long currentDescriptorSet;
	long baseDescriptorSet;
	private BitmapGlyphsBuffer glyphsBuffer;

	private CommandRecorder recorder;

	UiRenderer(
			BoilerInstance boiler, long imageSampler, long pipelineLayout, long graphicsPipeline,
			PerFrameBuffer perFrame, MappedVkbBuffer glyphsBuffer, VkbImage dummyImage,
			Function<VkbImage, Long> imageToDescriptorSet
	) {
		this.boiler = boiler;
		this.pipelineLayout = pipelineLayout;
		this.graphicsPipeline = graphicsPipeline;
		this.dummyImage = dummyImage;
		this.perFrame = perFrame;
		this.glyphStorageBuffer = glyphsBuffer;
		this.imageSampler = imageSampler;
		this.imageToDescriptorSet = imageToDescriptorSet;
	}

	public void prepare() {
		this.glyphsBuffer = new BitmapGlyphsBuffer(glyphStorageBuffer.byteBuffer());
		try (var stack = stackPush()) {
			var updater = new DescriptorUpdater(stack, 4);
			updater.writeStorageBuffer(0, baseDescriptorSet, 0, perFrame.buffer);
			updater.writeStorageBuffer(1, baseDescriptorSet, 1, glyphStorageBuffer);
			updater.writeStorageBuffer(2, baseDescriptorSet, 2, perFrame.buffer);
			updater.writeSampler(3, baseDescriptorSet, 3, imageSampler);
			updater.update(boiler);
		}
	}

	private int viewportWidth, viewportHeight;

	public void begin(CommandRecorder recorder, int viewportWidth, int viewportHeight) {
		this.recorder = recorder;
		if (glyphsBuffer != null) glyphsBuffer.startFrame();
		this.viewportWidth = viewportWidth;
		this.viewportHeight = viewportHeight;
	}

	public void beginBatch() {
		this.currentDescriptorSet = VK_NULL_HANDLE;
		vkCmdBindPipeline(recorder.commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline);
		recorder.bindGraphicsDescriptors(pipelineLayout, baseDescriptorSet);
	}

	private void flushImage(VkbImage image) {
		currentDescriptorSet = imageToDescriptorSet.apply(image);
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

	private int offsetTo(MappedVkbBuffer destination) {
		return toIntExact((destination.offset - perFrame.buffer.offset) / 4L);
	}

	public void drawString(
			FontData fontData, String text, int color, int[] outlineColors,
			int minX, int minY, int maxX, int maxY, int baseY, int heightA,
			int minScale, TextAlignment alignment, Gradient... gradients
	) {
		if (heightA <= 0 || maxY < minY || maxX < minX) return;
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
			extra.put(offsetTo(sharedExtraRange));

			renderQuads.put(offsetTo(extraRange));
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

	private void putGradient(CircleGradient gradient, IntBuffer extra) {
		extra.put((int) (gradient.minRadius() * 1000));
		extra.put((int) (gradient.maxRadius() * 1000));
		extra.put(gradient.minColor());
		extra.put(gradient.maxColor());
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
		renderQuad.put(offsetTo(extraRange));

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
		renderQuad.put(offsetTo(extraRange));

		extra.put(x1).put(y1);
		extra.put(x2).put(y2);
		extra.put(x3).put(y3);
		extra.put(x4).put(y4);
		extra.put(color);
		extra.put(gradients.length);
		for (var gradient : gradients) putGradient(gradient, extra);

		draw(quadRange);
	}

	public void fillCircle(int minX, int minY, int maxX, int maxY, int color, CircleGradient... gradients) {
		if (maxX < minX || maxY < minY) return;
		var quadRange = perFrame.allocate(QUAD_SIZE, 4);
		var renderQuad = quadRange.intBuffer();

		var extraRange = perFrame.allocate(4L * (2 + 4L * gradients.length), 4L);
		var extra = extraRange.intBuffer();

		renderQuad.put(minX);
		renderQuad.put(minY);
		renderQuad.put(1 + maxX - minX);
		renderQuad.put(1 + maxY - minY);
		renderQuad.put(4);
		renderQuad.put(offsetTo(extraRange));

		extra.put(color);
		extra.put(gradients.length);
		for (var gradient : gradients) putGradient(gradient, extra);

		draw(quadRange);
	}

	private void draw(MappedVkbBuffer quads) {
		// TODO Batch draws?
		if (currentDescriptorSet == VK_NULL_HANDLE) {
			vkCmdBindDescriptorSets(
					recorder.commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineLayout, 1,
					recorder.stack.longs(imageToDescriptorSet.apply(dummyImage)), null
			);
		}
		//noinspection SuspiciousNameCombination
		vkCmdPushConstants(
				recorder.commandBuffer, pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
				0, recorder.stack.ints(offsetTo(quads), viewportWidth, viewportHeight)
		);
		vkCmdDraw(recorder.commandBuffer, 6 * toIntExact(quads.size / QUAD_SIZE), 1, 0, 0);
	}

	public void endBatch() {

	}

	public void end() {

	}

	public void destroy() {
		fonts.values().forEach(FontResources::destroy);
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
