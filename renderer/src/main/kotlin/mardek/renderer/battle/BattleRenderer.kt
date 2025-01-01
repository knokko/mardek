package mardek.renderer.battle

import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.images.VkbImage
import mardek.renderer.SharedResources
import mardek.state.ingame.battle.BattleState

class BattleRenderer(
	private val recorder: CommandRecorder,
	private val targetImage: VkbImage,
	private val state: BattleState,
	private val resources: SharedResources,
) {

	fun render(frameIndex: Int) {
		val uiRenderer = resources.uiRenderers[frameIndex]
		uiRenderer.beginBatch()

		val background = resources.bcImages[state.battle.background.sprite.index]
		uiRenderer.drawImage(background, 0, 0, targetImage.width, targetImage.height)

		uiRenderer.endBatch()
	}
}
