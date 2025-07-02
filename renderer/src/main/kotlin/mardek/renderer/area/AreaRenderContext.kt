package mardek.renderer.area

import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dImageBatch
import com.github.knokko.vk2d.batch.Vk2dOvalBatch
import mardek.renderer.RenderContext
import mardek.renderer.animation.AnimationPartBatch
import mardek.renderer.glyph.MardekGlyphBatch
import mardek.state.ingame.area.AreaState
import mardek.state.util.Rectangle

internal class AreaRenderContext(
	val context: RenderContext,
	val state: AreaState,
	val scale: Int,
	val region: Rectangle,
	val spriteBatch: AreaSpriteBatch,
	val portraitBackgroundBatch: Vk2dColorBatch,
	val portraitBatch: AnimationPartBatch,
	val colorBatch: Vk2dColorBatch,
	val imageBatch: Vk2dImageBatch,
	val ovalBatch: Vk2dOvalBatch,
	val textBatch: MardekGlyphBatch,
	val scissorLeft: Int,
	val scissor: Rectangle,
) {
	var cameraX = 0
	var cameraY = 0
	val area = state.area
	val tileSize = 16 * scale

	val renderJobs = mutableListOf<SpriteRenderJob>()
}
