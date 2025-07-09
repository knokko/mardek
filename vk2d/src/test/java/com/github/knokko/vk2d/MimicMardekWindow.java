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
import com.github.knokko.vk2d.pipeline.Vk2dColorPipeline;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;

import static com.github.knokko.boiler.utilities.ColorPacker.rgb;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_IMMEDIATE_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK12.VK_API_VERSION_1_2;

public class MimicMardekWindow extends SimpleWindowRenderLoop {

	private MemoryBlock memory;
	private PerFrameBuffer perFrameBuffer;
	private PipelineContext pipelineContext;
	private Vk2dColorPipeline colorPipeline;
	private SwapchainResourceManager<Object, Long> framebuffers;

	public MimicMardekWindow(VkbWindow window) {
		super(
				window, 2, false,
				window.supportedPresentModes.contains(VK_PRESENT_MODE_MAILBOX_KHR) ?
						VK_PRESENT_MODE_MAILBOX_KHR : VK_PRESENT_MODE_IMMEDIATE_KHR,
				ResourceUsage.COLOR_ATTACHMENT_WRITE, ResourceUsage.COLOR_ATTACHMENT_WRITE
		);
	}

	@Override
	protected void setup(BoilerInstance boiler, MemoryStack stack) {
		super.setup(boiler, stack);

		MemoryCombiner combiner = new MemoryCombiner(boiler, "AllMemory");
		this.perFrameBuffer = new PerFrameBuffer(combiner.addMappedBuffer(
				10_000L, 4L, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT
		));
		this.memory = combiner.build(false);
		this.pipelineContext = PipelineContext.renderPass(boiler, window.surfaceFormat);
		this.colorPipeline = new Vk2dColorPipeline(pipelineContext);
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

	@Override
	protected void recordFrame(
			MemoryStack stack, int frameIndex, CommandRecorder recorder,
			AcquiredImage acquiredImage, BoilerInstance instance
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

		Vk2dBatch colorBatch1 = frame.addBatch(colorPipeline, 6);
		colorPipeline.fill(colorBatch1, 10, 20, 30, 40, rgb(255, 0, 255));

		frame.record(recorder);
		vkCmdEndRenderPass(recorder.commandBuffer);
	}

	@Override
	protected void cleanUp(BoilerInstance boiler) {
		super.cleanUp(boiler);
		colorPipeline.destroy(boiler);
		try (MemoryStack stack = stackPush()) {
			vkDestroyRenderPass(
					boiler.vkDevice(), pipelineContext.vkRenderPass(),
					CallbackUserData.RENDER_PASS.put(stack, boiler)
			);
		}
		memory.destroy(boiler);
	}

	public static void main(String[] args) {
		BoilerInstance boiler = new BoilerBuilder(
				VK_API_VERSION_1_2, "MimicMardekWindow", 1
		).validation().forbidValidationErrors().addWindow(
				new WindowBuilder(1600, 900, VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
		).useSDL().build();

		WindowEventLoop eventLoop = new WindowEventLoop();
		eventLoop.addWindow(new MimicMardekWindow(boiler.window()));
		eventLoop.runMain();

		boiler.destroyInitialObjects();
	}
}
