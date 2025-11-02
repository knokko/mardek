package com.github.knokko.vk2d.pipeline;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.pipelines.SimpleRenderPass;
import static org.lwjgl.vulkan.VK10.*;

public class Vk2dPipelineContext {

	public final BoilerInstance boiler;
	public final long vkRenderPass;
	public final int viewMask;
	public final int colorFormat;

	public boolean printBatchSizes;

	public Vk2dPipelineContext(BoilerInstance boiler, long vkRenderPass, int viewMask, int colorFormat) {
		this.boiler = boiler;
		this.vkRenderPass = vkRenderPass;
		this.viewMask = viewMask;
		this.colorFormat = colorFormat;
	}

	public static Vk2dPipelineContext renderPass(BoilerInstance boiler, long vkRenderPass) {
		return new Vk2dPipelineContext(boiler, vkRenderPass, 0, 0);
	}

	public static Vk2dPipelineContext dynamicRendering(
			BoilerInstance boiler, int viewMask, int colorFormat
	) {
		return new Vk2dPipelineContext(boiler, VK_NULL_HANDLE, viewMask, colorFormat);
	}

	public static Vk2dPipelineContext dynamicRendering(BoilerInstance boiler, int colorFormat) {
		return dynamicRendering(boiler, 0, colorFormat);
	}

	public static Vk2dPipelineContext renderPass(BoilerInstance boiler, int targetImageFormat) {
		return renderPass(boiler, SimpleRenderPass.create(
				boiler, "Vk2dRenderPass", null,
				new SimpleRenderPass.ColorAttachment(
						targetImageFormat, VK_ATTACHMENT_LOAD_OP_CLEAR,
						VK_ATTACHMENT_STORE_OP_STORE, VK_SAMPLE_COUNT_1_BIT
				)
		));
	}
}
