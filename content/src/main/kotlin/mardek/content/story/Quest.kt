package mardek.content.story

import com.github.knokko.bitser.BitPostInit
import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceFieldTarget

/**
 * Represents a quest, which is an entry in the Quests tab of the in-game menu.
 */
@BitStruct(backwardCompatible = true)
class Quest(
	/**
	 * The display name of the quest, which is shown in the quest list of the Quests tab of the in-game menu.
	 */
	@BitField(id = 0)
	val tabName: String,

	/**
	 * The title of the quest, which is shown above the quest description, when the quest is selected in the Quests
	 * tab of the in-game menu.
	 */
	@BitField(id = 1)
	val title: String,

	/**
	 * The full description of the quest, which is shown when the quest is selected in the Quests tab of the in-game
	 * menu.
	 */
	@BitField(id = 2)
	val description: String,
) : BitPostInit {

	/**
	 * The timeline variable that will be activated to mark this quest as *active* (visible).
	 */
	@BitField(id = 3)
	@ReferenceFieldTarget(label = "timeline variables")
	val isActive = FixedTimelineVariable<Unit>()

	/**
	 * The timeline variable that will be activated to mark this quest as completed. Timeline nodes that mark quests
	 * as completed should have `ignoresTimelineActivation = true`, to ensure the quest remains completed, even after
	 * the story moves on to the next chapter.
	 */
	@BitField(id = 4)
	@ReferenceFieldTarget(label = "timeline variables")
	val wasCompleted = FixedTimelineVariable<Unit>()

	init {
		assignDebugNames()
	}

	@Suppress("unused")
	private constructor() : this("", "", "")

	private fun assignDebugNames() {
		isActive.debugName = "quest $tabName is active"
		wasCompleted.debugName = "quest $tabName was completed"
	}

	override fun postInit(context: BitPostInit.Context) {
		assignDebugNames()
	}

	override fun toString() = "Quest($tabName)"
}
