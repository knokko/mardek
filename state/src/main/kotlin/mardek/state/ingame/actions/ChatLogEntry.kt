package mardek.state.ingame.actions

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.stats.Element
import mardek.state.saves.savesBitser

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

	override fun equals(other: Any?) = savesBitser.deepEquals(this, other)

	override fun hashCode() = savesBitser.hashCode(this)

	override fun toString() = "ChatLogEntry($speaker, $speakerElement, $text)"
}
