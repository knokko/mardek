package com.github.knokko.vk2d;

import com.github.knokko.boiler.buffers.PerFrameBuffer;
import com.github.knokko.boiler.builders.BoilerBuilder;
import com.github.knokko.boiler.commands.SingleTimeCommands;
import com.github.knokko.boiler.images.ImageBuilder;
import com.github.knokko.boiler.memory.MemoryCombiner;
import com.github.knokko.boiler.synchronization.ResourceUsage;
import com.github.knokko.boiler.utilities.ImageCoding;
import com.github.knokko.vk2d.batch.Vk2dColorBatch;
import com.github.knokko.vk2d.frame.Vk2dFrame;
import com.github.knokko.vk2d.frame.Vk2dRenderStage;
import com.github.knokko.vk2d.pipeline.Vk2dPipelineContext;
import com.github.knokko.vk2d.pipeline.Vk2dPipelines;

import javax.imageio.ImageIO;

import java.io.File;
import java.io.IOException;

import static com.github.knokko.boiler.utilities.ColorPacker.rgb;
import static org.lwjgl.vulkan.VK10.*;

public class HeadlessPlayground {

	public static void main(String[] args) {

		// Initialize vk-boiler
		var boiler = new BoilerBuilder(
				VK_API_VERSION_1_0, "HeadlessPlayground", 1
		).enableDynamicRendering().build();

		// Initialize vk2d
		var config = new Vk2dConfig();
		config.color = true;

		var vk2d = new Vk2dInstance(boiler, config);
		var pipelines = new Vk2dPipelines(vk2d, Vk2dPipelineContext.dynamicRendering(boiler, VK_FORMAT_R8G8B8A8_SRGB));

		// Allocate memory for the target image and the per-frame buffer
		var memoryCombiner = new MemoryCombiner(boiler, "HeadlessMemory");
		var perFrameBuffer = new PerFrameBuffer(memoryCombiner.addMappedBuffer(
				1000L, pipelines.perFrameBufferAlignment(), pipelines.perFrameBufferUsage()
		));
		var targetImage = memoryCombiner.addImage(new ImageBuilder(
				"HeadlessTargetImage", 100, 100
		).colorAttachment().addUsage(VK_IMAGE_USAGE_TRANSFER_SRC_BIT), 1f);
		var resultImageDataBuffer = memoryCombiner.addMappedBuffer(
				4L * targetImage.width * targetImage.height, 4L, VK_BUFFER_USAGE_TRANSFER_DST_BIT
		);
		var memory = memoryCombiner.build(false);

		// Start the frame
		perFrameBuffer.startFrame(0);
		var frame = new Vk2dFrame(perFrameBuffer, VK_NULL_HANDLE, null);
		var stage = new Vk2dRenderStage(targetImage, perFrameBuffer, null, ResourceUsage.TRANSFER_SOURCE);
		frame.stages.add(stage);

		// Add render commands
		var colorBatch = pipelines.color.addBatch(stage, 4);
		colorBatch.fill(0, 0, 100, 30, rgb(1f, 0f, 0f));
		colorBatch.gradient(
				10, 50, 80, 70,
				rgb(0f, 0f, 1f), rgb(0f, 1f, 1f), rgb(0f, 0f, 1f)
		);

		// Record & run render commands + copy image data to the host + await completion
		SingleTimeCommands.submit(boiler, "HeadlessCommands", recorder -> {
			frame.record(recorder);
			recorder.copyImageToBuffer(targetImage, resultImageDataBuffer);
			recorder.bufferBarrier(resultImageDataBuffer, ResourceUsage.TRANSFER_DEST, ResourceUsage.HOST_READ);
		}).destroy();

		// Save result image to disk
		var bufferedImage = ImageCoding.decodeBufferedImage(
				resultImageDataBuffer.byteBuffer(), targetImage.width, targetImage.height
		);
		try {
			ImageIO.write(bufferedImage, "PNG", new File("headless-playground.png"));
		} catch (IOException failed) {
			throw new RuntimeException(failed);
		}

		// Clean everything up
		pipelines.destroy();
		vk2d.destroy();
		memory.destroy(boiler);
		boiler.destroyInitialObjects();
	}
}
