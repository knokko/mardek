package mardek.state.ingame.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ClassField
import com.github.knokko.bitser.field.IntegerField
import mardek.content.area.TransitionDestination
import mardek.content.util.Time
import kotlin.time.Duration

/**
 * When a playable character or [mardek.content.area.objects.AreaCharacter] is walking from one tile to another,
 * a `NextAreaPosition` will be used to indicate this.
 *
 * The position of the character will keep its old value until `areaState.currentTime < startTime + walkDuration`,
 * after which the position is changed to the new position, and the `NextAreaPosition` is deleted.
 */
@BitStruct(backwardCompatible = true)
class NextAreaPosition(

	/**
	 * The destination (tile) position
	 */
	@BitField(id = 0)
	val position: AreaPosition,

	/**
	 * The value of [AreaState.currentTime] when the character *started* walking.
	 */
	@BitField(id = 1)
	val startTime: Time,

	/**
	 * The character reaches [position] when `areaState.currentTime >= startTime + walkDuration`
	 */
	@BitField(id = 2)
	@IntegerField(expectUniform = true)
	val walkDuration: Duration,

	/**
	 * When this field is non-null, the player will be teleported to this transition destination right after reaching
	 * [position]. This is used by e.g. doors and area transitions.
	 *
	 * Most of the time though, this field is `null`.
	 */
	@BitField(id = 3, optional = true)
	@ClassField(root = TransitionDestination::class)
	val transition: TransitionDestination?,
) {

	internal constructor() : this(
		AreaPosition(), Time.ZERO,
		Duration.ZERO, null
	)

	override fun toString() = "($position at $startTime + $walkDuration)"
}
