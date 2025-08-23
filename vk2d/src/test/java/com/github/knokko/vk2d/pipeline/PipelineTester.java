package com.github.knokko.vk2d.pipeline;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.buffers.MappedVkbBuffer;
import com.github.knokko.boiler.buffers.PerFrameBuffer;
import com.github.knokko.boiler.builders.BoilerBuilder;
import com.github.knokko.boiler.commands.SingleTimeCommands;
import com.github.knokko.boiler.descriptors.DescriptorCombiner;
import com.github.knokko.boiler.images.ImageBuilder;
import com.github.knokko.boiler.images.VkbImage;
import com.github.knokko.boiler.memory.MemoryBlock;
import com.github.knokko.boiler.memory.MemoryCombiner;
import com.github.knokko.boiler.synchronization.ResourceUsage;
import com.github.knokko.boiler.utilities.ImageCoding;
import com.github.knokko.vk2d.Vk2dConfig;
import com.github.knokko.vk2d.Vk2dInstance;
import com.github.knokko.vk2d.frame.Vk2dFrame;
import com.github.knokko.vk2d.frame.Vk2dRenderStage;
import com.github.knokko.vk2d.resource.Vk2dResourceBundle;
import com.github.knokko.vk2d.resource.Vk2dResourceLoader;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Consumer;

import static java.lang.Math.abs;
import static org.junit.jupiter.api.Assertions.fail;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_TRANSFER_SRC_BIT;
import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;

record PipelineTester(Vk2dFrame frame, Vk2dRenderStage stage, Vk2dResourceBundle bundle) {

	static BoilerInstance boiler;
	static Vk2dInstance vk2d;
	static Vk2dPipelines staticPipelines;

	static void staticSetup(Consumer<Vk2dConfig> setupConfig) {
		boiler = new BoilerBuilder(
				VK_API_VERSION_1_0, "TestColorPipeline", 1
		)
				.enableDynamicRendering()
				.validation().forbidValidationErrors()
				.defaultTimeout(10_000_000_000L)
				.build();

		Vk2dConfig config = new Vk2dConfig();
		setupConfig.accept(config);
		vk2d = new Vk2dInstance(boiler, config);
		staticPipelines = new Vk2dPipelines(
				vk2d, Vk2dPipelineContext.dynamicRendering(boiler, VK_FORMAT_R8G8B8A8_SRGB), config
		);
	}

	static void runTest(String testCase, File resourceBundle, int width, int height, Consumer<PipelineTester> render) {
		try {
			Vk2dResourceLoader loader = resourceBundle != null ?
					new Vk2dResourceLoader(vk2d, Files.newInputStream(resourceBundle.toPath())) : null;

			MemoryCombiner combiner = new MemoryCombiner(boiler, "TestColorMemory");
			if (loader != null) loader.claimMemory(combiner);
			PerFrameBuffer perFrameBuffer = new PerFrameBuffer(combiner.addMappedBuffer(
					1000L, 4L, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT
			));
			VkbImage targetImage = combiner.addImage(new ImageBuilder(
					"TestColorTargetImage", width, height
			).format(VK_FORMAT_R8G8B8A8_SRGB).setUsage(
					VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT
			), 1f);
			MappedVkbBuffer destinationBuffer = combiner.addMappedBuffer(
					4L * targetImage.width * targetImage.height, 4L, VK_BUFFER_USAGE_TRANSFER_DST_BIT
			);
			MemoryBlock memory = combiner.build(true);

			long descriptorPool = VK_NULL_HANDLE;
			if (loader != null) {
				loader.prepareStaging();
				DescriptorCombiner descriptors = new DescriptorCombiner(boiler);
				SingleTimeCommands.submit(boiler, testCase + " ResourceBundle", recorder ->
						loader.performStaging(recorder, descriptors)
				).destroy();
				descriptorPool = descriptors.build(testCase + "DescriptorBundle");
			}

			Vk2dFrame frame = new Vk2dFrame(perFrameBuffer, VK_NULL_HANDLE, null);
			Vk2dRenderStage stage = new Vk2dRenderStage(
					targetImage, perFrameBuffer, null, ResourceUsage.TRANSFER_SOURCE
			);
			frame.stages.add(stage);

			perFrameBuffer.startFrame(0);
			render.accept(new PipelineTester(frame, stage, loader != null ? loader.finish() : null));
			SingleTimeCommands.submit(boiler, testCase, recorder -> {
				frame.record(recorder);
				recorder.copyImageToBuffer(targetImage, destinationBuffer);
			}).destroy();

			BufferedImage actual = ImageCoding.decodeBufferedImage(
					destinationBuffer.byteBuffer(), targetImage.width, targetImage.height
			);

			try {
				BufferedImage expected = ImageIO.read(new File("test-cases/expected/" + testCase + ".png"));

				String failure = null;
				if (expected.getWidth() == actual.getWidth() && expected.getHeight() == actual.getHeight()) {
					for (int y = 0; y < expected.getHeight(); y++) {
						for (int x = 0; x < expected.getWidth(); x++) {
							Color e = new Color(expected.getRGB(x, y), true);
							Color a = new Color(actual.getRGB(x, y), true);
							int threshold = 2;
							if (abs(a.getRed() - e.getRed()) > threshold) failure = "red mismatch at " + x + ", " + y;
							if (abs(a.getGreen() - e.getGreen()) > threshold) failure = "green mismatch at " + x + ", " + y;
							if (abs(a.getBlue() - e.getBlue()) > threshold) failure = "blue mismatch at " + x + ", " + y;
							if (abs(a.getAlpha() - e.getAlpha()) > threshold) failure = "alpha mismatch at " + x + ", " + y;
						}
					}
				} else {
					failure = "Sizes are different";
				}

				if (failure != null) {
					ImageIO.write(actual, "PNG", new File("test-cases/actual/" + testCase + ".png"));
					fail(failure);
				}
			} catch (IOException io) {
				try {
					ImageIO.write(actual, "PNG", new File("test-cases/actual/" + testCase + ".png"));
				} catch (IOException ignored) {
				}
				throw new RuntimeException(io);
			}

			memory.destroy(boiler);
			vkDestroyDescriptorPool(boiler.vkDevice(), descriptorPool, null);
		} catch (IOException failure) {
			fail(failure);
		}
	}

	static void staticTearDown() {
		staticPipelines.destroy();
		vk2d.destroy();
		boiler.destroyInitialObjects();
	}
}
