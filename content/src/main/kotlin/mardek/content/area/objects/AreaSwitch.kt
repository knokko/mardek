package mardek.content.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.BITSER

abstract class AreaSwitch(
	@BitField(id = 0)
	@ReferenceField(stable = false, label = "switch colors")
	val color: SwitchColor,

	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	val x: Int,

	@BitField(id = 2)
	@IntegerField(expectUniform = false, minValue = 0)
	val y: Int,
) {

	override fun toString() = "${this::class.java.simpleName.substring(4)}($color, x=$x, y=$y)"

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)
}

@BitStruct(backwardCompatible = true)
class AreaSwitchOrb(color: SwitchColor, x: Int, y: Int): AreaSwitch(color, x, y) {

	@Suppress("unused")
	private constructor() : this(SwitchColor(), 0, 0)
}

@BitStruct(backwardCompatible = true)
class AreaSwitchGate(color: SwitchColor, x: Int, y: Int): AreaSwitch(color, x, y) {

	@Suppress("unused")
	private constructor() : this(SwitchColor(), 0, 0)
}

@BitStruct(backwardCompatible = true)
class AreaSwitchPlatform(color: SwitchColor, x: Int, y: Int): AreaSwitch(color, x, y) {

	@Suppress("unused")
	private constructor() : this(SwitchColor(), 0, 0)
}
