package com.github.knokko.vk2d.pipeline;

import com.github.knokko.boiler.pipelines.GraphicsPipelineBuilder;
import com.github.knokko.vk2d.Vk2dInstance;
import com.github.knokko.vk2d.frame.Vk2dRenderStage;
import com.github.knokko.vk2d.batch.Vk2dColorBatch;
import org.lwjgl.system.MemoryStack;

public class Vk2dColorPipeline extends Vk2dAbstractColorPipeline {

	public Vk2dColorPipeline(Vk2dPipelineContext context, Vk2dInstance instance) {
		super(context, instance.colorPipelineLayout);
	}

	/**
	 * @param initialCapacity The initial capacity of this batch, in <i>triangles</i>
	 */
	public Vk2dColorBatch addBatch(Vk2dRenderStage stage, int initialCapacity) {
		return new Vk2dColorBatch(this, stage, initialCapacity);
	}

	@Override
	protected void modifyPipelineSettings(MemoryStack stack, GraphicsPipelineBuilder pipelineBuilder) {}
}
