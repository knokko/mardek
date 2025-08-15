package com.github.knokko.vk2d;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.descriptors.DescriptorCombiner;
import com.github.knokko.boiler.memory.MemoryBlock;
import com.github.knokko.boiler.memory.MemoryCombiner;
import com.github.knokko.boiler.memory.callbacks.CallbackUserData;
import com.github.knokko.boiler.window.AcquiredImage;
import com.github.knokko.boiler.window.SwapchainResourceManager;
import com.github.knokko.boiler.window.VkbWindow;
import com.github.knokko.vk2d.batch.Vk2dColorBatch;
import com.github.knokko.vk2d.batch.Vk2dImageBatch;
import com.github.knokko.vk2d.frame.Vk2dRenderStage;
import com.github.knokko.vk2d.frame.Vk2dSwapchainFrame;
import com.github.knokko.vk2d.pipeline.Vk2dBlurPipeline;
import org.lwjgl.system.MemoryStack;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import static com.github.knokko.boiler.utilities.ColorPacker.rgb;
import static org.lwjgl.vulkan.VK10.*;

public class BlurBenchmark extends Vk2dWindow {

	SwapchainResourceManager<SwapchainResources, SwapchainResources> swapchainResources;
	Vk2dBlurPipeline.Descriptors[] blurDescriptors;

	public BlurBenchmark(VkbWindow window) {
		super(window, true);
	}

	@Override
	protected InputStream initialResourceBundle() throws IOException {
		return Files.newInputStream(new File("image-benchmark-resources.bin").toPath());
	}

	@Override
	protected void setup(BoilerInstance boiler, MemoryStack stack) {
		super.setup(boiler, stack);
		this.swapchainResources = new SwapchainResourceManager<>() {

			@Override
			public SwapchainResources createSwapchain(int width, int height, int numImages) {
				return new SwapchainResources(boiler, window, pipelines.blur, vkRenderPass, width, height);
			}

			@Override
			public SwapchainResources createImage(SwapchainResources swapchain, AcquiredImage image) {
				return swapchain;
			}

			@Override
			public void destroySwapchain(SwapchainResources resources) {
				resources.memoryBlock.destroy(boiler);
				try (MemoryStack stack = MemoryStack.stackPush()) {
					vkDestroyFramebuffer(
							boiler.vkDevice(), resources.blur.sourceFramebuffer,
							CallbackUserData.FRAME_BUFFER.put(stack, boiler)
					);
				}
			}
		};
	}

	@Override
	protected void setupConfig(Vk2dConfig config) {
		config.color = true;
		config.image = true;
		config.blur = true;
	}

	@Override
	protected void createResources(BoilerInstance boiler, MemoryCombiner memory, DescriptorCombiner descriptors) {
		super.createResources(boiler, memory, descriptors);
		this.blurDescriptors = pipelines.blur.claimResources(numFramesInFlight, instance, descriptors);
	}

	@Override
	protected void renderFrame(
			Vk2dSwapchainFrame frame, int frameIndex,
			CommandRecorder recorder, AcquiredImage swapchainImage, BoilerInstance boiler
	) {
		printFps();
		SwapchainResources associated = swapchainResources.get(swapchainImage);
		Vk2dRenderStage sourceStage = pipelines.blur.addSourceStage(frame, associated.blur, -1);
		pipelines.blur.addComputeStage(
				frame, blurDescriptors[frameIndex], associated.blur,
				4, 100, -1
		);

		Vk2dColorBatch colorBatch = pipelines.color.addBatch(sourceStage, 2);
		colorBatch.fill(0, 0, sourceStage.width, sourceStage.height, rgb(0, 200, 250));

		Vk2dImageBatch imageBatch = pipelines.image.addBatch(sourceStage, 10, resources);
		imageBatch.simpleScale(50, 200, 10, 10);

		pipelines.blur.addBatch(
				frame.swapchainStage, associated.blur, blurDescriptors[frameIndex],
				0f, 0f, frame.swapchainStage.width, frame.swapchainStage.height
		).fixedColorTransform(0, rgb(1f, 1f, 0f));
	}

	static class SwapchainResources {

		final Vk2dBlurPipeline.Framebuffer blur;
		final MemoryBlock memoryBlock;

		SwapchainResources(
				BoilerInstance boiler, VkbWindow window, Vk2dBlurPipeline pipeline,
				long renderPass, int width, int height
		) {
			MemoryCombiner memory = new MemoryCombiner(boiler, "BlurMemory");
			this.blur = pipeline.createFramebuffer(memory, window.surfaceFormat, width, height, width / 4, height / 4);
			this.memoryBlock = memory.build(false);
			this.blur.createFramebuffer(boiler, renderPass);
		}
	}

	public static void main(String[] args) {
		bootstrap("BlurBenchmark", VK_API_VERSION_1_0, Vk2dValidationMode.STRONG, BlurBenchmark::new);
	}
}
