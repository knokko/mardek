package com.github.knokko.vk2d.batch;

import com.github.knokko.vk2d.frame.Vk2dRenderStage;
import com.github.knokko.vk2d.pipeline.Vk2dMultiplyPipeline;

public class Vk2dMultiplyBatch extends Vk2dAbstractColorBatch {

	/**
	 * This constructor is for internal use only. Use {@link Vk2dMultiplyPipeline#addBatch}
	 */
	public Vk2dMultiplyBatch(Vk2dMultiplyPipeline pipeline, Vk2dRenderStage frame, int initialCapacity) {
		super(pipeline, frame, initialCapacity);
	}
}
