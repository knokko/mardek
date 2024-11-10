package mardek.assets.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField

@BitStruct(backwardCompatible = false)
class AreaTrigger(
	/**
	 * Seems to be unused, but could be useful to keep around for debugging
	 */
	@BitField(ordering = 0)
	val name: String,

	@BitField(ordering = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	val x: Int,

	@BitField(ordering = 2)
	@IntegerField(expectUniform = false, minValue = 0)
	val y: Int,

	/**
	 * The flash code, mostly useful for development
	 */
	@BitField(ordering = 3)
	val flashCode: String,

	/**
	 * When true, the trigger can only happen once (per 'campaign'). When false, players can keep triggering it.
	 */
	@BitField(ordering = 4)
	val oneTimeOnly: Boolean,

	/**
	 * Only used for some complex triggers in the inventors lab. When true, the trigger is not `oneTimeOnly`, but it
	 * can't be triggered again until the player leaves and re-enters the map.
	 */
	@BitField(ordering = 5)
	val oncePerAreaLoad: Boolean,

	/**
	 * Seems to be true for most teleporting triggers. It means that it only triggers after the player
	 * **reaches** the tile. When `walkOn == null`, it is... complicated
	 */
	@BitField(ordering = 6, optional = true)
	val walkOn: Boolean?
) {

	@Suppress("unused")
	private constructor() : this("", 0, 0, "", false, false, null)

	override fun toString() = "Trigger(x=$x, y=$y, once=$oneTimeOnly, walkOn=$walkOn, code=$flashCode)"

	override fun equals(other: Any?) = other is AreaTrigger && name == other.name && x == other.x && y == other.y &&
			flashCode == other.flashCode && oneTimeOnly == other.oneTimeOnly &&
			oncePerAreaLoad == other.oncePerAreaLoad && walkOn == other.walkOn

	override fun hashCode(): Int {
		var result = x
		result = 31 * result + y
		result = 31 * result + flashCode.hashCode()
		return result
	}
}
