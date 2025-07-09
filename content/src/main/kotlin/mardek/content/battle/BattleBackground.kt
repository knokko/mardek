package mardek.content.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.content.animation.AnimationNode
import java.util.*

@BitStruct(backwardCompatible = true)
class BattleBackground(
	@BitField(id = 0)
	val name: String,

	@BitField(id = 1)
	val nodes: Array<AnimationNode>,

	@BitField(id = 2)
	@IntegerField(expectUniform = false, minValue = 1)
	val magicScale: Int,
) {

	@BitField(id = 3)
	@StableReferenceFieldId
	val id = UUID.randomUUID()!!

	constructor() : this("", emptyArray<AnimationNode>(), 1)
}
