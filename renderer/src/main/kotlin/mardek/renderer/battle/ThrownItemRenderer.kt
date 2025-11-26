package mardek.renderer.battle

import com.github.knokko.vk2d.batch.Vk2dKim3Batch
import mardek.state.ingame.battle.BattleStateMachine
import kotlin.math.pow
import kotlin.math.roundToInt

internal fun renderThrownItems(battleContext: BattleRenderContext, kimBatch: Vk2dKim3Batch) {
	battleContext.run {
		val stateMachine = battle.state
		if (stateMachine !is BattleStateMachine.UseItem) return

		val passedTime = renderTime - stateMachine.startTime
		val rotationPeriod = 1000_000_000L
		val relativeTime = passedTime.toFloat() / rotationPeriod.toFloat()

		val itemX = relativeTime * stateMachine.target.renderInfo.core.x +
				(1f - relativeTime) * stateMachine.thrower.renderInfo.core.x
		var itemY = relativeTime * stateMachine.target.renderInfo.core.y +
				(1f - relativeTime) * stateMachine.thrower.renderInfo.core.y

		val throwHeight = 1f - 4f * (relativeTime - 0.5f).pow(2)
		itemY -= 0.2f * kimBatch.height * throwHeight

		val sprite = stateMachine.item.sprite
		val scale = kimBatch.height / 200f
		kimBatch.rotated(itemX, itemY, 360f * relativeTime, scale, sprite.index)
	}
}
