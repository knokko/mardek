package com.github.knokko.vk2d;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.buffers.PerFrameBuffer;
import com.github.knokko.boiler.builders.BoilerBuilder;
import com.github.knokko.boiler.builders.WindowBuilder;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.commands.SingleTimeCommands;
import com.github.knokko.boiler.memory.MemoryBlock;
import com.github.knokko.boiler.memory.MemoryCombiner;
import com.github.knokko.boiler.memory.callbacks.CallbackUserData;
import com.github.knokko.boiler.synchronization.ResourceUsage;
import com.github.knokko.boiler.window.*;
import com.github.knokko.vk2d.pipeline.Vk2dPipelineContext;
import com.github.knokko.vk2d.resource.Vk2dResourceBundle;
import com.github.knokko.vk2d.resource.Vk2dResourceLoader;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

import static org.lwjgl.sdl.SDLVideo.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK13.VK_API_VERSION_1_3;

public abstract class Vk2dWindow extends SimpleWindowRenderLoop {

	private static int choosePresentMode(VkbWindow window, boolean capFps) {
		if (capFps) return VK_PRESENT_MODE_FIFO_KHR;
		if (window.supportedPresentModes.contains(VK_PRESENT_MODE_MAILBOX_KHR)) {
			return VK_PRESENT_MODE_MAILBOX_KHR;
		}
		if (window.supportedPresentModes.contains(VK_PRESENT_MODE_IMMEDIATE_KHR)) {
			return VK_PRESENT_MODE_IMMEDIATE_KHR;
		}
		System.err.println("This graphics driver doesn't appear to support any present mode with uncapped FPS");
		return VK_PRESENT_MODE_FIFO_KHR;
	}

	protected Vk2dPipelineContext pipelineContext;
	protected Vk2dShared shared;
	private MemoryBlock memory;
	protected PerFrameBuffer perFrameBuffer;
	protected Vk2dResourceBundle resources;
	private SwapchainResourceManager<Object, Long> framebuffers;

	public Vk2dWindow(VkbWindow window, boolean capFps) {
		super(
				window, 2, false, choosePresentMode(window, capFps),
				ResourceUsage.COLOR_ATTACHMENT_WRITE, ResourceUsage.COLOR_ATTACHMENT_WRITE
		);
	}

	protected InputStream initialResourceBundle() throws IOException {
		return null;
	}

	@Override
	protected void setup(BoilerInstance boiler, MemoryStack stack) {
		super.setup(boiler, stack);
		this.pipelineContext = Vk2dPipelineContext.renderPass(boiler, window.surfaceFormat);
		this.shared = new Vk2dShared(boiler);

		try {
			InputStream resourceInput = initialResourceBundle();
			Vk2dResourceLoader loader = null;
			if (resourceInput != null) loader = new Vk2dResourceLoader(resourceInput);

			MemoryCombiner combiner = new MemoryCombiner(boiler, "Vk2dPersistent");
			if (loader != null) loader.claimMemory(boiler, combiner);
			createResources(boiler, combiner);
			this.memory = combiner.build(false);

			if (loader != null) {
				loader.prepareStaging();

				Vk2dResourceLoader[] pLoader = { loader };
				SingleTimeCommands.submit(
						boiler, "Vk2dStaging",
						recorder -> pLoader[0].performStaging(boiler, recorder, shared)
				).destroy();
				this.resources = loader.finish(boiler, shared);
			}

			if (resourceInput != null) resourceInput.close();
		} catch (IOException io) {
			throw new RuntimeException(io);
		}

		this.framebuffers = new SwapchainResourceManager<>() {

			@Override
			protected Long createImage(Object swapchain, AcquiredImage swapchainImage) {
				return boiler.images.createFramebuffer(
						pipelineContext.vkRenderPass(), swapchainImage.width(), swapchainImage.height(),
						"SwapchainFramebuffer", swapchainImage.image().vkImageView
				);
			}

			@Override
			public void destroyImage(Long framebuffer) {
				try (MemoryStack stack = stackPush()) {
					vkDestroyFramebuffer(
							boiler.vkDevice(), framebuffer,
							CallbackUserData.FRAME_BUFFER.put(stack, boiler)
					);
				}
			}
		};
	}

	protected void createResources(BoilerInstance boiler, MemoryCombiner combiner) {
		this.perFrameBuffer = new PerFrameBuffer(combiner.addMappedBuffer(
				10_000_000L, 4L, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT
		));
	}

	@Override
	protected void recordFrame(
			MemoryStack stack, int frameIndex, CommandRecorder recorder,
			AcquiredImage acquiredImage, BoilerInstance boiler
	) {
		perFrameBuffer.startFrame(frameIndex);

		VkRenderPassBeginInfo biRenderPass = VkRenderPassBeginInfo.calloc(stack);
		biRenderPass.sType$Default();
		biRenderPass.renderPass(pipelineContext.vkRenderPass());
		biRenderPass.framebuffer(framebuffers.get(acquiredImage));
		biRenderPass.renderArea().extent().set(acquiredImage.width(), acquiredImage.height());
		biRenderPass.pClearValues(VkClearValue.calloc(1, stack));
		biRenderPass.clearValueCount(1);

		vkCmdBeginRenderPass(recorder.commandBuffer, biRenderPass, VK_SUBPASS_CONTENTS_INLINE);
		recorder.dynamicViewportAndScissor(acquiredImage.width(), acquiredImage.height());
		Vk2dFrame frame = new Vk2dFrame(perFrameBuffer, acquiredImage.width(), acquiredImage.height());

		renderFrame(frame, recorder, acquiredImage, boiler);

		frame.record(recorder);
		vkCmdEndRenderPass(recorder.commandBuffer);
	}

	protected abstract void renderFrame(
			Vk2dFrame frame, CommandRecorder recorder,
			AcquiredImage swapchainImage, BoilerInstance boiler
	);

	@Override
	protected void cleanUp(BoilerInstance boiler) {
		super.cleanUp(boiler);
		try (MemoryStack stack = stackPush()) {
			vkDestroyRenderPass(
					boiler.vkDevice(), pipelineContext.vkRenderPass(),
					CallbackUserData.RENDER_PASS.put(stack, boiler)
			);
		}
		if (resources != null) resources.destroy(boiler);
		memory.destroy(boiler);
		shared.destroy(boiler);
	}

	public static void bootstrap(
			String title, int appVersion, Vk2dValidationMode validationMode,
			Function<VkbWindow, WindowRenderLoop> createRenderer
	) {
		BoilerBuilder builder = new BoilerBuilder(
				validationMode == Vk2dValidationMode.STRONG ? VK_API_VERSION_1_3 : VK_API_VERSION_1_0, title, appVersion
		);
		if (validationMode != Vk2dValidationMode.NONE) builder.validation();
		if (validationMode == Vk2dValidationMode.STRONG) builder.forbidValidationErrors();
		BoilerInstance boiler = builder.addWindow(
				new WindowBuilder(
						1200, 800, VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT
				).sdlFlags(SDL_WINDOW_VULKAN | SDL_WINDOW_MAXIMIZED | SDL_WINDOW_RESIZABLE).hideUntilFirstFrame()
		).useSDL().build();

		WindowEventLoop eventLoop = new WindowEventLoop();
		eventLoop.addWindow(createRenderer.apply(boiler.window()));
		eventLoop.runMain();

		boiler.destroyInitialObjects();
	}
}
