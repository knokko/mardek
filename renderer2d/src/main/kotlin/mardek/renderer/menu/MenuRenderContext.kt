package mardek.renderer.menu

import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dImageBatch
import com.github.knokko.vk2d.batch.Vk2dKimBatch
import mardek.renderer.RenderContext
import mardek.renderer.glyph.MardekGlyphBatch
import mardek.state.ingame.CampaignState
import mardek.state.ingame.menu.InGameMenuState

internal val referenceTime = System.nanoTime()

internal class MenuRenderContext(
	val context: RenderContext,
	val colorBatch1: Vk2dColorBatch,
	val imageBatch: Vk2dImageBatch,
	val spriteBatch: Vk2dKimBatch,
	val textBatch: MardekGlyphBatch,
	val menu: InGameMenuState,
	val state: CampaignState,
) {
}
