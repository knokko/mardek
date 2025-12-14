package com.github.knokko.vk2d;

import com.github.knokko.vk2d.pipeline.Vk2dBlurPipeline;

public class Vk2dConfig {

	/**
	 * Set this to {@code true} to enable the {@link com.github.knokko.vk2d.pipeline.Vk2dColorPipeline}
	 */
	public boolean color;

	/**
	 * Set this to {@code true} to enable the {@link com.github.knokko.vk2d.pipeline.Vk2dMultiplyPipeline}
	 */
	public boolean multiply;

	/**
	 * Set this to {@code true} to enable the {@link com.github.knokko.vk2d.pipeline.Vk2dOvalPipeline}
	 */
	public boolean oval;

	/**
	 * Set this to {@code true} to enable the {@link com.github.knokko.vk2d.pipeline.Vk2dImagePipeline}
	 */
	public boolean image;

	/**
	 * Set this to {@code true} to enable the {@link com.github.knokko.vk2d.pipeline.Vk2dGlyphPipeline}
	 */
	public boolean text;

	/**
	 * Set these to {@code true} to enable the {@link com.github.knokko.vk2d.pipeline.Vk2dKimPipeline}s
	 */
	public boolean kim1, kim2, kim3;

	/**
	 * Set this to {@code true} to enable the {@link Vk2dBlurPipeline}
	 */
	public boolean blur;

	/**
	 * Whether the {@link Vk2dInstance#colorPipelineLayout} should be created
	 */
	public boolean shouldCreateColorPipelineLayout() {
		return color || multiply;
	}

	/**
	 * Whether the {@link Vk2dInstance#singleBufferPipelineLayout} should be created
	 */
	public boolean shouldCreateBufferPipelineLayout() {
		return kim1 || kim2 || oval;
	}
}
