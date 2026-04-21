package mardek.renderer

import com.github.knokko.vk2d.Vk2dInstance
import com.github.knokko.vk2d.pipeline.Vk2dPipelineContext
import com.github.knokko.vk2d.pipeline.Vk2dPipelines
import mardek.renderer.animation.AnimationPartPipeline
import mardek.renderer.area.AreaLightPipeline
import mardek.renderer.area.AreaSpritePipeline
import mardek.renderer.area.water.SimpleWaterPipeline

class MardekPipelines(
	vk2d: Vk2dInstance, pipelineContext: Vk2dPipelineContext
) : Vk2dPipelines(vk2d, pipelineContext) {

	val simpleWater = SimpleWaterPipeline(pipelineContext, vk2d)
	val areaSprite = AreaSpritePipeline(pipelineContext, vk2d)
	val areaLight = AreaLightPipeline(pipelineContext, vk2d)
	val animation = AnimationPartPipeline(pipelineContext, vk2d)
}

