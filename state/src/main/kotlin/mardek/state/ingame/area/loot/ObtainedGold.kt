package mardek.state.ingame.area.loot

import mardek.content.util.Time
import kotlin.time.Duration.Companion.seconds

/**
 * An instance of this class will be created when the player opens a chest containing gold, and put in
 * [mardek.state.ingame.area.AreaState.obtainedGold]. When the renderer sees that the `obtainedGold != null`, it will
 * render the gold icon and the amount at the location of the chest.
 */
class ObtainedGold(

	/**
	 * The X-coordinate of the tile containing the opened chest
	 */
	val chestX: Int,

	/**
	 * The Y-coordinate of the tile containing the opened chest
	 */
	val chestY: Int,

	/**
	 * The amount of gold that was in the chest
	 */
	val amount: Int,

	/**
	 * The gold icon should be shown as long as `areaState.currentTime <= shownSince + DURATION`
	 */
	val shownSince: Time,
) {

	override fun toString() = "ObtainedGold($amount, x=$chestX, y=$chestY)"

	companion object {

		val DURATION = 1.seconds
	}
}
