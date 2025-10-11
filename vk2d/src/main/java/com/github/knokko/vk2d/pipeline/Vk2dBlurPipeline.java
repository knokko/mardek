package com.github.knokko.vk2d.pipeline;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.buffers.PerFrameBuffer;
import com.github.knokko.boiler.buffers.VkbBuffer;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.descriptors.DescriptorCombiner;
import com.github.knokko.boiler.descriptors.DescriptorUpdater;
import com.github.knokko.boiler.images.ImageBuilder;
import com.github.knokko.boiler.images.VkbImage;
import com.github.knokko.boiler.memory.MemoryCombiner;
import com.github.knokko.boiler.synchronization.ResourceUsage;
import com.github.knokko.vk2d.frame.Vk2dComputeStage;
import com.github.knokko.vk2d.frame.Vk2dFrame;
import com.github.knokko.vk2d.frame.Vk2dRenderStage;
import com.github.knokko.vk2d.Vk2dInstance;
import com.github.knokko.vk2d.batch.MiniBatch;
import com.github.knokko.vk2d.batch.Vk2dBatch;
import com.github.knokko.vk2d.batch.Vk2dBlurBatch;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static com.github.knokko.boiler.utilities.BoilerMath.nextMultipleOf;
import static java.lang.Math.max;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class Vk2dBlurPipeline extends Vk2dPipeline {

	private static final int VERTEX_SIZE = 8;
	private static final int[] BYTES_PER_TRIANGLE = { 3 * VERTEX_SIZE };
	private static final int[] VERTEX_ALIGNMENTS = { 6 * VERTEX_SIZE };

	private final Vk2dInstance instance;

	public Vk2dBlurPipeline(Vk2dPipelineContext context, Vk2dInstance instance) {
		this.instance = instance;
		try (MemoryStack stack = stackPush()) {
			var vertexAttributes = VkVertexInputAttributeDescription.calloc(1, stack);
			//noinspection resource
			vertexAttributes.get(0).set(0, 0, VK_FORMAT_R32G32_UINT, 0);

			var builder = pipelineBuilder(context, stack);
			builder.simpleShaderStages(
					"Blur", "com/github/knokko/vk2d/blur/",
					"sample.vert.spv", "sample.frag.spv"
			);
			simpleVertexInput(builder, stack, vertexAttributes, VERTEX_SIZE);
			builder.ciPipeline.layout(instance.blurPipelineLayoutSample);

			this.vkPipeline = builder.build("Vk2dBlurPipelineSample");
		}
	}

	public Descriptors[] claimResources(int numFramesInFlight, Vk2dInstance vk2d, DescriptorCombiner descriptors) {
		Descriptors[] blurResources = new Descriptors[numFramesInFlight];
		for (int frame = 0; frame < numFramesInFlight; frame++) {
			var resources = new Descriptors();
			resources.request(vk2d, descriptors);
			blurResources[frame] = resources;
		}
		return blurResources;
	}

	public Vk2dRenderStage addSourceStage(Vk2dFrame frame, Framebuffer framebuffer, int insertionIndex) {
		Vk2dRenderStage sourceStage = new Vk2dRenderStage(
				framebuffer.sourceImage, frame.perFrameBuffer,
				new ResourceUsage(VK_IMAGE_LAYOUT_UNDEFINED, VK_ACCESS_SHADER_READ_BIT, VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT),
				ResourceUsage.shaderRead(VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT)
		);
		if (insertionIndex < 0) frame.stages.add(sourceStage);
		else frame.stages.add(insertionIndex, sourceStage);

		if (frame.imageViewToFramebuffer != null) {
			frame.imageViewToFramebuffer.put(framebuffer.sourceImage.vkImageView, framebuffer.sourceFramebuffer);
		}
		return sourceStage;
	}

	public ComputeStage addComputeStage(
			Vk2dFrame frame, Descriptors descriptors, Framebuffer framebuffer,
			int filterSize, int sectionLength, int insertionIndex
	) {
		ComputeStage stage = new ComputeStage(instance);
		stage.additional(descriptors, framebuffer, filterSize, sectionLength);
		if (insertionIndex >= 0) frame.stages.add(insertionIndex, stage);
		else frame.stages.add(stage);
		return stage;
	}

	public Vk2dBlurBatch addBatch(
			Vk2dRenderStage destinationStage, Framebuffer framebuffer, Descriptors descriptors,
			float minX, float minY, float boundX, float boundY
	) {
		return new Vk2dBlurBatch(
				this, destinationStage,
				framebuffer.bufferWidth,
				framebuffer.bufferHeight,
				minX, minY, boundX, boundY,
				descriptors.sampleDescriptorSet
		);
	}

	@Override
	protected int[] getBytesPerTriangle() {
		return BYTES_PER_TRIANGLE;
	}

	@Override
	protected int[] getVertexAlignments() {
		return VERTEX_ALIGNMENTS;
	}

	@Override
	public void recordBatch(CommandRecorder recorder, PerFrameBuffer perFrameBuffer, MiniBatch miniBatch, Vk2dBatch batch) {
		Vk2dBlurBatch blur = (Vk2dBlurBatch) batch;
		recorder.bindGraphicsDescriptors(instance.blurPipelineLayoutSample, blur.descriptorSet);

		IntBuffer sharedPushConstants = recorder.stack.callocInt(2);
		sharedPushConstants.put(0, blur.textureWidth);
		sharedPushConstants.put(1, blur.textureHeight);

		FloatBuffer vertexPushConstants = recorder.stack.callocFloat(4);
		vertexPushConstants.put(0, blur.minX);
		vertexPushConstants.put(1, blur.minY);
		vertexPushConstants.put(2, blur.boundX);
		vertexPushConstants.put(3, blur.boundY);

		vkCmdPushConstants(
				recorder.commandBuffer, instance.blurPipelineLayoutSample,
				VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
				0, sharedPushConstants
		);
		vkCmdPushConstants(
				recorder.commandBuffer, instance.blurPipelineLayoutSample,
				VK_SHADER_STAGE_VERTEX_BIT, 8, vertexPushConstants
		);

		int firstVertex = Math.toIntExact((miniBatch.vertexBuffers()[0].offset - perFrameBuffer.buffer.offset) / VERTEX_SIZE);
		vkCmdDraw(recorder.commandBuffer, 6, 1, firstVertex, 0);
	}

	public Framebuffer createFramebuffer(
			MemoryCombiner memory, int sourceFormat, int sourceWidth, int sourceHeight,
			int bufferWidth, int bufferHeight
	) {
		sourceWidth = max(1, sourceWidth);
		sourceHeight = max(1, sourceHeight);
		bufferWidth = max(1, bufferWidth);
		bufferHeight = max(1, bufferHeight);

		VkbImage sourceImage = memory.addImage(new ImageBuilder(
				"BlurSourceImage", sourceWidth, sourceHeight
		).colorAttachment().addUsage(VK_IMAGE_USAGE_SAMPLED_BIT).format(sourceFormat), 1f);

		long alignment = instance.boiler.deviceProperties.limits().minStorageBufferOffsetAlignment();
		VkbBuffer buffer1 = memory.addBuffer(
				4L * bufferWidth * bufferHeight, alignment, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, 1f
		);
		VkbBuffer buffer2 = memory.addBuffer(
				4L * bufferWidth * bufferHeight, alignment, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, 1f
		);

		return new Framebuffer(sourceImage, buffer1, buffer2, bufferWidth, bufferHeight);
	}

	public static class Framebuffer {

		public final VkbImage sourceImage;
		public final VkbBuffer stage1, stage2;
		public final int bufferWidth, bufferHeight;

		public long sourceFramebuffer;

		public Framebuffer(VkbImage sourceImage, VkbBuffer stage1, VkbBuffer stage2, int bufferWidth, int bufferHeight) {
			this.sourceImage = sourceImage;
			this.stage1 = stage1;
			this.stage2 = stage2;
			this.bufferWidth = bufferWidth;
			this.bufferHeight = bufferHeight;
		}

		public void createFramebuffer(BoilerInstance boiler, long vkRenderPass) {
			this.sourceFramebuffer = boiler.images.createFramebuffer(
					vkRenderPass, sourceImage.width, sourceImage.height,
					"BlurFramebuffer", sourceImage.vkImageView
			);
		}
	}

	private record ComputeJob(
			Descriptors descriptors, int bufferWidth, int bufferHeight,
			int filterSize, int sectionLength
	) {}

	public static class ComputeStage extends Vk2dComputeStage {

		final Vk2dInstance vk2d;
		List<ComputeJob> jobs = new ArrayList<>();

		ComputeStage(Vk2dInstance vk2d) {
			this.vk2d = vk2d;
		}

		public void additional(Descriptors descriptors, Framebuffer framebuffer, int filterSize, int sectionLength) {
			descriptors.sourceImage = framebuffer.sourceImage;
			descriptors.stage1Buffer = framebuffer.stage1;
			descriptors.stage2Buffer = framebuffer.stage2;
			try (MemoryStack stack = stackPush()) {
				descriptors.update(vk2d, stack);
			}
			this.jobs.add(new ComputeJob(
					descriptors, framebuffer.bufferWidth, framebuffer.bufferHeight,
					filterSize, sectionLength
			));
		}

		@Override
		public void record(CommandRecorder recorder) {
			for (ComputeJob job : jobs) job.descriptors.update(vk2d, recorder.stack);
			VkbBuffer[] stage1Buffers = jobs.stream().map(
					job -> job.descriptors.stage1Buffer
			).toArray(VkbBuffer[]::new);
			VkbBuffer[] stage2Buffers = jobs.stream().map(
					job -> job.descriptors.stage2Buffer
			).toArray(VkbBuffer[]::new);

			recorder.bulkBufferBarrier(
					ResourceUsage.computeBuffer(VK_ACCESS_SHADER_READ_BIT),
					ResourceUsage.computeBuffer(VK_ACCESS_SHADER_WRITE_BIT),
					stage1Buffers
			);
			vkCmdBindPipeline(recorder.commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, vk2d.blurPipeline1);

			IntBuffer computePushConstants = recorder.stack.callocInt(4);
			for (ComputeJob job : jobs) {
				recorder.bindComputeDescriptors(vk2d.blurPipelineLayout1, job.descriptors.stage1DescriptorSet);
				computePushConstants.put(0, job.bufferWidth);
				computePushConstants.put(1, job.bufferHeight);
				computePushConstants.put(2, job.filterSize);
				computePushConstants.put(3, job.sectionLength);
				vkCmdPushConstants(
						recorder.commandBuffer, vk2d.blurPipelineLayout1,
						VK_SHADER_STAGE_COMPUTE_BIT, 0, computePushConstants
				);
				vkCmdDispatch(
						recorder.commandBuffer, nextMultipleOf(job.bufferHeight, 64) / 64,
						nextMultipleOf(job.bufferWidth, job.sectionLength) / job.sectionLength, 1
				);
			}

			recorder.bulkBufferBarrier(
					ResourceUsage.computeBuffer(VK_ACCESS_SHADER_WRITE_BIT),
					ResourceUsage.computeBuffer(VK_ACCESS_SHADER_READ_BIT),
					stage1Buffers
			);
			recorder.bulkBufferBarrier(
					ResourceUsage.shaderRead(VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT),
					ResourceUsage.computeBuffer(VK_ACCESS_SHADER_WRITE_BIT),
					stage2Buffers
			);

			vkCmdBindPipeline(recorder.commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, vk2d.blurPipeline2);
			for (ComputeJob job : jobs) {
				recorder.bindComputeDescriptors(vk2d.blurPipelineLayout2, job.descriptors.stage2DescriptorSet);
				computePushConstants.put(0, job.bufferWidth);
				computePushConstants.put(1, job.bufferHeight);
				computePushConstants.put(2, job.filterSize);
				computePushConstants.put(3, job.sectionLength);
				vkCmdPushConstants(
						recorder.commandBuffer, vk2d.blurPipelineLayout2,
						VK_SHADER_STAGE_COMPUTE_BIT, 0, computePushConstants
				);
				vkCmdDispatch(
						recorder.commandBuffer, nextMultipleOf(job.bufferWidth, 64) / 64,
						nextMultipleOf(job.bufferHeight, job.sectionLength) / job.sectionLength, 1
				);
			}

			recorder.bulkBufferBarrier(
					ResourceUsage.computeBuffer(VK_ACCESS_SHADER_WRITE_BIT),
					ResourceUsage.shaderRead(VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT),
					stage2Buffers
			);
		}
	}

	public static class Descriptors {
		VkbImage sourceImage, previousBlurTarget;
		VkbBuffer stage1Buffer, stage2Buffer;

		long stage1DescriptorSet, stage2DescriptorSet, sampleDescriptorSet;

		public void request(Vk2dInstance vk2d, DescriptorCombiner descriptors) {
			descriptors.addSingle(
					vk2d.blurDescriptorLayout1,
					descriptorSet -> stage1DescriptorSet = descriptorSet
			);
			descriptors.addSingle(
					vk2d.doubleComputeBufferDescriptorLayout,
					descriptorSet -> stage2DescriptorSet = descriptorSet
			);
			descriptors.addSingle(
					vk2d.bufferDescriptorSetLayout,
					descriptorSet -> sampleDescriptorSet = descriptorSet
			);
		}

		void update(Vk2dInstance vk2d, MemoryStack stack) {
			if (sourceImage == previousBlurTarget) return;

			previousBlurTarget = sourceImage;
			DescriptorUpdater updater = new DescriptorUpdater(stack, 5);
			updater.writeStorageBuffer(0, stage1DescriptorSet, 0, stage1Buffer);
			updater.writeImage(1, stage1DescriptorSet, 1, sourceImage.vkImageView, vk2d.smoothSampler);
			updater.writeStorageBuffer(2, stage2DescriptorSet, 0, stage1Buffer);
			updater.writeStorageBuffer(3, stage2DescriptorSet, 1, stage2Buffer);
			updater.writeStorageBuffer(4, sampleDescriptorSet, 0, stage2Buffer);
			updater.update(vk2d.boiler);
		}
	}
}
