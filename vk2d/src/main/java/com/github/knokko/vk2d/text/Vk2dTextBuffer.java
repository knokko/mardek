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
import com.github.knokko.vk2d.frame.Vk2dComputeStage;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static com.github.knokko.boiler.utilities.BoilerMath.nextMultipleOf;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class Vk2dTextBuffer extends Vk2dComputeStage {

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
	private boolean didInitializeNextOffsetBuffer;

	private final List<ScratchJob> scratchJobs = new ArrayList<>();

	private boolean shouldStartFrame, shouldClearNextTransfer;

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
		descriptors.addSingle(instance.doubleComputeBufferDescriptorLayout, set -> this.scratchDescriptorSet = set);
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

	public int scratch(
			Vk2dFont font, int glyph, float offset,
			float heightA, float size, int intSize, boolean horizontal, float strokeWidth
	) {
		if (shouldStartFrame) {
			shouldStartFrame = false;
			shouldClearNextTransfer = cache.startFrame();
		}

		int numCurves = font.getNumCurves(glyph);
		if (intSize <= 0 || numCurves == 0) return -1;

		float maxOrthogonalDistance = 0.5f * strokeWidth / heightA;

		// Round to multiples of 0.001 to reduce the number of distinct keys in the glyph cache
		maxOrthogonalDistance = Math.round(maxOrthogonalDistance * 1000f) * 0.001f;

		Integer existing = cache.get(font.index, glyph, offset, size, intSize, horizontal, maxOrthogonalDistance);
		if (existing != null) return existing;

		int scratchIntersectionOffset = cache.getNextScratchIntersectionIndex();
		int scratchInfoOffset = cache.putScratch(
				font.index, glyph, offset, size, intSize, numCurves, horizontal, maxOrthogonalDistance
		);
		if (scratchInfoOffset == -1) return -1;

		scratchJobs.add(new ScratchJob(
				font,
				glyph,
				scratchIntersectionOffset,
				scratchInfoOffset,
				horizontal,
				size,
				intSize,
				offset,
				maxOrthogonalDistance
		));

		return cache.get(font.index, glyph, offset, size, intSize, horizontal, maxOrthogonalDistance);
	}

	private void preIntersectionInfoBarrier(CommandRecorder recorder) {
		ResourceUsage computeUsage = ResourceUsage.computeBuffer(VK_ACCESS_SHADER_WRITE_BIT);
		recorder.bufferBarrier(nextIntersectionIndexBuffer, computeUsage, computeUsage);
	}

	private void postIntersectionInfoBarrier(CommandRecorder recorder) {
		ResourceUsage computeUsage = ResourceUsage.computeBuffer(VK_ACCESS_SHADER_WRITE_BIT);
		recorder.bufferBarrier(nextIntersectionIndexBuffer, computeUsage, ResourceUsage.HOST_READ);
	}

	public long getRenderDescriptorSet() {
		return intersectionDescriptorSet;
	}

	@Override
	public void record(CommandRecorder recorder) {
		if (!scratchJobs.isEmpty()) {
			vkCmdBindPipeline(recorder.commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, instance.textScratchPipeline);
		}

		long lastFontDescriptorSet = VK_NULL_HANDLE;

		ByteBuffer scratchPushConstants = recorder.stack.calloc(48);
		for (ScratchJob job : scratchJobs) {
			if (job.font.vkDescriptorSet != lastFontDescriptorSet) {
				lastFontDescriptorSet = job.font.vkDescriptorSet;
				recorder.bindComputeDescriptors(
						instance.textScratchPipelineLayout,
						scratchDescriptorSet, job.font.vkDescriptorSet
				);
			}
			scratchPushConstants.putInt(0, job.intersectionDataOffset);
			scratchPushConstants.putInt(4, job.intersectionInfoOffset);
			scratchPushConstants.putInt(8, 2 * job.font.getFirstCurve(job.glyph));
			scratchPushConstants.putInt(12, job.font.getNumCurves(job.glyph));
			scratchPushConstants.putFloat(16, job.horizontal ? job.size : -job.size);
			scratchPushConstants.putFloat(20, job.font.getGlyphMinX(job.glyph));
			scratchPushConstants.putFloat(24, job.font.getGlyphMinY(job.glyph));
			scratchPushConstants.putFloat(28, job.font.getGlyphMaxX(job.glyph));
			scratchPushConstants.putFloat(32, job.font.getGlyphMaxY(job.glyph));
			scratchPushConstants.putFloat(36, job.subpixelOffset);
			scratchPushConstants.putFloat(40, job.maxOrthogonalDistance);
			scratchPushConstants.putInt(44, job.intSize);

			vkCmdPushConstants(
					recorder.commandBuffer, instance.textScratchPipelineLayout,
					VK_SHADER_STAGE_COMPUTE_BIT, 0, scratchPushConstants
			);
			int numGroupsX = nextMultipleOf(job.intSize, 64) / 64;
			vkCmdDispatch(recorder.commandBuffer, numGroupsX, 1, 1);
		}

		shouldStartFrame = true;
		scratchJobs.clear();

		if (cache.getNextScratchInfoIndex() == 0) {
			IntBuffer transferPushConstants = recorder.stack.callocInt(4);
			transferPushConstants.put(2, cache.getCurrentFrameInFlight());

			preIntersectionInfoBarrier(recorder);
			vkCmdBindPipeline(recorder.commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, instance.textTransferPipeline);
			recorder.bindComputeDescriptors(instance.textTransferPipelineLayout, transferDescriptorSet);
			vkCmdPushConstants(
					recorder.commandBuffer, instance.textTransferPipelineLayout,
					VK_SHADER_STAGE_COMPUTE_BIT, 0, transferPushConstants
			);
			vkCmdDispatch(recorder.commandBuffer, 1, 1, 1);
			postIntersectionInfoBarrier(recorder);
			return;
		}

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

		IntBuffer transferPushConstants = recorder.stack.callocInt(4);
		transferPushConstants.put(0, cache.getNextStableInfoIndex());
		transferPushConstants.put(1, cache.getNextScratchInfoIndex() / 2);
		transferPushConstants.put(2, cache.getCurrentFrameInFlight());
		transferPushConstants.put(3, Math.toIntExact(intersectionBuffer.size / 4L));

		preIntersectionInfoBarrier(recorder);
		vkCmdBindPipeline(recorder.commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, instance.textTransferPipeline);
		recorder.bindComputeDescriptors(instance.textTransferPipelineLayout, transferDescriptorSet);
		vkCmdPushConstants(
				recorder.commandBuffer, instance.textTransferPipelineLayout,
				VK_SHADER_STAGE_COMPUTE_BIT, 0, transferPushConstants
		);
		vkCmdDispatch(recorder.commandBuffer, 1, 1, 1);

		ResourceUsage nextOffsetUsage = ResourceUsage.computeBuffer(
				VK_ACCESS_SHADER_WRITE_BIT | VK_ACCESS_SHADER_READ_BIT
		);
		recorder.bufferBarrier(nextOffsetBuffer, nextOffsetUsage, nextOffsetUsage);
		postIntersectionInfoBarrier(recorder);

		recorder.bulkBufferBarrier(
				ResourceUsage.computeBuffer(VK_ACCESS_SHADER_WRITE_BIT),
				ResourceUsage.shaderRead(VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT),
				intersectionBuffer, infoBuffer
		);
	}

	private record ScratchJob(
			Vk2dFont font,
			int glyph,
			int intersectionDataOffset,
			int intersectionInfoOffset,
			boolean horizontal,
			float size,
			int intSize,
			float subpixelOffset,
			float maxOrthogonalDistance
	) {}
}
