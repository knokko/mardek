package com.github.knokko.vk2d;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.buffers.PerFrameBuffer;
import com.github.knokko.boiler.builders.BoilerBuilder;
import com.github.knokko.boiler.builders.WindowBuilder;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.memory.MemoryBlock;
import com.github.knokko.boiler.memory.MemoryCombiner;
import com.github.knokko.boiler.memory.callbacks.CallbackUserData;
import com.github.knokko.boiler.synchronization.ResourceUsage;
import com.github.knokko.boiler.window.*;
import com.github.knokko.vk2d.pipeline.PipelineContext;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;

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

	private MemoryBlock memory;
	protected PerFrameBuffer perFrameBuffer;
	protected PipelineContext pipelineContext;
	private SwapchainResourceManager<Object, Long> framebuffers;

	public Vk2dWindow(VkbWindow window, boolean capFps) {
		super(
				window, 2, false, choosePresentMode(window, capFps),
				ResourceUsage.COLOR_ATTACHMENT_WRITE, ResourceUsage.COLOR_ATTACHMENT_WRITE
		);
	}

	@Override
	protected void setup(BoilerInstance boiler, MemoryStack stack) {
		super.setup(boiler, stack);
		this.pipelineContext = PipelineContext.renderPass(boiler, window.surfaceFormat);

		MemoryCombiner combiner = new MemoryCombiner(boiler, "Vk2dWindowMemory");
		createResources(combiner);
		this.memory = combiner.build(false);

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

	protected void createResources(MemoryCombiner combiner) {
		this.perFrameBuffer = new PerFrameBuffer(combiner.addMappedBuffer(
				10_000L, 4L, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT
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
		memory.destroy(boiler);
	}

	public static void bootstrap(
			String title, int appVersion, ValidationMode validationMode,
			Function<VkbWindow, WindowRenderLoop> createRenderer
	) {
		BoilerBuilder builder = new BoilerBuilder(
				validationMode == ValidationMode.STRONG ? VK_API_VERSION_1_3 : VK_API_VERSION_1_0, title, appVersion
		);
		if (validationMode != ValidationMode.NONE) builder.validation();
		if (validationMode == ValidationMode.STRONG) builder.forbidValidationErrors();
		BoilerInstance boiler = builder.addWindow(
				new WindowBuilder(
						1200, 800, VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT
				).sdlFlags(SDL_WINDOW_VULKAN | SDL_WINDOW_MAXIMIZED | SDL_WINDOW_RESIZABLE)
		).useSDL().build();

		WindowEventLoop eventLoop = new WindowEventLoop();
		eventLoop.addWindow(createRenderer.apply(boiler.window()));
		eventLoop.runMain();

		boiler.destroyInitialObjects();
	}
}
