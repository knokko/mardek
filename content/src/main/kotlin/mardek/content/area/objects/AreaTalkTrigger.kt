package mardek.content.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import mardek.content.BITSER

/**
 * When a talk trigger is placed at (x, y), the player can interact with the tile (x, y), causing the player to
 * interact with the NPC linked by the talk trigger.
 */
@BitStruct(backwardCompatible = true)
class AreaTalkTrigger(

	/**
	 * The name of the talk trigger, as imported from Flash. I don't know what its purpose is.
	 */
	@BitField(id = 0)
	val name: String,

	/**
	 * The X-coordinate of the tile containing this trigger
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	val x: Int,

	/**
	 * The Y-coordinate of the tile containing this trigger
	 */
	@BitField(id = 2)
	@IntegerField(expectUniform = false, minValue = 0)
	val y: Int,

	/**
	 * The name of the NPC linked by this trigger. I should probably turn this into a reference when I actually start
	 * using talk triggers...
	 */
	@BitField(id = 3)
	val npcName: String,
) {

	@Suppress("unused")
	private constructor() : this("", 0, 0, "")

	override fun toString() = "TalkTrigger($name, x=$x, y=$y, npc=$npcName)"

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)
}
