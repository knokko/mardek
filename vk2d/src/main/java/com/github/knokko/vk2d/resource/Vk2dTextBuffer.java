package com.github.knokko.vk2d.resource;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.buffers.MappedVkbBuffer;
import com.github.knokko.boiler.buffers.VkbBuffer;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.descriptors.DescriptorCombiner;
import com.github.knokko.boiler.descriptors.DescriptorUpdater;
import com.github.knokko.boiler.memory.MemoryCombiner;
import com.github.knokko.boiler.synchronization.ResourceUsage;
import com.github.knokko.vk2d.Vk2dSharedText;
import com.github.knokko.vk2d.text.GlyphCacheTracker;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

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
	private final VkbBuffer scratchIntersectionBuffer, scratchInfoBuffer;
	private final VkbBuffer intersectionBuffer, infoBuffer, nextOffsetBuffer;
	private final MappedVkbBuffer nextIntersectionIndexBuffer;

	private long scratchDescriptorSet, transferDescriptorSet, intersectionDescriptorSet;
	private boolean didInitializeNextOffsetBuffer, shouldBindScratchPipeline;

	private boolean shouldClearNextTransfer;
	private ByteBuffer scratchPushConstants;
	private long previousFontDescriptorSet;

	public Vk2dTextBuffer(
			VkbBuffer scratchIntersectionBuffer, VkbBuffer scratchInfoBuffer,
			VkbBuffer intersectionBuffer, VkbBuffer infoBuffer,
			VkbBuffer nextOffsetBuffer, MappedVkbBuffer nextIntersectionIndexBuffer
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
		this.nextIntersectionIndexBuffer = nextIntersectionIndexBuffer;
	}

	public Vk2dTextBuffer(
			BoilerInstance boiler, MemoryCombiner combiner, Vk2dSharedText shared,
			DescriptorCombiner descriptors, int numFramesInFlight
	) {
		// Scratch buffer size:
		//  target: fit glyph with 100 curves, using a height of 10k pixels
		//  the info buffer needs 2 ints (4 bytes each) per pixel height: 80k bytes
		//  the intersection buffer needs 2 * numCurves intersections per pixel height:
		//    2 * 100 curves * 4 bytes * 10k pixels = 8M bytes
		// Real buffer size:
		//  target: framesInFlight * scratchSize + desiredSize
		//  desiredSize: fit glyphs with an average of 10 intersections per row, with a total height of 100k rows
		//    the info buffer needs 2 ints per row: 800k bytes
		//    the intersection buffer needs 10 intersections * 4 bytes * 100k rows = 4M bytes
		this(
				request(boiler, combiner, 8_000_000L),
				request(boiler, combiner, 80_000L),
				request(boiler, combiner, numFramesInFlight * 8_000_000L + 4_000_000L),
				//request(boiler, combiner, 210_000L),
				request(boiler, combiner, numFramesInFlight * 80_000L + 800_000),
				request(boiler, combiner, 4L),
				combiner.addMappedBuffer(
						4L * numFramesInFlight,
						boiler.deviceProperties.limits().minStorageBufferOffsetAlignment(),
						VK_BUFFER_USAGE_STORAGE_BUFFER_BIT
				)
		);
		descriptors.addSingle(shared.scratchDescriptorLayout0, set -> this.scratchDescriptorSet = set);
		descriptors.addSingle(shared.transferDescriptorLayout, set -> this.transferDescriptorSet = set);
		descriptors.addSingle(shared.intersectionDescriptorLayout, set -> this.intersectionDescriptorSet = set);
	}

	public void initializeDescriptorSets(BoilerInstance boiler) {
		this.cache.setIntersectionIndexBuffer(nextIntersectionIndexBuffer.intBuffer());
		try (MemoryStack stack = stackPush()) {
			DescriptorUpdater updater = new DescriptorUpdater(stack, 10);
			updater.writeStorageBuffer(0, scratchDescriptorSet, 0, scratchIntersectionBuffer);
			updater.writeStorageBuffer(1, scratchDescriptorSet, 1, scratchInfoBuffer);

			updater.writeStorageBuffer(2, transferDescriptorSet, 0, scratchIntersectionBuffer);
			updater.writeStorageBuffer(3, transferDescriptorSet, 1, scratchInfoBuffer);
			updater.writeStorageBuffer(4, transferDescriptorSet, 2, intersectionBuffer);
			updater.writeStorageBuffer(5, transferDescriptorSet, 3, infoBuffer);
			updater.writeStorageBuffer(6, transferDescriptorSet, 4, nextOffsetBuffer);
			updater.writeStorageBuffer(7, transferDescriptorSet, 5, nextIntersectionIndexBuffer);

			updater.writeStorageBuffer(8, intersectionDescriptorSet, 0, intersectionBuffer);
			updater.writeStorageBuffer(9, intersectionDescriptorSet, 1, infoBuffer);

			updater.update(boiler);
		}
	}

	public void startFrame(CommandRecorder recorder) {
		shouldClearNextTransfer = cache.startFrame();
		shouldBindScratchPipeline = true;
		scratchPushConstants = recorder.stack.calloc(36);
	}

	public int scratch(
			CommandRecorder recorder, Vk2dSharedText shared,
			Vk2dFont font, int glyph, int size, boolean horizontal
	) {
		int numCurves = font.getNumCurves(glyph);
		if (size == 0 || numCurves == 0) return -1;
		Integer existing = cache.get(glyph, size, horizontal);
		if (existing != null) return existing;

		int scratchIntersectionOffset = cache.getNextScratchIntersectionIndex();
		int scratchInfoOffset = cache.putScratch(glyph, size, numCurves, horizontal);
		if (scratchInfoOffset == -1) return -1;

		if (shouldBindScratchPipeline) {
			shouldBindScratchPipeline = false;
			vkCmdBindPipeline(recorder.commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, shared.scratchPipeline);
			recorder.bindComputeDescriptors(shared.scratchPipelineLayout, scratchDescriptorSet, font.vkDescriptorSet);
			previousFontDescriptorSet = font.vkDescriptorSet;
		}

		if (previousFontDescriptorSet != font.vkDescriptorSet) {
			vkCmdBindDescriptorSets(
					recorder.commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, shared.scratchPipelineLayout,
					1, recorder.stack.longs(font.vkDescriptorSet), null
			);
			previousFontDescriptorSet = font.vkDescriptorSet;
		}

		ByteBuffer pushConstants = scratchPushConstants;
		pushConstants.putInt(0, scratchIntersectionOffset);
		pushConstants.putInt(4, scratchInfoOffset);
		pushConstants.putInt(8, 2 * font.getFirstCurve(glyph));
		pushConstants.putInt(12, numCurves);
		pushConstants.putInt(16, horizontal ? size : -size);
		pushConstants.putFloat(20, font.getGlyphMinX(glyph));
		pushConstants.putFloat(24, font.getGlyphMinY(glyph));
		pushConstants.putFloat(28, font.getGlyphMaxX(glyph));
		pushConstants.putFloat(32, font.getGlyphMaxY(glyph));

		vkCmdPushConstants(
				recorder.commandBuffer, shared.scratchPipelineLayout,
				VK_SHADER_STAGE_COMPUTE_BIT, 0, pushConstants
		);
		vkCmdDispatch(recorder.commandBuffer, size, 1, 1);

		return cache.get(glyph, size, horizontal);
	}

	private void nextIntersectionBarrier(CommandRecorder recorder) {
		ResourceUsage usage = ResourceUsage.computeBuffer(VK_ACCESS_SHADER_WRITE_BIT);
		recorder.bufferBarrier(nextIntersectionIndexBuffer, usage, usage);
	}

	public void transfer(CommandRecorder recorder, Vk2dSharedText shared) {
		vkCmdBindPipeline(recorder.commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, shared.transferPipeline);
		recorder.bindComputeDescriptors(shared.transferPipelineLayout, transferDescriptorSet);
		if (cache.getNextScratchInfoIndex() == 0) {
			IntBuffer pushConstants = recorder.stack.callocInt(3);
			pushConstants.put(2, cache.getCurrentFrameInFlight());
			vkCmdPushConstants(
					recorder.commandBuffer, shared.transferPipelineLayout,
					VK_SHADER_STAGE_COMPUTE_BIT, 0, pushConstants
			);
			vkCmdDispatch(recorder.commandBuffer, 1, 1, 1);
			nextIntersectionBarrier(recorder);
			return;
		}

		System.out.println("Propagate " + (cache.getNextScratchInfoIndex() / 2) + " rows");
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
				scratchIntersectionBuffer,
				ResourceUsage.computeBuffer(VK_ACCESS_SHADER_READ_BIT | VK_ACCESS_SHADER_WRITE_BIT),
				ResourceUsage.computeBuffer(VK_ACCESS_SHADER_READ_BIT)
		);
		recorder.bufferBarrier(
				scratchInfoBuffer,
				ResourceUsage.computeBuffer(VK_ACCESS_SHADER_WRITE_BIT),
				ResourceUsage.computeBuffer(VK_ACCESS_SHADER_READ_BIT)
		);

		IntBuffer pushConstants = recorder.stack.callocInt(3);
		pushConstants.put(0, cache.getNextStableInfoIndex());
		pushConstants.put(1, cache.getNextScratchInfoIndex() / 2);
		pushConstants.put(2, cache.getCurrentFrameInFlight());
		vkCmdPushConstants(
				recorder.commandBuffer, shared.transferPipelineLayout,
				VK_SHADER_STAGE_COMPUTE_BIT, 0, pushConstants
		);
		vkCmdDispatch(recorder.commandBuffer, 1, 1, 1);

		ResourceUsage nextOffsetUsage = ResourceUsage.computeBuffer(
				VK_ACCESS_SHADER_WRITE_BIT | VK_ACCESS_SHADER_READ_BIT
		);
		recorder.bufferBarrier(nextOffsetBuffer, nextOffsetUsage, nextOffsetUsage);
		nextIntersectionBarrier(recorder);

		recorder.bulkBufferBarrier(
				ResourceUsage.computeBuffer(VK_ACCESS_SHADER_WRITE_BIT),
				ResourceUsage.shaderRead(VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT),
				intersectionBuffer, infoBuffer
		);
	}

	public long getRenderDescriptorSet() {
		return intersectionDescriptorSet;
	}
}
