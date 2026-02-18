package mardek.content.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField

/**
 * The walking behavior of an [AreaCharacter]. This determines whether a character randomly walks around, or stays at
 * the same tile.
 */
@BitStruct(backwardCompatible = true)
class WalkBehavior(

	/**
	 * The average number of times that this character will decide to randomly move to another time, when it is
	 * standing still for 1 second.
	 *
	 * - When this is 0, the character will stay in its starting position, unless it is forcibly moved by actions.
	 * - When this is positive, the character will randomly walk around. Since all characters need 500 milliseconds to
	 * move from one tile to another, the character would need `N / movesPerSecond + 0.5N` seconds on average to move
	 * `N` tiles.
	 */
	@BitField(id = 0)
	@FloatField(expectMultipleOf = 0.1, errorTolerance = 0.06)
	val movesPerSecond: Float,

	/**
	 * Whether this character shows a walking animation while standing still. This may sound weird, but it is commonly
	 * used on characters whose [movesPerSecond] is 0.
	 */
	@BitField(id = 1)
	val showAnimationWhileStandingStill: Boolean,
) {

	internal constructor() : this(0f, false)
}