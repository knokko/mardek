package mardek.assets.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceFieldTarget
import mardek.assets.animations.Skeleton

@BitStruct(backwardCompatible = true)
class BattleAssets {

	@BitField(id = 0)
	@ReferenceFieldTarget(label = "battle backgrounds")
	val backgrounds = ArrayList<BattleBackground>()

	@BitField(id = 1)
	@ReferenceFieldTarget(label = "skeletons")
	val skeletons = ArrayList<Skeleton>()

	@BitField(id = 2)
	@ReferenceFieldTarget(label = "monsters")
	val monsters = ArrayList<Monster>()

	@BitField(id = 3)
	@ReferenceFieldTarget(label = "enemy party layouts")
	val enemyPartyLayouts = ArrayList<PartyLayout>()
}
