package mardek.content.story

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceFieldTarget

/**
 * The story-related part of the `Content`
 */
@BitStruct(backwardCompatible = true)
class StoryContent {

	/**
	 * The root node of the timeline
	 */
	@BitField(id = 0)
	@ReferenceFieldTarget(label = "timelines")
	val timelines = ArrayList<Timeline>()

	/**
	 * The 'fixed' timeline variables that are needed by the engine, and have some 'magic' meaning (e.g. whether the
	 * item storage is currently blocked, or the available party members).
	 */
	@BitField(id = 1)
	val fixedVariables = FixedTimelineVariables()

	/**
	 * The timeline variables that are *not* needed by the engine, but can be used in some `TimelineExpression`s.
	 */
	@BitField(id = 2)
	@ReferenceFieldTarget(label = "timeline variables")
	val customVariables = ArrayList<CustomTimelineVariable<*>>()

	/**
	 * The global timeline expression, which can be reused in multiple places. These are potentially useful for code
	 * reuse, even after the importing phase is finished.
	 */
	@BitField(id = 3)
	@ReferenceFieldTarget(label = "global expressions")
	val globalExpressions = ArrayList<GlobalExpression<*>>()

	/**
	 * All the quests (which can show up in the Quests tab of the in-game menu).
	 */
	@BitField(id = 4)
	@ReferenceFieldTarget(label = "quests")
	val quests = ArrayList<Quest>()
}
