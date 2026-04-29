package com.github.knokko.vk2d.text;

import com.github.knokko.boiler.buffers.MappedVkbBuffer;
import com.github.knokko.boiler.buffers.VkbBuffer;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.descriptors.BulkDescriptorUpdater;
import com.github.knokko.boiler.descriptors.DescriptorCombiner;
import com.github.knokko.boiler.images.VkbImage;
import com.github.knokko.boiler.memory.MemoryCombiner;
import com.github.knokko.boiler.synchronization.ResourceUsage;
import com.github.knokko.compressor.Bc4Compressor;
import com.github.knokko.vk2d.Vk2dInstance;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.harfbuzz.hb_glyph_extents_t;
import org.lwjgl.util.harfbuzz.hb_glyph_position_t;

import java.util.*;

import static com.github.knokko.boiler.utilities.BoilerMath.nextMultipleOf;
import static com.github.knokko.vk2d.text.HarfbuzzChecks.assertHbSuccess;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static org.lwjgl.util.harfbuzz.HarfBuzz.*;
import static org.lwjgl.vulkan.VK10.*;

public class Vk2dFont {

	public static void generateAtlases(
			Vk2dInstance instance, CommandRecorder recorder,
			Bc4Compressor compressor, Vk2dFont[] fonts
	) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			var updater = new BulkDescriptorUpdater(instance.boiler, stack, 30,30, 60);
			for (var font : fonts) {
				for (var atlas : font.atlases) atlas.updateDescriptors(updater, instance, font.outlines);
			}
			updater.finish();
		}

		var uncompressedBcBuffers = Arrays.stream(fonts).flatMap(
				font -> font.atlases.stream().filter(atlas -> atlas.bitsPerPixel == 4).map(
						atlas -> atlas.uncompressedBcBuffer
				)
		).toArray(VkbBuffer[]::new);

		var compressedBcBuffers = Arrays.stream(fonts).flatMap(
				font -> font.atlases.stream().filter(atlas -> atlas.bitsPerPixel == 4).map(
						atlas -> atlas.compressedBcBuffer
				)
		).toArray(VkbBuffer[]::new);

		var outlineStagingBuffers = Arrays.stream(fonts).map(
				font -> font.outlines.stagingBuffer).toArray(MappedVkbBuffer[]::new
		);
		var outlineBuffers = Arrays.stream(fonts).map(
				font -> font.outlines.persistentBuffer).toArray(VkbBuffer[]::new
		);
		recorder.bulkCopyBuffers(outlineStagingBuffers, outlineBuffers);
		recorder.bulkBufferBarrier(ResourceUsage.TRANSFER_DEST, ResourceUsage.shaderRead(
				VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT | VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT
		), outlineBuffers);

		var atlasGenerationTargets = Arrays.stream(fonts).flatMap(font -> font.atlases.stream().flatMap(
				atlas -> Arrays.stream(new VkbImage[] {
						atlas.bitsPerPixel == 4 ? null : atlas.image, atlas.dummyImage8, atlas.dummyImage16
				})
		)).filter(Objects::nonNull).toArray(VkbImage[]::new);

		recorder.bulkTransitionLayout(null, ResourceUsage.TRANSFER_DEST, atlasGenerationTargets);
		for (var font : fonts) {
			for (var atlas : font.atlases) {
				recorder.clearColorImage(
						atlas.bitsPerPixel == 4 ? atlas.dummyImage8.vkImage : atlas.image.vkImage,
						-1f, 0f, 0f, 0f
				);
			}
		}
		recorder.bulkTransitionLayout(ResourceUsage.TRANSFER_DEST, new ResourceUsage(
				VK_IMAGE_LAYOUT_GENERAL, VK_ACCESS_SHADER_WRITE_BIT, VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT
		), atlasGenerationTargets);
		vkCmdBindPipeline(recorder.commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, instance.sdfGeneratePipeline);

		var pushConstants = recorder.stack.calloc(12 * 4);
		for (var font : fonts) {
			for (var atlas : font.atlases) {
				recorder.bindComputeDescriptors(instance.sdfGeneratePipelineLayout, atlas.getGenerateDescriptorSet());
				for (int glyph = 0; glyph < font.glyphExtents.capacity(); glyph++) {
					var extents = font.glyphExtents.get(glyph);
					if (extents.width() == 0 || extents.height() == 0) continue;
					if (atlas.supportedGlyphs != null && !atlas.supportedGlyphs.contains(glyph)) continue;

					int glyphWidth = atlas.getWidth(glyph);
					int glyphHeight = atlas.getHeight(glyph);
					pushConstants.putInt(atlas.getMinX(glyph)).putInt(atlas.getMinY(glyph));
					pushConstants.putInt(glyphWidth).putInt(glyphHeight);
					pushConstants.putInt(font.outlines.getLinesOffset(glyph)).putInt(font.outlines.getNumLines(glyph));
					pushConstants.putInt(font.outlines.getCurvesOffset(glyph)).putInt(font.outlines.getNumCurves(glyph));
					pushConstants.putFloat(atlas.outlineScale).putFloat(atlas.distanceScale).putInt(atlas.distanceMargin);
					pushConstants.putInt(atlas.bitsPerPixel);
					pushConstants.position(0);
					vkCmdPushConstants(recorder.commandBuffer, instance.sdfGeneratePipelineLayout, VK_SHADER_STAGE_COMPUTE_BIT, 0, pushConstants);

					int groupSize = 8;
					int groupsX = nextMultipleOf(glyphWidth, groupSize) / groupSize;
					int groupsY = nextMultipleOf(glyphHeight, groupSize) / groupSize;
					vkCmdDispatch(recorder.commandBuffer, groupsX, groupsY, 1);
				}
			}
		}

		var standardImagesToSample = Arrays.stream(fonts).flatMap(
				font -> font.atlases.stream().filter(atlas -> atlas.bitsPerPixel != 4).map(
						atlas -> atlas.image
				)
		).toArray(VkbImage[]::new);

		var afterGenerationUsage = new ResourceUsage(
				VK_IMAGE_LAYOUT_GENERAL, VK_ACCESS_SHADER_WRITE_BIT, VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT
		);
		var finalAtlasUsage = ResourceUsage.shaderRead(VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT);
		recorder.bulkTransitionLayout(afterGenerationUsage, finalAtlasUsage, standardImagesToSample);

		var imagesToCompress = Arrays.stream(fonts).flatMap(
				font -> font.atlases.stream().filter(atlas -> atlas.bitsPerPixel == 4).map(
						atlas -> atlas.dummyImage8
				)
		).toArray(VkbImage[]::new);
		recorder.bulkTransitionLayout(afterGenerationUsage, ResourceUsage.TRANSFER_SOURCE, imagesToCompress);


		recorder.bulkCopyImageToBuffers(imagesToCompress, uncompressedBcBuffers);
		recorder.bulkBufferBarrier(
				ResourceUsage.TRANSFER_DEST,
				ResourceUsage.computeBuffer(VK_ACCESS_SHADER_READ_BIT),
				uncompressedBcBuffers
		);

		if (compressor != null) compressor.bindPipeline(recorder);

		for (var font : fonts) {
			for (var atlas : font.atlases) {
				if (atlas.bitsPerPixel == 4) {
					assert compressor != null;
					compressor.compress(
							recorder, atlas.getCompressDescriptorSet(), atlas.uncompressedBcBuffer,
							atlas.compressedBcBuffer, atlas.image.width, atlas.image.height, true
					);
				}
			}
		}

		recorder.bulkBufferBarrier(
				ResourceUsage.computeBuffer(VK_ACCESS_SHADER_WRITE_BIT),
				ResourceUsage.TRANSFER_SOURCE,
				compressedBcBuffers
		);

		var compressedBcImages = Arrays.stream(fonts).flatMap(
				font -> font.atlases.stream().filter(atlas -> atlas.bitsPerPixel == 4).map(
						atlas -> atlas.image
				)
		).toArray(VkbImage[]::new);
		recorder.bulkTransitionLayout(null, ResourceUsage.TRANSFER_DEST, compressedBcImages);
		recorder.bulkCopyBufferToImage(compressedBcImages, compressedBcBuffers);
		recorder.bulkTransitionLayout(ResourceUsage.TRANSFER_DEST, finalAtlasUsage, compressedBcImages);
	}

	public final long hbFace, hbFont;
	public final int fontHeightA;
	private final int whitespaceAdvance;

	public final hb_glyph_extents_t.Buffer glyphExtents;
	private final ArrayList<SdfAtlas> atlases = new ArrayList<>();
	final SdfOutlines outlines;

	public Vk2dFont(
			long hbFace, Vk2dInstance instance,
			MemoryCombiner mainMemoryCombiner,
			MemoryCombiner stagingMemoryCombiner
	) {
		this.hbFace = hbFace;
		this.hbFont = assertHbSuccess(hb_font_create(hbFace), "font_create");

		int numGlyphs = hb_face_get_glyph_count(hbFace);
		this.glyphExtents = hb_glyph_extents_t.calloc(numGlyphs);
		int glyphA, glyphWhitespace;
		try (MemoryStack stack = MemoryStack.stackPush()) {
			var pGlyph = stack.callocInt(1);

			// Almost all fonts should contain the 'A' and ' ' glyphs.
			// If not, we will just pick the first glyph, which may yield the wrong font size,
			// but should at least allow us to continue.
			if (hb_font_get_nominal_glyph(hbFont, 'A', pGlyph)) glyphA = pGlyph.get(0);
			else glyphA = 0;
			if (hb_font_get_nominal_glyph(hbFont, ' ', pGlyph)) glyphWhitespace = pGlyph.get(0);
			else glyphWhitespace = glyphA;
		}

		int fontHeightA = 0;

		for (int glyphID = 0; glyphID < numGlyphs; glyphID++) {
			var extents = glyphExtents.get(glyphID);
			assertHbSuccess(hb_font_get_glyph_extents(
					hbFont, glyphID, extents
			), "font_get_glyph_extents");
			if (glyphID == glyphA) fontHeightA = abs(extents.height());
		}

		if (fontHeightA == 0) throw new RuntimeException("Cannot find the glyph extents of the 'A' glyph");
		this.fontHeightA = fontHeightA;
		this.outlines = new SdfOutlines(hbFont, glyphExtents, instance, mainMemoryCombiner, stagingMemoryCombiner);
		this.whitespaceAdvance = hb_font_get_glyph_h_advance(hbFont, glyphWhitespace);
	}

	public void addAtlas(
			Vk2dInstance instance, int bitsPerPixel, float heightA, float distanceScale,
			MemoryCombiner persistentCombiner, MemoryCombiner stagingCombiner,
			float minRenderHeightA, float maxRenderHeightA,
			float minRelativeStrokeWidth, float maxRelativeStrokeWidth, int[] supportedGlyphs
	) {
		Set<Integer> supportedGlyphsSet = null;
		if (supportedGlyphs != null) {
			supportedGlyphsSet = new HashSet<>(supportedGlyphs.length);
			for (int glyph : supportedGlyphs) supportedGlyphsSet.add(glyph);
		}

		atlases.add(new SdfAtlas(
				instance, bitsPerPixel, fontHeightA, heightA, distanceScale, glyphExtents, persistentCombiner, stagingCombiner,
				minRenderHeightA, maxRenderHeightA, minRelativeStrokeWidth, maxRelativeStrokeWidth, supportedGlyphsSet
		));
	}

	public void prepareStaging(Vk2dInstance instance, DescriptorCombiner descriptors, Bc4Compressor compressor) {
		outlines.fillBuffer();
		for (var atlas : atlases) atlas.claimDescriptors(instance, descriptors, compressor);
	}

	public SdfAtlas chooseAtlas(float heightA, float strokeWidth, int glyph) {
		// First, avoid potential rounding errors and divisions by 0
		heightA = max(heightA, 0.001f);
		float relativeStrokeWidth = max(strokeWidth / heightA, 0f);

		for (var atlas : atlases) {
			if (atlas.canRender(heightA, relativeStrokeWidth, glyph)) return atlas;
		}

		return null;
	}

	public float getGlyphAdvanceX(hb_glyph_position_t glyphOffset, float heightA) {
		return glyphOffset.x_advance() * heightA / fontHeightA;
	}

	public float getWhitespaceAdvance(float heightA) {
		return whitespaceAdvance * heightA / fontHeightA;
	}

	public float getGlyphAdvanceY(hb_glyph_position_t glyphOffset, float heightA) {
		return glyphOffset.y_advance() * heightA / fontHeightA;
	}

	public void destroy() {
		glyphExtents.free();
		hb_font_destroy(hbFont);
		hb_face_destroy(hbFace);
	}
}
