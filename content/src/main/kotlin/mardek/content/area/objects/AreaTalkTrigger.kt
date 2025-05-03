package mardek.content.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import mardek.content.BITSER

@BitStruct(backwardCompatible = true)
class AreaTalkTrigger(

	@BitField(id = 0)
	val name: String,

	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	val x: Int,

	@BitField(id = 2)
	@IntegerField(expectUniform = false, minValue = 0)
	val y: Int,

	@BitField(id = 3)
	val npcName: String,
) {

	@Suppress("unused")
	private constructor() : this("", 0, 0, "")

	override fun toString() = "TalkTrigger($name, x=$x, y=$y, npc=$npcName)"

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)
}
