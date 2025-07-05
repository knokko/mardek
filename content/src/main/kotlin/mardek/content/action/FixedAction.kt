package mardek.content.action

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ClassField
import com.github.knokko.bitser.field.IntegerField

@BitStruct(backwardCompatible = true)
sealed class FixedAction {
	companion object {

		@JvmStatic
		@Suppress("unused")
		val BITSER_HIERARCHY = arrayOf(
			ActionWalk::class.java,
			ActionTalk::class.java,
			ActionBattle::class.java,
		)
	}
}

@BitStruct(backwardCompatible = true)
class ActionWalk(

	@BitField(id = 0)
	@ClassField(root = ActionTarget::class)
	val target: ActionTarget,

	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	val destinationX: Int,

	@BitField(id = 2)
	@IntegerField(expectUniform = false, minValue = 0)
	val destinationY: Int,

	@BitField(id = 3)
	val speed: WalkSpeed,
) : FixedAction()

class ActionTalk(

	@BitField(id = 0)
	@ClassField(root = ActionTarget::class)
	val speaker: ActionTarget,

	@BitField(id = 1)
	val expression: String,

	@BitField(id = 2)
	val text: String,
) : FixedAction()

@BitStruct(backwardCompatible = true)
class ActionBattle() : FixedAction()
