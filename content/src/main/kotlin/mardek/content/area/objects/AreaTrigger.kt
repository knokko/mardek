package mardek.content.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.content.action.ActionSequence
import java.util.UUID

/**
 * An area trigger can activate an `ActionSequence` when the player steps on its tile.
 */
@BitStruct(backwardCompatible = true)
class AreaTrigger(
	/**
	 * Seems to be unused, but could be useful to keep around for debugging
	 */
	@BitField(id = 0)
	val name: String,

	x: Int,
	y: Int,

	/**
	 * The flash code, mostly useful for development. Either `flashCode` or `actions` should be non-null
	 */
	@BitField(id = 1, optional = true)
	val flashCode: String?,

	/**
	 * When true, the trigger can only happen once (per 'campaign'). When false, players can keep triggering it.
	 */
	@BitField(id = 2)
	val oneTimeOnly: Boolean,

	/**
	 * Only used for some complex triggers in the inventors lab. When true, the trigger is not `oneTimeOnly`, but it
	 * can't be triggered again until the player leaves and re-enters the map.
	 */
	@BitField(id = 3)
	val oncePerAreaLoad: Boolean,

	/**
	 * Seems to be true for most teleporting triggers. It means that it only triggers after the player
	 * **reaches** the tile. When `walkOn == null`, it is... complicated
	 */
	@BitField(id = 4, optional = true)
	val walkOn: Boolean?,

	/**
	 * The actions to be executed when this trigger is activated.
	 */
	@BitField(id = 5, optional = true)
	@ReferenceField(stable = false, label = "action sequences")
	val actions: ActionSequence?,

	@BitField(id = 6)
	@StableReferenceFieldId
	@Suppress("unused")
	private val id: UUID,
) : StaticAreaObject(x, y) {

	init {
		if ((flashCode == null) == (actions == null)) {
			throw IllegalArgumentException("Exactly 1 of `flashCode` and `actions` must be non-null")
		}
	}

	@Suppress("unused")
	private constructor() : this(
		"", 0, 0, "", false,
		false, null, null, UUID.randomUUID(),
	)

	override fun toString() = "Trigger(x=$x, y=$y, once=$oneTimeOnly, walkOn=$walkOn, code=$flashCode)"
}
