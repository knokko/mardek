package mardek.renderer.battle

import com.github.knokko.vk2d.batch.Vk2dImageBatch

internal fun renderBattleBackground(battleContext: BattleRenderContext, batch: Vk2dImageBatch) {
	val background = battleContext.battle.battle.background
	batch.fillWithoutDistortion(
		0f, 0f, batch.width.toFloat(),
		batch.height.toFloat(), background.sprite.index
	)
}
