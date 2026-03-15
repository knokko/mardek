package mardek.state.ingame.battle

/**
 * An indicator that a player got a level-up during a battle
 */
class LevelUpIndicator(

	/**
	 * The result of `System.nanoTime()` at the time of the level-up
	 */
	val startTime: Long,

	/**
	 * The new level of the combatant
	 */
	val newLevel: Int,
)
