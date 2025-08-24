package mardek.renderer.battle

import com.github.knokko.vk2d.batch.Vk2dImageBatch

internal fun renderBattleBackground(battleContext: BattleRenderContext, batch: Vk2dImageBatch) {
	val background = battleContext.battle.battle.background
	batch.fillWithoutDistortion(
		0, 0, batch.width - 1, batch.height - 1, background.sprite.index,
		background.sprite.width, background.sprite.height
	)
}
