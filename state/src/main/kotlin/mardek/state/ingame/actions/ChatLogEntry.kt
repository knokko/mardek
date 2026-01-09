package mardek.state.ingame.actions

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.stats.Element

/**
 * Represents an entry of [AreaActionsState.chatLog].
 */
@BitStruct(backwardCompatible = true)
class ChatLogEntry(

	/**
	 * The display name of the speaker of the dialogue message (usually a human)
	 */
	@BitField(id = 0)
	val speaker: String,

	/**
	 * The element of the speaker of the dialogue message, or `null` if the speaker doesn't have an element
	 * (e.g. a save crystal)
	 */
	@BitField(id = 1, optional = true)
	@ReferenceField(stable = true, label = "elements")
	val speakerElement: Element?,

	/**
	 * The message that the speaker said
	 */
	@BitField(id = 2)
	val text: String
) {

	@Suppress("unused")
	private constructor() : this("", null, "")
}
