package mardek.renderer.area

import mardek.renderer.RenderContext
import mardek.state.util.RenderTiming
import mardek.state.ingame.area.AreaState
import mardek.state.util.Rectangle
import kotlin.math.max
import kotlin.math.roundToInt

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

	val areaTimings = RenderTiming(
		state.currentTime,
		context.timing.renderNanoTime,
		context.timing.extrapolationLimit,
	)

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

	/**
	 * Used for rendering obtained/lost items during dialogues
	 */
	val itemNotificationBatch = context.addAreaSpriteBatch(2, scissor)

	/**
	 * Used during dialogue rendering, and for rendering special effects like souls
	 */
	val ovalBatch = context.addOvalBatch(40)
	val dialogueElementBatch = context.addImageBatch(2)
	val simpleTextBatch = context.addTextBatch(2500)
	val fancyTextBatch = context.addFancyTextBatch(20)
	val portraitBatch = context.addAnimationPartBatch(200)

	companion object {

		fun create(context: RenderContext, state: AreaState, region: Rectangle): AreaRenderContext {
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

			val scissorLeft: Int
			val scissor: Rectangle
			if (visibleHorizontalTiles > maxVisibleHorizontalTiles) {
				scissorLeft = (region.width * ((visibleHorizontalTiles - maxVisibleHorizontalTiles) / visibleHorizontalTiles) / 2.0).roundToInt()
				scissor = Rectangle(region.minX + scissorLeft, region.minY, region.width - 2 * scissorLeft, region.height)
			} else {
				scissorLeft = 0
				scissor = region
			}

			return AreaRenderContext(context, state, scale, region, scissorLeft, scissor)
		}
	}
}
