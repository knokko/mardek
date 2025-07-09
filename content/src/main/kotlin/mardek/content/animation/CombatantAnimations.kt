package mardek.content.animation

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceField

@BitStruct(backwardCompatible = true)
class CombatantAnimations(
	@BitField(id = 0)
	@ReferenceField(stable = false, label = "combatant skeletons")
	val skeleton: CombatantSkeleton,

	@BitField(id = 1, optional = true)
	val skin: String?,
) {
	constructor() : this(CombatantSkeleton(), "")

	operator fun get(name: String) = skeleton[name]
}
