package mardek.assets.area.objects

import com.github.knokko.bitser.BitEnum
import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField

abstract class AreaSwitch(
	@BitField(ordering = 0)
	val color: SwitchColor,

	@BitField(ordering = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	val x: Int,

	@BitField(ordering = 2)
	@IntegerField(expectUniform = false, minValue = 0)
	val y: Int,
) {

	@BitField(ordering = 3)
	@IntegerField(expectUniform = false, minValue = -1)
	var offSpriteOffset = -1

	@BitField(ordering = 4)
	@IntegerField(expectUniform = false, minValue = -1)
	var onSpriteOffset = -1

	init {
		if (color == SwitchColor.Off) throw IllegalArgumentException("Off switch is not allowed")
	}

	override fun toString() = "${this::class.java.simpleName.substring(4)}($color, x=$x, y=$y)"

	override fun equals(other: Any?) = other is AreaSwitch && this::class.java == other::class.java &&
			color == other.color && x == other.x && y == other.y

	override fun hashCode(): Int {
		var result = x
		result = 31 * result + y
		result = 31 * result + color.hashCode()
		return result
	}
}

@BitStruct(backwardCompatible = false)
class AreaSwitchOrb(color: SwitchColor, x: Int, y: Int): AreaSwitch(color, x, y) {

	@Suppress("unused")
	private constructor() : this(SwitchColor.UnusedBlue, 0, 0)
}

@BitStruct(backwardCompatible = false)
class AreaSwitchGate(color: SwitchColor, x: Int, y: Int): AreaSwitch(color, x, y) {

	@Suppress("unused")
	private constructor() : this(SwitchColor.UnusedBlue, 0, 0)
}

@BitStruct(backwardCompatible = false)
class AreaSwitchPlatform(color: SwitchColor, x: Int, y: Int): AreaSwitch(color, x, y) {

	@Suppress("unused")
	private constructor() : this(SwitchColor.UnusedBlue, 0, 0)
}

@Suppress("unused")
@BitEnum(mode = BitEnum.Mode.VariableIntOrdinal)
enum class SwitchColor {
	Off,
	Ruby,
	Amethyst,
	Moonstone,
	Emerald,
	Topaz,
	Turquoise,
	Sapphire,
	UnusedBlue
}
