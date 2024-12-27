package mardek.state.ingame.area.loot

import kotlin.time.Duration

class ObtainedGold(
	val chestX: Int,
	val chestY: Int,
	val amount: Int,
	val showUntil: Duration
) {

	override fun toString() = "ObtainedGold($amount, x=$chestX, y=$chestY)"
}
