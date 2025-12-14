package mardek.content.story

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ClassField

/**
 * Represents a named global (timeline) expression, which is potentially good for 'code' reuse, even after the hardcoded
 * importing phase is finished.
 */
@BitStruct(backwardCompatible = true)
class GlobalExpression<T>(

	/**
	 * The name of the global expression, which is only used for editing and debugging
	 */
	@BitField(id = 0)
	val name: String,

	/**
	 * The expression to be reused
	 */
	@BitField(id = 1)
	@ClassField(root = TimelineExpression::class)
	val expression: TimelineExpression<T>,
) {

	internal constructor() : this("", ConstantTimelineExpression())
}
