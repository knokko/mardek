package mardek.renderer.area

import mardek.renderer.RenderContext
import mardek.state.ingame.area.AreaState
import mardek.state.util.Rectangle

internal class AreaRenderContext(
	val context: RenderContext,
	val state: AreaState,
	val scale: Int,
	val region: Rectangle,
	val scissorLeft: Int,
	val scissor: Rectangle,
) {
	var cameraX = 0
	var cameraY = 0
	val area = state.area
	val tileSize = 16 * scale

	val renderJobs = mutableListOf<SpriteRenderJob>()

	val simpleWaterBatch = context.addSimpleWaterBatch(1000, scissor, scale)

	/**
	 * Used for all tiles and area characters
	 */
	val spriteBatch = context.addAreaSpriteBatch(3000, scissor)
	val lightBatch = context.addAreaLightBatch(scissor)

	/**
	 * Used for area ambience
	 */
	val multiplyBatch = context.addMultiplyBatch(2)

	/**
	 * Used for rendering obtained gold
	 */
	val goldSpriteBatch = context.addAreaSpriteBatch(2, scissor)
	val actionsImageBatch = context.addImageBatch(2)

	/**
	 * Used for chest loot and dialogues
	 */
	val uiColorBatch = context.addColorBatch(600)
	val dialogueOvalBatch = context.addOvalBatch(24)
	val dialogueElementBatch = context.addImageBatch(2)
	val textBatch = context.addFancyTextBatch(1000)
	val portraitBatch = context.addAnimationPartBatch(200)
}
