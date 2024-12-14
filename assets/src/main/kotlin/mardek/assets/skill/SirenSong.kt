package mardek.assets.skill

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting

@BitStruct(backwardCompatible = false)
class SirenSong(
	@BitField(ordering = 0)
	val name: String,

	@BitField(ordering = 1)
	@IntegerField(expectUniform = true, minValue = 1, maxValue = 2)
	val time: Int,

	@BitField(ordering = 2)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 2)
	val tempo: Int,

	@BitField(ordering = 3)
	@NestedFieldSetting(path = "c", optional = true)
	val notes: ArrayList<SirenNote?>,
) {

	@Suppress("unused")
	private constructor() : this("", 0, 0, ArrayList(0))
}

@BitStruct(backwardCompatible = false)
class SirenNote(
	@BitField(ordering = 0)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 16)
	val value1: Int,

	@BitField(ordering = 1)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 16)
	val value2: Int,
) {

	@Suppress("unused")
	private constructor() : this(0, 0)

	override fun toString() = if (value1 == value2) value1.toString() else "[$value1, $value2]"

	override fun equals(other: Any?) = other is SirenNote && value1 == other.value1 && value2 == other.value2

	override fun hashCode() = 123 * value1 + value2
}
