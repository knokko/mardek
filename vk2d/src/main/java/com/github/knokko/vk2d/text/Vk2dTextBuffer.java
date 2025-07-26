package com.github.knokko.vk2d.text;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.buffers.MappedVkbBuffer;
import com.github.knokko.boiler.buffers.VkbBuffer;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.descriptors.DescriptorCombiner;
import com.github.knokko.boiler.descriptors.DescriptorUpdater;
import com.github.knokko.boiler.memory.MemoryCombiner;
import com.github.knokko.boiler.synchronization.ResourceUsage;
import com.github.knokko.vk2d.Vk2dInstance;
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
	private final Vk2dInstance instance;
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
			VkbBuffer nextOffsetBuffer, MappedVkbBuffer nextIntersectionIndexBuffer,
			Vk2dInstance instance, DescriptorCombiner descriptors
	) {
		this.instance = instance;
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
		descriptors.addSingle(instance.textScratchDescriptorLayout0, set -> this.scratchDescriptorSet = set);
		descriptors.addSingle(instance.textTransferDescriptorLayout, set -> this.transferDescriptorSet = set);
		descriptors.addSingle(instance.textIntersectionDescriptorLayout, set -> this.intersectionDescriptorSet = set);
	}

	public Vk2dTextBuffer(
			Vk2dInstance instance, MemoryCombiner combiner,
			DescriptorCombiner descriptors, int numFramesInFlight
	) {
		// Scratch buffer size:
		//  target 1: fit glyph with 100 curves, using a height and width of 10k pixels
		//    the info buffer needs 2 ints (4 bytes each) per pixel height and width: 160k bytes
		//    the intersection buffer needs 2 * numCurves intersections per pixel height and width:
		//      2 * 100 curves * 4 bytes * (10k rows + 10k columns) = 16M bytes
		// Real buffer size:
		//  target: fit glyphs with an average of 10 intersections per row and column, with a total height of 100k rows
		//    the info buffer needs 2 ints per row and column: 1600k bytes
		//    the intersection buffer needs 10 intersections * 4 bytes * (100k rows + 100k columns) = 8M bytes
		this(
				request(instance.boiler, combiner, 16_000_000L),
				request(instance.boiler, combiner, 160_000L),
				request(instance.boiler, combiner, 8_000_000L),
				request(instance.boiler, combiner, 1_600_000),
				request(instance.boiler, combiner, 4L),
				combiner.addMappedBuffer(
						4L * numFramesInFlight,
						instance.boiler.deviceProperties.limits().minStorageBufferOffsetAlignment(),
						VK_BUFFER_USAGE_STORAGE_BUFFER_BIT
				),
				instance,
				descriptors
		);
	}

	public void initializeDescriptorSets() {
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

			updater.update(instance.boiler);
		}
	}

	public void startFrame() {
		shouldClearNextTransfer = cache.startFrame();
		shouldBindScratchPipeline = true;
		scratchPushConstants = null;
	}

	public int scratch(CommandRecorder recorder, Vk2dFont font, int glyph, float offset, float size, int intSize, boolean horizontal) {
		int numCurves = font.getNumCurves(glyph);
		if (intSize <= 0 || numCurves == 0) return -1;
		Integer existing = cache.get(font.index, glyph, offset, size, intSize, horizontal);
		if (existing != null) return existing;

		int scratchIntersectionOffset = cache.getNextScratchIntersectionIndex();
		int scratchInfoOffset = cache.putScratch(font.index, glyph, offset, size, intSize, numCurves, horizontal);
		if (scratchInfoOffset == -1) return -1;

		if (shouldBindScratchPipeline) {
			shouldBindScratchPipeline = false;
			vkCmdBindPipeline(recorder.commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, instance.textScratchPipeline);
			recorder.bindComputeDescriptors(instance.textScratchPipelineLayout, scratchDescriptorSet, font.vkDescriptorSet);
			previousFontDescriptorSet = font.vkDescriptorSet;
		}

		if (previousFontDescriptorSet != font.vkDescriptorSet) {
			vkCmdBindDescriptorSets(
					recorder.commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, instance.textScratchPipelineLayout,
					1, recorder.stack.longs(font.vkDescriptorSet), null
			);
			previousFontDescriptorSet = font.vkDescriptorSet;
			throw new UnsupportedOperationException("TODO");
		}

		if (scratchPushConstants == null) scratchPushConstants = recorder.stack.calloc(40);
		ByteBuffer pushConstants = scratchPushConstants;
		pushConstants.putInt(0, scratchIntersectionOffset);
		pushConstants.putInt(4, scratchInfoOffset);
		pushConstants.putInt(8, 2 * font.getFirstCurve(glyph));
		pushConstants.putInt(12, numCurves);
		pushConstants.putFloat(16, horizontal ? size : -size);
		pushConstants.putFloat(20, font.getGlyphMinX(glyph));
		pushConstants.putFloat(24, font.getGlyphMinY(glyph));
		pushConstants.putFloat(28, font.getGlyphMaxX(glyph));
		pushConstants.putFloat(32, font.getGlyphMaxY(glyph));
		pushConstants.putFloat(36, offset);

		vkCmdPushConstants(
				recorder.commandBuffer, instance.textScratchPipelineLayout,
				VK_SHADER_STAGE_COMPUTE_BIT, 0, pushConstants
		);
		vkCmdDispatch(recorder.commandBuffer, intSize, 1, 1);

		return cache.get(font.index, glyph, offset, size, intSize, horizontal);
	}

	private void nextIntersectionBarrier(CommandRecorder recorder) {
		ResourceUsage usage = ResourceUsage.computeBuffer(VK_ACCESS_SHADER_WRITE_BIT);
		recorder.bufferBarrier(nextIntersectionIndexBuffer, usage, usage);
	}

	public void transfer(CommandRecorder recorder) {
		vkCmdBindPipeline(recorder.commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, instance.textTransferPipeline);
		recorder.bindComputeDescriptors(instance.textTransferPipelineLayout, transferDescriptorSet);
		if (cache.getNextScratchInfoIndex() == 0) {
			IntBuffer pushConstants = recorder.stack.callocInt(4);
			pushConstants.put(2, cache.getCurrentFrameInFlight());
			vkCmdPushConstants(
					recorder.commandBuffer, instance.textTransferPipelineLayout,
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

		IntBuffer pushConstants = recorder.stack.callocInt(4);
		pushConstants.put(0, cache.getNextStableInfoIndex());
		pushConstants.put(1, cache.getNextScratchInfoIndex() / 2);
		pushConstants.put(2, cache.getCurrentFrameInFlight());
		pushConstants.put(3, Math.toIntExact(intersectionBuffer.size / 4L));
		vkCmdPushConstants(
				recorder.commandBuffer, instance.textTransferPipelineLayout,
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
