package mardek.content.story

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ClassField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField

/**
 * Represents the assignment of a `TimelineValue` to a `TimelineVariable`.
 */
@BitStruct(backwardCompatible = true)
class TimelineAssignment<T>(

	/**
	 * The variable whose value is assigned to `value`
	 */
	@BitField(id = 0)
	@ReferenceField(stable = false, label = "timeline variables")
	val variable: TimelineVariable<T>,

	/**
	 * The `value` that will be assigned to `variable`
	 */
	@BitField(id = 1)
	@ClassField(root = TimelineValue::class)
	val value: TimelineValue<T>,

	/**
	 * Most timeline assignments have `appliesToFutureNodes = false`, which means that the variable assignment is
	 * removed/rolled back once the story state moves on to the next node.
	 *
	 * For instance, when a timeline node assigns "crickets" to the world map music, the world map music will be
	 * "crickets" *while the story state is inside the node*, but the world map music will be rolled back to the
	 * default music as soon as the story state moves on to the next timeline node.
	 *
	 * When `appliesToFutureNodes = true`, the variable assignment will remain active, even after the story state moves
	 * on to the next node. This is e.g. used for completed main quests: the "Heroes Quest!" of chapter 1 will stay
	 * 'completed', even after chapter 1 is already completed.
	 */
	@BitField(id = 2)
	val appliesToFutureNodes: Boolean = false,

	/**
	 * When multiple timeline assignments try to assign a value to the same timeline variable, the priority determines
	 * which assignment 'wins': timeline assignments with a higher priority overrule timeline assignments with a lower
	 * priority. When their priorities are the same, an exception will be thrown.
	 *
	 * Almost all timeline assignments have a priority of 0. Larger priorities are only needed when timeline nodes
	 * need to override timeline assignments of other nodes.
	 */
	@BitField(id = 3)
	@IntegerField(expectUniform = false, commonValues = [0])
	val priority: Int = 0,
) {

	@Suppress("UNCHECKED_CAST", "unused")
	private constructor() : this(CustomTimelineVariable<T>(), TimelineUnitValue() as TimelineValue<T>)

	override fun toString() = "$variable := $value"
}
