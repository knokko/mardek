package mardek.renderer.menu

import com.github.knokko.vk2d.batch.Vk2dColorBatch
import com.github.knokko.vk2d.batch.Vk2dFancyTextBatch
import com.github.knokko.vk2d.batch.Vk2dImageBatch
import com.github.knokko.vk2d.batch.Vk2dKim3Batch
import com.github.knokko.vk2d.batch.Vk2dOvalBatch
import com.github.knokko.vk2d.batch.Vk2dSimpleTextBatch
import mardek.renderer.RenderContext
import mardek.renderer.animation.AnimationPartBatch
import mardek.renderer.area.AreaSpriteBatch
import mardek.state.ingame.CampaignState
import mardek.state.ingame.menu.InGameMenuState
import mardek.state.ingame.menu.UiUpdateContext

internal val referenceTime = System.nanoTime()

internal class MenuRenderContext(
	val context: RenderContext,
	val colorBatch: Vk2dColorBatch,
	val ovalBatch: Vk2dOvalBatch,
	/**
	 * Only used for the encyclopedia icons
	 */
	val areaSpriteBatch: AreaSpriteBatch,
	val imageBatch: Vk2dImageBatch,
	val spriteBatch: Vk2dKim3Batch,
	val animationPartBatch: AnimationPartBatch,
	val lateColorBatch: Vk2dColorBatch,
	val simpleTextBatch: Vk2dSimpleTextBatch,
	val fancyTextBatch: Vk2dFancyTextBatch,
	val menu: InGameMenuState,
	val state: CampaignState,
) {

	val uiContext = UiUpdateContext(
		context.campaign.usedPartyMembers(), context.campaign.allPartyMembers(),
		context.state.soundQueue, context.content.audio.fixedEffects, context.content.skills,
		{ context.campaign.cursorItemStack },
		{ newCursorStack -> context.campaign.cursorItemStack = newCursorStack },
	)
}
