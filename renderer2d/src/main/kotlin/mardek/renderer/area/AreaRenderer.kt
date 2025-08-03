package mardek.renderer.area

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.vk2d.batch.Vk2dColorBatch
import mardek.renderer.RenderContext
import mardek.state.ingame.area.AreaState
import mardek.state.util.Rectangle

internal fun renderCurrentArea(context: RenderContext, state: AreaState, region: Rectangle, colorBatch: Vk2dColorBatch) {
	colorBatch.fill(100, 200, 300, 400, rgb(250, 0, 0))

	val kimBatch = context.addKim3Batch()
	val position = state.getPlayerPosition(0)
	val testTile = state.area.getTile(position.x, position.y)
	kimBatch.simple(400, 500, 463, 563, testTile.sprites[0].offset)
}
