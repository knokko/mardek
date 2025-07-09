package com.github.knokko.vk2d;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.buffers.MappedVkbBuffer;
import com.github.knokko.boiler.buffers.PerFrameBuffer;
import com.github.knokko.boiler.builders.BoilerBuilder;
import com.github.knokko.boiler.commands.SingleTimeCommands;
import com.github.knokko.boiler.descriptors.DescriptorCombiner;
import com.github.knokko.boiler.descriptors.DescriptorUpdater;
import com.github.knokko.boiler.images.ImageBuilder;
import com.github.knokko.boiler.images.VkbImage;
import com.github.knokko.boiler.memory.MemoryBlock;
import com.github.knokko.boiler.memory.MemoryCombiner;
import com.github.knokko.boiler.synchronization.ResourceUsage;
import com.github.knokko.boiler.utilities.ImageCoding;
import com.github.knokko.vk2d.batch.Vk2dColorBatch;
import com.github.knokko.vk2d.batch.Vk2dGlyphBatch;
import com.github.knokko.vk2d.frame.Vk2dRenderStage;
import com.github.knokko.vk2d.pipeline.Vk2dPipelineContext;
import com.github.knokko.vk2d.pipeline.Vk2dPipelines;
import com.github.knokko.vk2d.text.Vk2dFont;
import com.github.knokko.vk2d.resource.Vk2dResourceBundle;
import com.github.knokko.vk2d.resource.Vk2dResourceLoader;
import com.github.knokko.vk2d.text.Vk2dTextBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkRenderingAttachmentInfo;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static com.github.knokko.boiler.utilities.ColorPacker.rgb;
import static com.github.knokko.boiler.utilities.ColorPacker.rgba;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK13.VK_API_VERSION_1_3;

public class GlyphOffsetPlayground {

	public static void main(String[] args) throws IOException {
		BoilerInstance boiler = new BoilerBuilder(
				VK_API_VERSION_1_3, "GlyphOffsetPlayground", 1
		).validation().forbidValidationErrors().doNotUseVma().enableDynamicRendering().build();

		Vk2dConfig config = new Vk2dConfig();
		config.color = true;
		config.text = true;
		Vk2dInstance instance = new Vk2dInstance(boiler, config);
		Vk2dPipelines pipelines = new Vk2dPipelines(
				instance, Vk2dPipelineContext.dynamicRendering(boiler, VK_FORMAT_R8G8B8A8_SRGB), config
		);

		MemoryCombiner combiner = new MemoryCombiner(boiler, "OffsetsMemory");
		Vk2dResourceLoader loader = new Vk2dResourceLoader(
				instance, GlyphOffsetPlayground.class.getResourceAsStream("text-benchmark-resources.bin")
		);
		loader.claimMemory(combiner);
		VkbImage targetImage = combiner.addImage(new ImageBuilder(
				"TargetImage", 90, 20
		).addUsage(
				VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT
		).format(VK_FORMAT_R8G8B8A8_SRGB), 1f);
		MappedVkbBuffer destinationBuffer = combiner.addMappedBuffer(
				4L * targetImage.width * targetImage.height, 4L,
				VK_BUFFER_USAGE_TRANSFER_DST_BIT
		);
		PerFrameBuffer perFrameBuffer = new PerFrameBuffer(combiner.addMappedBuffer(
				5000L, boiler.deviceProperties.limits().minStorageBufferOffsetAlignment(),
				VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT
		));
		DescriptorCombiner descriptors = new DescriptorCombiner(boiler);
		long[] perFrameDescriptorSet = descriptors.addMultiple(instance.bufferDescriptorSetLayout, 1);
		Vk2dTextBuffer textBuffer = new Vk2dTextBuffer(instance, combiner, descriptors, 1);
		MemoryBlock memory = combiner.build(false);
		loader.prepareStaging();
		SingleTimeCommands.submit(boiler, "Staging",
				recorder -> loader.performStaging(recorder, descriptors)
		).destroy();
		long descriptorPool = descriptors.build("OffsetsDescriptors");
		Vk2dResourceBundle resources = loader.finish();
		Vk2dFont font = resources.getFont(0);
		textBuffer.initializeDescriptorSets();
		try (MemoryStack stack = stackPush()) {
			DescriptorUpdater updater = new DescriptorUpdater(stack, 1);
			updater.writeStorageBuffer(0, perFrameDescriptorSet[0], 0, perFrameBuffer.buffer);
			updater.update(boiler);
		}

		Vk2dRenderStage frame = new Vk2dRenderStage(targetImage, perFrameBuffer, null, null);
		perFrameBuffer.startFrame(1);
		SingleTimeCommands.submit(boiler, "GlyphOffsets", recorder -> {
			int heightA = 14;
			Vk2dGlyphBatch batch = pipelines.text.addBatch(
					frame, 16, recorder, textBuffer, perFrameDescriptorSet[0]
			);
			Vk2dColorBatch colorBatch = pipelines.color.addBatch(frame, 6);

			batch.drawPrimitiveString(
					"helloAgi", 1f, 16f, font, heightA, rgb(255, 255, 255)
			);
			colorBatch.fill(0, 16, targetImage.width, targetImage.height, rgba(255, 0, 0, 100));
			textBuffer.record(recorder);

			VkRenderingAttachmentInfo.Buffer colorAttachments = recorder.singleColorRenderingAttachment(
					targetImage.vkImageView, VK_ATTACHMENT_LOAD_OP_CLEAR, VK_ATTACHMENT_STORE_OP_STORE, 0
			);
			recorder.transitionLayout(targetImage, null, ResourceUsage.COLOR_ATTACHMENT_WRITE);
			recorder.beginSimpleDynamicRendering(targetImage.width, targetImage.height, colorAttachments, null, null);
			recorder.dynamicViewportAndScissor(targetImage.width, targetImage.height);
			frame.record(recorder);
			recorder.endDynamicRendering();

			recorder.transitionLayout(targetImage, ResourceUsage.COLOR_ATTACHMENT_WRITE, ResourceUsage.TRANSFER_SOURCE);
			recorder.copyImageToBuffer(targetImage, destinationBuffer);
			recorder.bufferBarrier(destinationBuffer, ResourceUsage.TRANSFER_DEST, ResourceUsage.HOST_READ);
		}).destroy();

		BufferedImage resultImage = ImageCoding.decodeBufferedImage(
				destinationBuffer.byteBuffer(), targetImage.width, targetImage.height
		);
		ImageIO.write(resultImage, "PNG", new File("test.png"));

		vkDestroyDescriptorPool(boiler.vkDevice(), descriptorPool, null);
		memory.destroy(boiler);
		pipelines.destroy();
		instance.destroy();
		boiler.destroyInitialObjects();
	}
}
