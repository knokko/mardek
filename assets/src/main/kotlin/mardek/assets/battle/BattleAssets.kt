package mardek.assets.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceFieldTarget
import mardek.assets.animations.Skeleton

@BitStruct(backwardCompatible = false)
class BattleAssets {

	@BitField(ordering = 0)
	@ReferenceFieldTarget(label = "battle backgrounds")
	val backgrounds = ArrayList<BattleBackground>()

	@BitField(ordering = 1)
	@ReferenceFieldTarget(label = "skeletons")
	val skeletons = ArrayList<Skeleton>()

	@BitField(ordering = 2)
	@ReferenceFieldTarget(label = "enemy party layouts")
	val enemyPartyLayouts = ArrayList<PartyLayout>()
}
