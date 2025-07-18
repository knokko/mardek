package com.github.knokko.vk2d.resource;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.buffers.VkbBuffer;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.descriptors.DescriptorCombiner;
import com.github.knokko.boiler.descriptors.DescriptorUpdater;
import com.github.knokko.boiler.memory.MemoryCombiner;
import com.github.knokko.boiler.synchronization.ResourceUsage;
import com.github.knokko.vk2d.Vk2dSharedText;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class Vk2dTextBuffer {

	private static VkbBuffer request(BoilerInstance boiler, MemoryCombiner combiner, long size) {
		return combiner.addBuffer(
				size, boiler.deviceProperties.limits().minStorageBufferOffsetAlignment(),
				VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, 0.75f
		);
	}

	private Vk2dFont font;
	private final VkbBuffer scratchIntersectionBuffer;
	private final VkbBuffer scratchInfoBuffer;
	private final VkbBuffer intersectionBuffer;
	private final VkbBuffer infoBuffer;

	private long scratchDescriptorSet, transferDescriptorSet, intersectionDescriptorSet;

	private List<PreparedGlyph> preparedGlyphs = new ArrayList<>();

	public Vk2dTextBuffer(
			VkbBuffer scratchIntersectionBuffer,
			VkbBuffer scratchInfoBuffer, VkbBuffer intersectionBuffer, VkbBuffer infoBuffer
	) {
		this.scratchIntersectionBuffer = scratchIntersectionBuffer;
		this.scratchInfoBuffer = scratchInfoBuffer;
		this.intersectionBuffer = intersectionBuffer;
		this.infoBuffer = infoBuffer;
	}

	public Vk2dTextBuffer(BoilerInstance boiler, MemoryCombiner combiner) {
		this(
				request(boiler, combiner, 5000_000L),
				request(boiler, combiner, 500_000L),
				request(boiler, combiner, 5000_000L),
				request(boiler, combiner, 1000_000L)
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
			DescriptorUpdater updater = new DescriptorUpdater(stack, 9);
			updater.writeStorageBuffer(0, scratchDescriptorSet, 0, scratchIntersectionBuffer);
			updater.writeStorageBuffer(1, scratchDescriptorSet, 1, scratchInfoBuffer);
			updater.writeStorageBuffer(2, scratchDescriptorSet, 2, font.curveBuffer);

			updater.writeStorageBuffer(3, transferDescriptorSet, 0, scratchIntersectionBuffer);
			updater.writeStorageBuffer(4, transferDescriptorSet, 1, scratchInfoBuffer);
			updater.writeStorageBuffer(5, transferDescriptorSet, 2, intersectionBuffer);
			updater.writeStorageBuffer(6, transferDescriptorSet, 3, infoBuffer);

			updater.writeStorageBuffer(7, intersectionDescriptorSet, 0, intersectionBuffer);
			updater.writeStorageBuffer(8, intersectionDescriptorSet, 1, infoBuffer);

			updater.update(boiler);
		}
	}

	public void prepareScratch(CommandRecorder recorder, Vk2dSharedText shared) {
		vkCmdBindPipeline(recorder.commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, shared.scratchPipeline);
		recorder.bindComputeDescriptors(shared.scratchPipelineLayout, scratchDescriptorSet);
		preparedGlyphs.clear();
	}

	public void scratch(CommandRecorder recorder, Vk2dSharedText shared, int glyph, int height) {
		int scratchIntersectionOffset, scratchInfoOffset;
		if (preparedGlyphs.isEmpty()) {
			scratchIntersectionOffset = 0;
			scratchInfoOffset = 0;
		} else {
			PreparedGlyph last = preparedGlyphs.get(preparedGlyphs.size() - 1);
			scratchIntersectionOffset = last.nextScratchIntersectionOffset();
			scratchInfoOffset = last.nextScratchInfoOffset();
		}

		ByteBuffer pushConstants = recorder.stack.calloc(28);
		pushConstants.putInt(0, scratchIntersectionOffset);
		pushConstants.putInt(4, scratchInfoOffset);
		pushConstants.putInt(8, 2 * font.getFirstCurve(glyph));
		pushConstants.putInt(12, font.getNumCurves(glyph));
		pushConstants.putInt(16, height);
		pushConstants.putFloat(20, font.getGlyphMinY(glyph));
		pushConstants.putFloat(24, font.getGlyphMaxY(glyph));

		vkCmdPushConstants(
				recorder.commandBuffer, shared.scratchPipelineLayout,
				VK_SHADER_STAGE_COMPUTE_BIT, 0, pushConstants
		);
		//noinspection SuspiciousNameCombination
		vkCmdDispatch(recorder.commandBuffer, height, 1, 1);

		PreparedGlyph next = new PreparedGlyph();
		next.scratchIntersectionOffset = scratchIntersectionOffset;
		next.scratchInfoOffset = scratchInfoOffset;
		next.height = height;
		next.numGlyphCurves = font.getNumCurves(glyph);
		preparedGlyphs.add(next);
	}

	public void prepareTransfer(CommandRecorder recorder, Vk2dSharedText shared) {
		PreparedGlyph last = preparedGlyphs.get(preparedGlyphs.size() - 1);
		recorder.bufferBarrier(
				scratchIntersectionBuffer.child(0L, 4L * last.nextScratchIntersectionOffset()),
				ResourceUsage.computeBuffer(VK_ACCESS_SHADER_READ_BIT | VK_ACCESS_SHADER_WRITE_BIT),
				ResourceUsage.computeBuffer(VK_ACCESS_SHADER_READ_BIT)
		);
		recorder.bufferBarrier(
				scratchInfoBuffer.child(0L, 4L * last.nextScratchInfoOffset()),
				ResourceUsage.computeBuffer(VK_ACCESS_SHADER_WRITE_BIT),
				ResourceUsage.computeBuffer(VK_ACCESS_SHADER_READ_BIT)
		);
		vkCmdBindPipeline(recorder.commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, shared.transferPipeline);
		recorder.bindComputeDescriptors(shared.transferPipelineLayout, transferDescriptorSet);
	}

	public void transfer(CommandRecorder recorder, Vk2dSharedText shared) {
		if (preparedGlyphs.isEmpty()) return;
		if (preparedGlyphs.size() != 1) throw new UnsupportedOperationException("TODO");

		IntBuffer pushConstants = recorder.stack.callocInt(6);
		for (PreparedGlyph prepared : preparedGlyphs) {
			pushConstants.put(0, prepared.scratchIntersectionOffset);
			pushConstants.put(1, prepared.scratchInfoOffset);
			pushConstants.put(2, 0);
			pushConstants.put(3, 0);
			pushConstants.put(4, prepared.height);
			pushConstants.put(5, prepared.numGlyphCurves);
			vkCmdPushConstants(
					recorder.commandBuffer, shared.scratchPipelineLayout,
					VK_SHADER_STAGE_COMPUTE_BIT, 0, pushConstants
			);
			vkCmdDispatch(recorder.commandBuffer, 1, 1, 1);
		}

		recorder.bulkBufferBarrier(
				ResourceUsage.computeBuffer(VK_ACCESS_SHADER_WRITE_BIT),
				ResourceUsage.shaderRead(VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT),
				intersectionBuffer, infoBuffer // TODO Do it on a subsection of the region
		);
	}

	public long getRenderDescriptorSet() {
		return intersectionDescriptorSet;
	}

	private static class PreparedGlyph {

		int scratchIntersectionOffset, scratchInfoOffset, height, numGlyphCurves;

		int nextScratchIntersectionOffset() {
			return scratchIntersectionOffset + 2 * height * numGlyphCurves;
		}

		int nextScratchInfoOffset() {
			return scratchInfoOffset + height;
		}
	}
}
