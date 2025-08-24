package mardek.renderer.menu

import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dImageBatch
import com.github.knokko.vk2d.batch.Vk2dKimBatch
import com.github.knokko.vk2d.batch.Vk2dOvalBatch
import mardek.renderer.RenderContext
import mardek.renderer.glyph.MardekGlyphBatch
import mardek.state.ingame.CampaignState
import mardek.state.ingame.menu.InGameMenuState
import mardek.state.ingame.menu.UiUpdateContext

internal val referenceTime = System.nanoTime()

internal class MenuRenderContext(
	val context: RenderContext,
	val colorBatch: Vk2dColorBatch,
	val ovalBatch: Vk2dOvalBatch,
	val imageBatch: Vk2dImageBatch,
	val spriteBatch: Vk2dKimBatch,
	val textBatch: MardekGlyphBatch,
	val menu: InGameMenuState,
	val state: CampaignState,
) {

	val uiContext = UiUpdateContext(
		state.characterSelection, state.characterStates,
		context.state.soundQueue, context.content.audio.fixedEffects, context.content.skills
	)
}
