package com.github.knokko.vk2d.text;

import com.github.knokko.boiler.buffers.VkbBuffer;
import com.github.knokko.boiler.descriptors.BulkDescriptorUpdater;
import com.github.knokko.boiler.descriptors.DescriptorCombiner;
import com.github.knokko.boiler.images.ImageBuilder;
import com.github.knokko.boiler.images.VkbImage;
import com.github.knokko.boiler.memory.MemoryCombiner;
import com.github.knokko.compressor.Bc4Compressor;
import com.github.knokko.vk2d.Vk2dInstance;
import org.lwjgl.util.harfbuzz.hb_glyph_extents_t;
import org.lwjgl.util.harfbuzz.hb_glyph_position_t;

import java.util.Arrays;
import java.util.Set;

import static com.github.knokko.boiler.utilities.BoilerMath.leastCommonMultiple;
import static com.github.knokko.boiler.utilities.BoilerMath.nextMultipleOf;
import static java.lang.Math.*;
import static org.lwjgl.vulkan.VK10.*;

public class SdfAtlas {

	private static final int PADDING = 1;

	private static int roundSize(int rawSize, float scale) {
		return (int) ceil(abs(rawSize) * scale);
	}

	final int bitsPerPixel, fontHeightA;
	public final float heightA;
	final float outlineScale;
	public final float distanceScale;
	final float minHeightA, maxHeightA, minRelativeStrokeWidth, maxRelativeStrokeWidth;
	final Set<Integer> supportedGlyphs;
	final int distanceMargin;
	private final hb_glyph_extents_t.Buffer glyphExtents;
	private final int[] positions;
	final VkbImage image, dummyImage8, dummyImage16;
	final VkbBuffer uncompressedBcBuffer, compressedBcBuffer;

	private long generateDescriptorSet;
	private long renderDescriptorSet;
	private long compressDescriptorSet;

	SdfAtlas(
			Vk2dInstance instance, int bitsPerPixel, int fontHeightA, float heightA, float distanceScale,
			hb_glyph_extents_t.Buffer glyphExtents, MemoryCombiner persistentCombiner, MemoryCombiner stagingCombiner,
			float minHeightA, float maxHeightA, float minRelativeStrokeWidth, float maxRelativeStrokeWidth,
			Set<Integer> supportedGlyphs
	) {
		this.outlineScale = heightA / fontHeightA;
		this.bitsPerPixel = bitsPerPixel;
		this.fontHeightA = fontHeightA;
		this.heightA = heightA;
		this.distanceScale = distanceScale;
		this.distanceMargin = (int) ceil(1 / distanceScale);
		this.glyphExtents = glyphExtents;

		this.minHeightA = minHeightA;
		this.maxHeightA = maxHeightA;
		this.minRelativeStrokeWidth = minRelativeStrokeWidth;
		this.maxRelativeStrokeWidth = maxRelativeStrokeWidth;
		this.supportedGlyphs = supportedGlyphs;

		var glyphSizes = new GlyphSize[glyphExtents.capacity()];
		for (int glyph = 0; glyph < glyphExtents.capacity(); glyph++) {
			if (supportedGlyphs != null && !supportedGlyphs.contains(glyph)) continue;
			var extent = glyphExtents.get(glyph);
			glyphSizes[glyph] = new GlyphSize(glyph, extent.width(), extent.height());
		}
		this.positions = new int[2 * glyphSizes.length];

		int guessTotalPixels = 0;
		int largestWidth = 0;
		int largestHeight = 0;
		for (var size : glyphSizes) {
			if (size == null) continue;
			largestWidth = max(largestWidth, size.getWidth(outlineScale, distanceMargin));
			largestHeight = max(largestHeight, size.getHeight(outlineScale, distanceMargin));
			guessTotalPixels += size.getPaddedWidth(outlineScale, distanceMargin) * size.getPaddedHeight(outlineScale, distanceMargin);
		}

		int guessWidth = max(largestWidth, (int) Math.sqrt(guessTotalPixels));
		Arrays.sort(glyphSizes, (a, b) -> {
			if (a == null && b == null) return 0;
			if (b == null) return 1;
			if (a == null) return -1;
			return a.compareTo(b);
		});

		int currentX = PADDING;
		int currentY = PADDING;
		int requiredWidth = 0;
		int requiredHeight = 0;

		Arrays.fill(positions, -1);
		for (var size : glyphSizes) {
			if (size == null || size.rawWidth == 0 || size.rawHeight == 0) continue;

			if (currentX + size.getWidth(outlineScale, distanceMargin) > guessWidth) {
				currentX = PADDING;
				currentY += size.getPaddedHeight(outlineScale, distanceMargin);
			}

			positions[2 * size.glyph] = currentX;
			positions[2 * size.glyph + 1] = currentY;
			requiredWidth = max(requiredWidth, currentX + size.getWidth(outlineScale, distanceMargin));
			requiredHeight = max(requiredHeight, currentY + size.getHeight(outlineScale, distanceMargin));

			currentX += size.getPaddedWidth(outlineScale, distanceMargin);
		}

		requiredWidth += PADDING;
		requiredHeight += PADDING;

		if (bitsPerPixel == 4) {
			requiredWidth = nextMultipleOf(requiredWidth, 4);
			requiredHeight = nextMultipleOf(requiredHeight, 4);
		}

		int imageFormat;
		if (bitsPerPixel == 4) imageFormat = VK_FORMAT_BC4_SNORM_BLOCK;
		else if (bitsPerPixel == 8) imageFormat = VK_FORMAT_R8_SNORM;
		else if (bitsPerPixel == 16) imageFormat = VK_FORMAT_R16_SNORM;
		else throw new IllegalArgumentException("bits per pixel must be 4, 8, or 16, but got " + bitsPerPixel);

		float priority = 0.5f;
		int mainImageUsage = VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT;
		if (bitsPerPixel != 4) mainImageUsage |= VK_IMAGE_USAGE_STORAGE_BIT;

		this.image = persistentCombiner.addImage(new ImageBuilder(
				"Vk2dSdfAtlas", requiredWidth, requiredHeight
		).format(imageFormat).setUsage(mainImageUsage), priority);

		if (bitsPerPixel == 4) {
			this.uncompressedBcBuffer = stagingCombiner.addBuffer(
					(long) requiredWidth * requiredHeight,
					leastCommonMultiple(4, instance.boiler.deviceProperties.limits().minStorageBufferOffsetAlignment()),
					VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, 0.25f
			);
			this.compressedBcBuffer = stagingCombiner.addBuffer(
					(long) requiredWidth * requiredHeight / 2L,
					leastCommonMultiple(8, instance.boiler.deviceProperties.limits().minStorageBufferOffsetAlignment()),
					VK_BUFFER_USAGE_TRANSFER_SRC_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, 0.25f
			);
		} else {
			this.uncompressedBcBuffer = null;
			this.compressedBcBuffer = null;
		}

		if (bitsPerPixel != 8) {
			int imageUsage = VK_IMAGE_USAGE_STORAGE_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT;
			int width = 1;
			int height = 1;
			if (bitsPerPixel == 4) {
				imageUsage |= VK_IMAGE_USAGE_TRANSFER_SRC_BIT;
				width = requiredWidth;
				height = requiredHeight;
			}
			this.dummyImage8 = stagingCombiner.addImage(new ImageBuilder(
					"Vk2dSdfDummyImage8", width, height
			).format(VK_FORMAT_R8_SNORM).setUsage(imageUsage), priority);
		} else this.dummyImage8 = null;

		if (bitsPerPixel != 16) {
			this.dummyImage16 = stagingCombiner.addImage(new ImageBuilder(
					"Vk2dSdfDummyImage16", 1, 1
			).format(VK_FORMAT_R16_SNORM).setUsage(
					VK_IMAGE_USAGE_STORAGE_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT
			), priority);
		} else {
			this.dummyImage16 = null;
		}
	}

	boolean canRender(float heightA, float relativeStrokeWidth, int glyph) {
		if (heightA < this.minHeightA || heightA > this.maxHeightA) return false;
		if (relativeStrokeWidth < this.minRelativeStrokeWidth || relativeStrokeWidth > this.maxRelativeStrokeWidth) {
			return false;
		}
		if (this.supportedGlyphs != null && !this.supportedGlyphs.contains(glyph)) return false;
		return true;
	}

	void claimDescriptors(Vk2dInstance instance, DescriptorCombiner descriptors, Bc4Compressor compressor) {
		descriptors.addSingle(
				instance.sdfGenerateDescriptorLayout,
				descriptorSet -> this.generateDescriptorSet = descriptorSet
		);
		descriptors.addSingle(
				instance.imageDescriptorSetLayout,
				descriptorSet -> this.renderDescriptorSet = descriptorSet
		);
		if (bitsPerPixel == 4) {
			descriptors.addSingle(
					compressor.descriptorSetLayout,
					descriptorSet -> this.compressDescriptorSet = descriptorSet
			);
		}
	}

	long getGenerateDescriptorSet() {
		return generateDescriptorSet;
	}

	long getCompressDescriptorSet() {
		return compressDescriptorSet;
	}

	public long getRenderDescriptorSet() {
		return renderDescriptorSet;
	}

	public int getMinX(int glyph) {
		return positions[2 * glyph];
	}

	public int getMinY(int glyph) {
		return positions[2 * glyph + 1];
	}

	int getWidth(int glyph) {
		return roundSize(glyphExtents.get(glyph).width(), outlineScale) + 2 * distanceMargin;
	}

	int getHeight(int glyph) {
		return roundSize(glyphExtents.get(glyph).height(), outlineScale) + 2 * distanceMargin;
	}

	public float getRenderMinX(int glyph, float baseX, hb_glyph_position_t glyphOffset, float heightA) {
		var extents = glyphExtents.get(glyph);
		int unscaledOffsetX = glyphOffset.x_offset() + extents.x_bearing();
		float desiredMinX = baseX + heightA * unscaledOffsetX / fontHeightA;
		float desiredWidth = heightA * extents.width() / fontHeightA;
		float desiredAtlasWidth = abs(glyphExtents.get(glyph).width()) * outlineScale;
		float atlasWidthWithMargin = desiredAtlasWidth + 2 * distanceMargin;
		float widthWithMargin = desiredWidth * atlasWidthWithMargin / desiredAtlasWidth;
		float leftMargin = 0.5f * (widthWithMargin - desiredWidth);
		return desiredMinX - leftMargin;
	}

	public float getRenderMinY(int glyph, float baseY, hb_glyph_position_t glyphOffset, float heightA) {
		var extents = glyphExtents.get(glyph);
		int unscaledOffsetY = glyphOffset.y_offset() - glyphExtents.get(glyph).y_bearing();
		float desiredMinY = baseY + heightA * unscaledOffsetY / fontHeightA;
		float desiredHeight = heightA * abs(extents.height()) / fontHeightA;
		float desiredAtlasHeight = abs(glyphExtents.get(glyph).height()) * outlineScale;
		float atlasHeightWithMargin = desiredAtlasHeight + 2 * distanceMargin;
		float heightsWithMargin = desiredHeight * atlasHeightWithMargin / desiredAtlasHeight;
		float topMargin = 0.5f * (heightsWithMargin - desiredHeight);
		return desiredMinY - topMargin;
	}

	public float getRenderMaxX(int glyph, float baseX, hb_glyph_position_t glyphOffset, float heightA) {
		var extents = glyphExtents.get(glyph);
		int unscaledOffsetX = glyphOffset.x_offset() + extents.x_bearing() + extents.width();
		float desiredMaxX = baseX + heightA * unscaledOffsetX / fontHeightA;
		float desiredWidth = heightA * extents.width() / fontHeightA;
		float desiredAtlasWidth = abs(glyphExtents.get(glyph).width()) * outlineScale;
		float atlasWidthWithMargin = desiredAtlasWidth + 2 * distanceMargin;
		float widthWithMargin = desiredWidth * atlasWidthWithMargin / desiredAtlasWidth;
		float rightMargin = 0.5f * (widthWithMargin - desiredWidth);
		return desiredMaxX + rightMargin;
	}

	public float getRenderMaxY(int glyph, float baseY, hb_glyph_position_t glyphOffset, float heightA) {
		var extents = glyphExtents.get(glyph);
		int unscaledOffsetY = glyphOffset.y_offset() - extents.y_bearing() - extents.height();
		float desiredMaxY = baseY + heightA * unscaledOffsetY / fontHeightA;
		float desiredHeight = heightA * abs(extents.height()) / fontHeightA;
		float desiredAtlasHeight = abs(glyphExtents.get(glyph).height()) * outlineScale;
		float atlasHeightWithMargin = desiredAtlasHeight + 2 * distanceMargin;
		float heightsWithMargin = desiredHeight * atlasHeightWithMargin / desiredAtlasHeight;
		float bottomMargin = 0.5f * (heightsWithMargin - desiredHeight);
		return desiredMaxY + bottomMargin;
	}

	public float getMinU(int glyph) {
		return getMinX(glyph) / (float) image.width;
	}

	public float getMinV(int glyph) {
		return getMinY(glyph) / (float) image.height;
	}

	public float getMaxU(int glyph) {
		return (getMinX(glyph) + abs(glyphExtents.get(glyph).width()) * outlineScale + 2 * distanceMargin) / image.width;
	}

	public float getMaxV(int glyph) {
		return (getMinY(glyph) + abs(glyphExtents.get(glyph).height()) * outlineScale + 2 * distanceMargin) / image.height;
	}

	public int getWidth() {
		return image.width;
	}

	public int getHeight() {
		return image.height;
	}

	void updateDescriptors(BulkDescriptorUpdater updater, Vk2dInstance instance, SdfOutlines outlines) {
		updater.writeStorageBuffer(generateDescriptorSet, 0, outlines.persistentBuffer);
		updater.writeImage(
				generateDescriptorSet, 1, bitsPerPixel == 8 ? image.vkImageView : dummyImage8.vkImageView,
				VK_NULL_HANDLE, VK_DESCRIPTOR_TYPE_STORAGE_IMAGE, VK_IMAGE_LAYOUT_GENERAL
		);
		updater.writeImage(
				generateDescriptorSet, 2, bitsPerPixel == 16 ? image.vkImageView : dummyImage16.vkImageView,
				VK_NULL_HANDLE, VK_DESCRIPTOR_TYPE_STORAGE_IMAGE, VK_IMAGE_LAYOUT_GENERAL
		);
		updater.writeImage(renderDescriptorSet, 0, image.vkImageView, instance.smoothSampler);
	}

	private record GlyphSize(int glyph, int rawWidth, int rawHeight) implements Comparable<GlyphSize> {

		@Override
		public int compareTo(GlyphSize other) {
			if (abs(rawHeight) < abs(other.rawHeight)) return -1;
			if (abs(rawHeight) > abs(other.rawHeight)) return 1;
			if (abs(rawWidth) < abs(other.rawWidth)) return -1;
			if (abs(rawWidth) > abs(other.rawWidth)) return 1;
			return Integer.compare(glyph, other.glyph);
		}

		int getWidth(float scale, int margin) {
			return roundSize(rawWidth, scale) + 2 * margin;
		}

		int getHeight(float scale, int margin) {
			return roundSize(rawHeight, scale) + 2 * margin;
		}

		int getPaddedWidth(float scale, int margin) {
			return getWidth(scale, margin) + PADDING;
		}

		int getPaddedHeight(float scale, int margin) {
			return getHeight(scale, margin) + PADDING;
		}
	}
}
