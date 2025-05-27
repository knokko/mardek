package mardek.content.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceFieldTarget
import mardek.content.animations.Skeleton
import mardek.content.particle.Particle
import mardek.content.particle.ParticleSprite
import mardek.content.sprite.BcSprite

@BitStruct(backwardCompatible = true)
class BattleContent {

	@BitField(id = 0)
	@ReferenceFieldTarget(label = "battle backgrounds")
	val backgrounds = ArrayList<BattleBackground>()

	@BitField(id = 1)
	@ReferenceFieldTarget(label = "skeletons")
	val skeletons = ArrayList<Skeleton>()

	@BitField(id = 2)
	@ReferenceFieldTarget(label = "particle sprites")
	val particleSprites = ArrayList<ParticleSprite>()

	@BitField(id = 3)
	@ReferenceFieldTarget(label = "particles")
	val particles = ArrayList<Particle>()

	@BitField(id = 4)
	@ReferenceFieldTarget(label = "monsters")
	val monsters = ArrayList<Monster>()

	@BitField(id = 5)
	@ReferenceFieldTarget(label = "enemy party layouts")
	val enemyPartyLayouts = ArrayList<PartyLayout>()
}
