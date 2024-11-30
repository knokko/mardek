package mardek.assets.skill

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.CollectionField
import com.github.knokko.bitser.field.IntegerField

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
	@CollectionField(optionalValues = true)
	val notes: List<SirenNote?>,
) {
}

@BitStruct(backwardCompatible = false)
class SirenNote(
	@BitField(ordering = 0)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 12)
	val value1: Int,

	@BitField(ordering = 0)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 12)
	val value2: Int,
) {
	override fun toString() = if (value1 == value2) value1.toString() else "[$value1, $value2]"

	override fun equals(other: Any?) = other is SirenNote && value1 == other.value1 && value2 == other.value2

	override fun hashCode() = 123 * value1 + value2
}
