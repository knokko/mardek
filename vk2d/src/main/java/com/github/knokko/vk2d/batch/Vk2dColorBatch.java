package com.github.knokko.vk2d.batch;

import com.github.knokko.vk2d.frame.Vk2dRenderStage;
import com.github.knokko.vk2d.pipeline.Vk2dColorPipeline;

/**
 * This is the batch class of {@link com.github.knokko.vk2d.pipeline.Vk2dColorPipeline}. See the color pipeline docs
 * (link is in the README) for more information.
 */
public class Vk2dColorBatch extends Vk2dAbstractColorBatch {

	/**
	 * This constructor is for internal use only. Use {@link com.github.knokko.vk2d.pipeline.Vk2dColorPipeline#addBatch}
	 */
	public Vk2dColorBatch(Vk2dColorPipeline pipeline, Vk2dRenderStage frame, int initialCapacity) {
		super(pipeline, frame, initialCapacity);
	}
}
