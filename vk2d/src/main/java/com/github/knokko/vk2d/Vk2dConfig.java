package com.github.knokko.vk2d;

public class Vk2dConfig {

	/**
	 * Set this to {@code true} to enable the {@link com.github.knokko.vk2d.pipeline.Vk2dColorPipeline}
	 */
	public boolean color;

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

	public boolean shouldCreateBufferPipelineLayout() {
		return kim1 || kim2 || kim3 || oval;
	}
}
