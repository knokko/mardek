package mardek.state.ingame.story

import mardek.content.story.Quest

/**
 * A 'snapshot' of the active & completed quests
 */
class QuestsSnapshot(

	/**
	 * The quests that were active when this snapshot was created
	 */
	val active: Array<Quest>,

	/**
	 * The quests that were completed when this snapshot was created
	 */
	val completed: Array<Quest>,
) {
	override fun toString() = "Quests(active=${active.contentToString()}, completed=${completed.contentToString()})"
}
