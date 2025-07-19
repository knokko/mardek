package com.github.knokko.vk2d.resource;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.buffers.VkbBuffer;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.descriptors.DescriptorCombiner;
import com.github.knokko.boiler.descriptors.DescriptorUpdater;
import com.github.knokko.boiler.memory.MemoryCombiner;
import com.github.knokko.boiler.synchronization.ResourceUsage;
import com.github.knokko.vk2d.Vk2dSharedText;
import com.github.knokko.vk2d.text.GlyphCacheTracker;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkBufferMemoryBarrier;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class Vk2dTextBuffer {

	private static VkbBuffer request(BoilerInstance boiler, MemoryCombiner combiner, long size) {
		int usage = VK_BUFFER_USAGE_STORAGE_BUFFER_BIT;
		if (size == 4L) usage |= VK_BUFFER_USAGE_TRANSFER_DST_BIT;
		return combiner.addBuffer(
				size, boiler.deviceProperties.limits().minStorageBufferOffsetAlignment(), usage, 0.75f
		);
	}

	private final GlyphCacheTracker cache;
	private Vk2dFont font;
	private final VkbBuffer scratchIntersectionBuffer, scratchInfoBuffer;
	private final VkbBuffer intersectionBuffer, infoBuffer, nextOffsetBuffer;

	private long scratchDescriptorSet, transferDescriptorSet, intersectionDescriptorSet;
	private boolean didInitializeNextOffsetBuffer, shouldBindScratchPipeline;

	private final List<PreparedGlyph> preparedGlyphs = new ArrayList<>();

	private boolean shouldClearNextTransfer;

	public Vk2dTextBuffer(
			VkbBuffer scratchIntersectionBuffer, VkbBuffer scratchInfoBuffer,
			VkbBuffer intersectionBuffer, VkbBuffer infoBuffer, VkbBuffer nextOffsetBuffer
	) {
		this.scratchIntersectionBuffer = scratchIntersectionBuffer;
		this.scratchInfoBuffer = scratchInfoBuffer;
		this.intersectionBuffer = intersectionBuffer;
		this.infoBuffer = infoBuffer;
		this.nextOffsetBuffer = nextOffsetBuffer;
		this.cache = new GlyphCacheTracker(
				scratchIntersectionBuffer.size, scratchInfoBuffer.size,
				intersectionBuffer.size, infoBuffer.size
		);
	}

	public Vk2dTextBuffer(BoilerInstance boiler, MemoryCombiner combiner, int numFramesInFlight) {
		// Scratch buffer size:
		//  target: fit glyph with 100 curves, using a height of 10k pixels
		//  the info buffer needs 1 int (4 bytes) per pixel height: 40k bytes
		//  the intersection buffer needs 2 * numCurves intersections per pixel height:
		//    2 * 100 curves * 4 bytes * 10k pixels = 8M bytes
		// Real buffer size:
		//  target: framesInFlight * scratchSize + desiredSize
		//  desiredSize: fit glyphs with an average of 10 intersections per row, with a total height of 100k rows
		//    the info buffer needs 2 ints per row: 800k bytes
		//    the intersection buffer needs 10 intersections * 4 bytes * 100k rows = 4M bytes
		this(
				request(boiler, combiner, 8_000_000L),
				request(boiler, combiner, 40_000L),
				request(boiler, combiner, numFramesInFlight * 8_000_000L + 4_000_000L),
				request(boiler, combiner, numFramesInFlight * 40_000L + 800_000),
				request(boiler, combiner, 4L)
		);
	}

	public void requestDescriptorSets(Vk2dSharedText shared, DescriptorCombiner descriptors) {
		descriptors.addSingle(shared.scratchDescriptorLayout, set -> this.scratchDescriptorSet = set);
		descriptors.addSingle(shared.transferDescriptorLayout, set -> this.transferDescriptorSet = set);
		descriptors.addSingle(shared.intersectionDescriptorLayout, set -> this.intersectionDescriptorSet = set);
	}

	public void initializeDescriptorSets(BoilerInstance boiler, Vk2dFont font) {
		this.font = font;
		try (MemoryStack stack = stackPush()) {
			DescriptorUpdater updater = new DescriptorUpdater(stack, 10);
			updater.writeStorageBuffer(0, scratchDescriptorSet, 0, scratchIntersectionBuffer);
			updater.writeStorageBuffer(1, scratchDescriptorSet, 1, scratchInfoBuffer);
			updater.writeStorageBuffer(2, scratchDescriptorSet, 2, font.curveBuffer);

			updater.writeStorageBuffer(3, transferDescriptorSet, 0, scratchIntersectionBuffer);
			updater.writeStorageBuffer(4, transferDescriptorSet, 1, scratchInfoBuffer);
			updater.writeStorageBuffer(5, transferDescriptorSet, 2, intersectionBuffer);
			updater.writeStorageBuffer(6, transferDescriptorSet, 3, infoBuffer);
			updater.writeStorageBuffer(7, transferDescriptorSet, 4, nextOffsetBuffer);

			updater.writeStorageBuffer(8, intersectionDescriptorSet, 0, intersectionBuffer);
			updater.writeStorageBuffer(9, intersectionDescriptorSet, 1, infoBuffer);

			updater.update(boiler);
		}
	}

	public void startFrame() {
		preparedGlyphs.clear();
		shouldClearNextTransfer = cache.startFrame();
		shouldBindScratchPipeline = true;
	}

	public int scratch(CommandRecorder recorder, Vk2dSharedText shared, int glyph, int height) {
		if (font.getNumCurves(glyph) == 0) return -1;
		Integer existing = cache.get(glyph, height);
		if (existing != null) return existing;

		int scratchIntersectionOffset = cache.getNextScratchIntersectionIndex();
		int scratchInfoOffset = cache.putScratch(glyph, height, font.getNumCurves(glyph));
		if (scratchInfoOffset == -1) return -1;

		if (shouldBindScratchPipeline) {
			shouldBindScratchPipeline = false;
			vkCmdBindPipeline(recorder.commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, shared.scratchPipeline);
			recorder.bindComputeDescriptors(shared.scratchPipelineLayout, scratchDescriptorSet);
		}

		ByteBuffer pushConstants = recorder.stack.calloc(36);
		pushConstants.putInt(0, scratchIntersectionOffset);
		pushConstants.putInt(4, scratchInfoOffset);
		pushConstants.putInt(8, 2 * font.getFirstCurve(glyph));
		pushConstants.putInt(12, font.getNumCurves(glyph));
		pushConstants.putInt(16, height);
		pushConstants.putFloat(20, font.getGlyphMinX(glyph));
		pushConstants.putFloat(24, font.getGlyphMinY(glyph));
		pushConstants.putFloat(28, font.getGlyphMaxX(glyph));
		pushConstants.putFloat(32, font.getGlyphMaxY(glyph));

		vkCmdPushConstants(
				recorder.commandBuffer, shared.scratchPipelineLayout,
				VK_SHADER_STAGE_COMPUTE_BIT, 0, pushConstants
		);
		//noinspection SuspiciousNameCombination
		vkCmdDispatch(recorder.commandBuffer, height, 1, 1);

		PreparedGlyph next = new PreparedGlyph();
		next.glyph = glyph;
		next.scratchIntersectionOffset = scratchIntersectionOffset;
		next.scratchInfoOffset = scratchInfoOffset;
		next.height = height;
		next.numGlyphCurves = font.getNumCurves(glyph);
		preparedGlyphs.add(next);

		return scratchInfoOffset;
	}

	public void transfer(CommandRecorder recorder, Vk2dSharedText shared) {
		if (preparedGlyphs.isEmpty()) return;

		if (shouldClearNextTransfer || !didInitializeNextOffsetBuffer) {
			if (didInitializeNextOffsetBuffer) {
				recorder.bufferBarrier(
						nextOffsetBuffer,
						ResourceUsage.computeBuffer(VK_ACCESS_SHADER_READ_BIT | VK_ACCESS_SHADER_WRITE_BIT),
						ResourceUsage.TRANSFER_DEST
				);
			}
			vkCmdFillBuffer(
					recorder.commandBuffer, nextOffsetBuffer.vkBuffer,
					nextOffsetBuffer.offset, nextOffsetBuffer.size, 0
			);
			recorder.bufferBarrier(
					nextOffsetBuffer, ResourceUsage.TRANSFER_DEST,
					ResourceUsage.computeBuffer(VK_ACCESS_SHADER_READ_BIT | VK_ACCESS_SHADER_WRITE_BIT)
			);
			didInitializeNextOffsetBuffer = true;
		}

		recorder.bufferBarrier(
				scratchIntersectionBuffer.child(0L, 4L * cache.getNextScratchIntersectionIndex()),
				ResourceUsage.computeBuffer(VK_ACCESS_SHADER_READ_BIT | VK_ACCESS_SHADER_WRITE_BIT),
				ResourceUsage.computeBuffer(VK_ACCESS_SHADER_READ_BIT)
		);
		recorder.bufferBarrier(
				scratchInfoBuffer.child(0L, 4L * cache.getNextScratchInfoIndex()),
				ResourceUsage.computeBuffer(VK_ACCESS_SHADER_WRITE_BIT),
				ResourceUsage.computeBuffer(VK_ACCESS_SHADER_READ_BIT)
		);
		vkCmdBindPipeline(recorder.commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, shared.transferPipeline);
		recorder.bindComputeDescriptors(shared.transferPipelineLayout, transferDescriptorSet);

		var nextOffsetUsage = ResourceUsage.computeBuffer(VK_ACCESS_SHADER_READ_BIT | VK_ACCESS_SHADER_WRITE_BIT);
		var nextOffsetBarrier = VkBufferMemoryBarrier.calloc(1, recorder.stack);
		nextOffsetBarrier.sType$Default();
		nextOffsetBarrier.srcAccessMask(nextOffsetUsage.accessMask());
		nextOffsetBarrier.dstAccessMask(nextOffsetUsage.accessMask());
		nextOffsetBarrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
		nextOffsetBarrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
		nextOffsetBarrier.buffer(nextOffsetBuffer.vkBuffer);
		nextOffsetBarrier.offset(nextOffsetBuffer.offset);
		nextOffsetBarrier.size(nextOffsetBuffer.size);

		IntBuffer pushConstants = recorder.stack.callocInt(5);
		for (PreparedGlyph prepared : preparedGlyphs) {
			pushConstants.put(0, prepared.scratchIntersectionOffset);
			pushConstants.put(1, prepared.scratchInfoOffset);
			pushConstants.put(2, 2 * cache.get(prepared.glyph, prepared.height));
			pushConstants.put(3, prepared.height);
			pushConstants.put(4, prepared.numGlyphCurves);
			vkCmdPushConstants(
					recorder.commandBuffer, shared.transferPipelineLayout,
					VK_SHADER_STAGE_COMPUTE_BIT, 0, pushConstants
			);
			vkCmdDispatch(recorder.commandBuffer, 1, 1, 1);
			if (prepared != preparedGlyphs.get(preparedGlyphs.size() - 1)) {
				vkCmdPipelineBarrier(
						recorder.commandBuffer, nextOffsetUsage.stageMask(), nextOffsetUsage.stageMask(),
						0, null, nextOffsetBarrier, null
				);
			}
		}

		recorder.bulkBufferBarrier(
				ResourceUsage.computeBuffer(VK_ACCESS_SHADER_WRITE_BIT),
				ResourceUsage.shaderRead(VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT),
				intersectionBuffer, infoBuffer.child(
						4L * cache.getNextStableIndex(),
						8L * cache.getNextScratchInfoIndex()
				)
		);
	}

	public long getRenderDescriptorSet() {
		return intersectionDescriptorSet;
	}

	private static class PreparedGlyph {

		int glyph, scratchIntersectionOffset, scratchInfoOffset, height, numGlyphCurves;
	}
}
