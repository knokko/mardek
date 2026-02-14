package mardek.renderer

import com.github.knokko.boiler.BoilerInstance
import com.github.knokko.vk2d.pipeline.Vk2dPipelineContext
import com.github.knokko.vk2d.pipeline.Vk2dPipelines
import mardek.renderer.animation.AnimationPartPipeline
import mardek.renderer.area.AreaLightPipeline
import mardek.renderer.area.AreaSpritePipeline
import mardek.renderer.area.water.SimpleWaterPipeline
import mardek.renderer.glyph.MardekGlyphPipeline

class MardekPipelines(
	val base: Vk2dPipelines, pipelineContext: Vk2dPipelineContext
) {
	val fancyText = MardekGlyphPipeline(pipelineContext, base.instance)
	val simpleWater = SimpleWaterPipeline(pipelineContext, base.instance)
	val areaSprite = AreaSpritePipeline(pipelineContext, base.instance)
	val areaLight = AreaLightPipeline(pipelineContext, base.instance)
	val animation = AnimationPartPipeline(pipelineContext, base.instance)

	fun destroy(boiler: BoilerInstance) {
		animation.destroy(boiler)
		areaLight.destroy(boiler)
		areaSprite.destroy(boiler)
		simpleWater.destroy(boiler)
		fancyText.destroy(boiler)
	}
}
