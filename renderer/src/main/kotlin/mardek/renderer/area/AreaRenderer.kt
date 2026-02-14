package mardek.renderer.area

import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dGlyphBatch
import mardek.renderer.RenderContext
import mardek.renderer.area.ui.renderChestLoot
import mardek.renderer.area.ui.renderDialogue
import mardek.state.ingame.area.AreaState
import mardek.state.util.Rectangle
import kotlin.math.max
import kotlin.math.roundToInt

internal fun renderCurrentArea(
	context: RenderContext, state: AreaState, region: Rectangle
): Pair<Vk2dColorBatch, Vk2dGlyphBatch> {

	val baseVisibleHorizontalTiles = region.width / 16.0
	val baseVisibleVerticalTiles = region.height / 16.0

	// The original MARDEK allow players to see at most 5 tiles above/below the player,
	// and at most 7 tiles left/right from the player.

	// I will aim for 6 tiles above/below the player, and let the aspect ratio determine the number of tiles
	// that can be seen left/right from the player, within reason.
	val floatScale = baseVisibleVerticalTiles / 13.0

	// Use integer scales to keep the tiles pretty
	val scale = max(1, floatScale.roundToInt())

	// Without restrictions, players with very wide screens/windows could see way too many tiles left/right
	// from the player. I will enforce a maximum of 14.5 tiles left/right, which is already ridiculous.
	val maxVisibleHorizontalTiles = 30.0
	val visibleHorizontalTiles = baseVisibleHorizontalTiles / scale

	var scissorLeft = 0
	var scissor = region
	if (visibleHorizontalTiles > maxVisibleHorizontalTiles) {
		scissorLeft = (region.width * ((visibleHorizontalTiles - maxVisibleHorizontalTiles) / visibleHorizontalTiles) / 2.0).roundToInt()
		scissor = Rectangle(region.minX + scissorLeft, region.minY, region.width - 2 * scissorLeft, region.height)
	}

	val simpleWaterBatch = context.addSimpleWaterBatch(1000, scissor, scale)
	val spriteBatch = context.addAreaSpriteBatch(3000, scissor)
	val multiplyBatch = context.addMultiplyBatch(2)
	val lateSpriteBatch = context.addAreaSpriteBatch(2, scissor)
	val lightBatch = context.addAreaLightBatch(scissor)
	val colorBatch = context.addColorBatch(600)
	val ovalBatch = context.addOvalBatch(24)
	val imageBatch = context.addImageBatch(2)
	val textBatch = context.addFancyTextBatch(1000)
	val portraitBatch = context.addAnimationPartBatch(200)
	val areaContext = AreaRenderContext(
		context, state, scale, region,
		simpleWaterBatch, spriteBatch, multiplyBatch,
		lateSpriteBatch, lightBatch, portraitBatch,
		colorBatch, imageBatch, ovalBatch, textBatch,
		scissorLeft, scissor
	)

	collectAreaObjects(areaContext)
	collectAreaCharacters(areaContext)
	renderTiles(areaContext)
	collectIncomingBattleIndicator(areaContext)

	for (job in areaContext.renderJobs) job.addToBatch(areaContext)

	renderObtainedGold(areaContext)
	renderAreaLights(areaContext)
	renderAreaAmbience(areaContext)
	renderChestLoot(areaContext)
	renderDialogue(areaContext)
	renderActionFlash(areaContext)
	renderActionOverlayColor(areaContext)
	renderAreaFadeEffects(areaContext)
	renderAreaIncomingBattleFlicker(areaContext)

	return Pair(areaContext.colorBatch, areaContext.textBatch)
}
