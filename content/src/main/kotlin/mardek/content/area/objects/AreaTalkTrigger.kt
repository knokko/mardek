package mardek.content.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ClassField
import com.github.knokko.bitser.field.IntegerField
import mardek.content.story.TimelineExpression

/**
 * When a talk trigger is placed at (x, y), the player can interact with the tile (x, y), causing the player to
 * interact with whatever is placed at (talkX, talkY).
 *
 * This is typically used in shops, where players can interact with the counter, to interact with the character behind
 * the counter.
 */
@BitStruct(backwardCompatible = true)
class AreaTalkTrigger(

	/**
	 * The name of the talk trigger, as imported from Flash. It's only used for debugging and editing.
	 */
	@BitField(id = 0)
	val name: String,

	x: Int,
	y: Int,

	/**
	 * The X-coordinate of the 'talk destination' tile
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = false)
	val talkX: Int,

	/**
	 * The Y-coordinate of the 'talk destination' tile
	 */
	@BitField(id = 2)
	@IntegerField(expectUniform = false)
	val talkY: Int,

	/**
	 * The condition of this trigger. When this evaluates to `false`, players cannot interact with this trigger.
	 * When `null`, players can always interact with this trigger.
	 */
	@BitField(id = 3, optional = true)
	@ClassField(root = TimelineExpression::class)
	val condition: TimelineExpression<Boolean>?,
) : StaticAreaObject(x, y) {

	@Suppress("unused")
	private constructor() : this("", 0, 0, 0, 0, null)

	override fun toString() = "TalkTrigger($name, x=$x, y=$y, talkTo=($talkX, $talkY))"
}
