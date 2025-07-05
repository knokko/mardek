package mardek.content.action

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.characters.PlayableCharacter

@BitStruct(backwardCompatible = true)
sealed class ActionTarget {
	companion object {

		@JvmStatic
		@Suppress("unused")
		val BITSER_HIERARCHY = arrayOf(
			ActionTargetPartyMember::class.java,
			ActionTargetPlayer::class.java,
			ActionTargetWholeParty::class.java,
		)
	}
}

@BitStruct(backwardCompatible = true)
class ActionTargetPartyMember(

	@BitField(id = 0)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 3)
	val index: Int
) : ActionTarget()

@BitStruct(backwardCompatible = true)
class ActionTargetPlayer(

	@BitField(id = 0)
	@ReferenceField(stable = false, label = "playable characters")
	val player: PlayableCharacter
) : ActionTarget()

@BitStruct(backwardCompatible = true)
class ActionTargetWholeParty : ActionTarget()
