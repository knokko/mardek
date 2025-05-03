package mardek.content.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import mardek.content.BITSER

@BitStruct(backwardCompatible = true)
class AreaTrigger(
	/**
	 * Seems to be unused, but could be useful to keep around for debugging
	 */
	@BitField(id = 0)
	val name: String,

	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	val x: Int,

	@BitField(id = 2)
	@IntegerField(expectUniform = false, minValue = 0)
	val y: Int,

	/**
	 * The flash code, mostly useful for development
	 */
	@BitField(id = 3)
	val flashCode: String,

	/**
	 * When true, the trigger can only happen once (per 'campaign'). When false, players can keep triggering it.
	 */
	@BitField(id = 4)
	val oneTimeOnly: Boolean,

	/**
	 * Only used for some complex triggers in the inventors lab. When true, the trigger is not `oneTimeOnly`, but it
	 * can't be triggered again until the player leaves and re-enters the map.
	 */
	@BitField(id = 5)
	val oncePerAreaLoad: Boolean,

	/**
	 * Seems to be true for most teleporting triggers. It means that it only triggers after the player
	 * **reaches** the tile. When `walkOn == null`, it is... complicated
	 */
	@BitField(id = 6, optional = true)
	val walkOn: Boolean?
) {

	@Suppress("unused")
	private constructor() : this("", 0, 0, "", false, false, null)

	override fun toString() = "Trigger(x=$x, y=$y, once=$oneTimeOnly, walkOn=$walkOn, code=$flashCode)"

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)
}
