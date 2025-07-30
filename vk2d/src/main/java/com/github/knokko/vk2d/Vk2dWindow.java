package com.github.knokko.vk2d;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.buffers.PerFrameBuffer;
import com.github.knokko.boiler.builders.BoilerBuilder;
import com.github.knokko.boiler.builders.WindowBuilder;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.commands.SingleTimeCommands;
import com.github.knokko.boiler.descriptors.DescriptorCombiner;
import com.github.knokko.boiler.descriptors.DescriptorUpdater;
import com.github.knokko.boiler.memory.MemoryBlock;
import com.github.knokko.boiler.memory.MemoryCombiner;
import com.github.knokko.boiler.memory.callbacks.CallbackUserData;
import com.github.knokko.boiler.synchronization.ResourceUsage;
import com.github.knokko.boiler.window.*;
import com.github.knokko.vk2d.pipeline.Vk2dPipelineContext;
import com.github.knokko.vk2d.pipeline.Vk2dPipelines;
import com.github.knokko.vk2d.resource.Vk2dResourceBundle;
import com.github.knokko.vk2d.resource.Vk2dResourceLoader;
import com.github.knokko.vk2d.text.Vk2dTextBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import static com.github.knokko.boiler.utilities.BoilerMath.leastCommonMultiple;
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

	protected Vk2dInstance instance;
	protected Vk2dPipelines pipelines;
	private MemoryBlock memory;
	private long vkDescriptorPool, vkRenderPass;
	protected PerFrameBuffer perFrameBuffer;
	protected Vk2dResourceBundle resources;
	protected Vk2dTextBuffer textBuffer;
	protected long perFrameDescriptorSet;
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

	protected abstract void setupConfig(Vk2dConfig config);

	@Override
	protected void setup(BoilerInstance boiler, MemoryStack stack) {
		super.setup(boiler, stack);
		Vk2dConfig config = new Vk2dConfig();
		setupConfig(config);
		this.instance = new Vk2dInstance(boiler, config);

		Vk2dPipelineContext pipelineContext = Vk2dPipelineContext.renderPass(boiler, window.surfaceFormat);
		this.pipelines = new Vk2dPipelines(instance, pipelineContext, config);
		this.vkRenderPass = pipelineContext.vkRenderPass();

		try {
			InputStream resourceInput = initialResourceBundle();
			Vk2dResourceLoader loader = null;
			if (resourceInput != null) loader = new Vk2dResourceLoader(instance, resourceInput);

			MemoryCombiner combiner = new MemoryCombiner(boiler, "Vk2dPersistent");
			DescriptorCombiner descriptors = new DescriptorCombiner(boiler);
			if (loader != null) loader.claimMemory(combiner);
			createResources(boiler, combiner, descriptors);
			if (config.text) this.textBuffer = new Vk2dTextBuffer(instance, combiner, descriptors, numFramesInFlight);
			if (config.oval) descriptors.addSingle(
					instance.bufferDescriptorSetLayout,
					descriptorSet -> this.perFrameDescriptorSet = descriptorSet
			);
			this.memory = combiner.build(false);

			if (loader != null) {
				loader.prepareStaging();
				Vk2dResourceLoader[] pLoader = { loader };
				SingleTimeCommands.submit(boiler, "Vk2dStaging", recorder ->
						pLoader[0].performStaging(recorder, descriptors)
				).destroy();
			}

			this.vkDescriptorPool = descriptors.build("Vk2dDescriptors");
			if (loader != null) this.resources = loader.finish();
			if (resourceInput != null) resourceInput.close();
		} catch (IOException io) {
			throw new RuntimeException(io);
		}

		if (textBuffer != null) textBuffer.initializeDescriptorSets();
		if (perFrameDescriptorSet != VK_NULL_HANDLE) {
			DescriptorUpdater updater = new DescriptorUpdater(stack, 1);
			updater.writeStorageBuffer(0, perFrameDescriptorSet, 0, perFrameBuffer.buffer);
			updater.update(boiler);
		}

		this.framebuffers = new SwapchainResourceManager<>() {

			@Override
			protected Long createImage(Object swapchain, AcquiredImage swapchainImage) {
				return boiler.images.createFramebuffer(
						vkRenderPass, swapchainImage.width(), swapchainImage.height(),
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

	protected void createResources(BoilerInstance boiler, MemoryCombiner combiner, DescriptorCombiner descriptors) {
		Set<Long> alignment = new HashSet<>();
		alignment.add(4L);
		alignment.add(boiler.deviceProperties.limits().minStorageBufferOffsetAlignment());
		this.perFrameBuffer = new PerFrameBuffer(combiner.addMappedBuffer(
				10_000_000L, leastCommonMultiple(alignment),
				VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT
		));
	}

	@Override
	protected void recordFrame(
			MemoryStack stack, int frameIndex, CommandRecorder recorder,
			AcquiredImage acquiredImage, BoilerInstance boiler
	) {
		perFrameBuffer.startFrame(frameIndex);
		if (textBuffer != null) textBuffer.startFrame();
		Vk2dFrame frame = new Vk2dFrame(perFrameBuffer, acquiredImage.width(), acquiredImage.height());
		renderFrame(frame, recorder, acquiredImage, boiler);
		if (textBuffer != null) textBuffer.transfer(recorder);

		VkRenderPassBeginInfo biRenderPass = VkRenderPassBeginInfo.calloc(stack);
		biRenderPass.sType$Default();
		biRenderPass.renderPass(vkRenderPass);
		biRenderPass.framebuffer(framebuffers.get(acquiredImage));
		biRenderPass.renderArea().extent().set(acquiredImage.width(), acquiredImage.height());
		biRenderPass.pClearValues(VkClearValue.calloc(1, stack));
		biRenderPass.clearValueCount(1);

		vkCmdBeginRenderPass(recorder.commandBuffer, biRenderPass, VK_SUBPASS_CONTENTS_INLINE);
		recorder.dynamicViewportAndScissor(acquiredImage.width(), acquiredImage.height());
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
					boiler.vkDevice(), vkRenderPass,
					CallbackUserData.RENDER_PASS.put(stack, boiler)
			);
			vkDestroyDescriptorPool(
					boiler.vkDevice(), vkDescriptorPool,
					CallbackUserData.DESCRIPTOR_POOL.put(stack, boiler)
			);
		}
		memory.destroy(boiler);
		pipelines.destroy();
		instance.destroy();
	}

	public static void bootstrap(
			String title, int appVersion, Vk2dValidationMode validationMode,
			Function<VkbWindow, WindowRenderLoop> createRenderer
	) {
		BoilerBuilder builder = new BoilerBuilder(
				validationMode == Vk2dValidationMode.STRONG ? VK_API_VERSION_1_3 : VK_API_VERSION_1_0, title, appVersion
		);
		builder.requiredFeatures10("textureCompressionBc", VkPhysicalDeviceFeatures::textureCompressionBC);
		builder.featurePicker10((stack, supported, toEnable) -> toEnable.textureCompressionBC(true));
		if (validationMode != Vk2dValidationMode.NONE) builder.validation();
		if (validationMode == Vk2dValidationMode.STRONG) builder.forbidValidationErrors();
		BoilerInstance boiler = builder.addWindow(
				new WindowBuilder(
						1200, 800, VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT
				).sdlFlags(SDL_WINDOW_VULKAN | SDL_WINDOW_MAXIMIZED | SDL_WINDOW_RESIZABLE).hideUntilFirstFrame()
		).useSDL().doNotUseVma().build();

		WindowEventLoop eventLoop = new WindowEventLoop();
		eventLoop.addWindow(createRenderer.apply(boiler.window()));
		eventLoop.runMain();

		boiler.destroyInitialObjects();
	}
}
