package mardek.content.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceFieldTarget
import mardek.content.animation.AnimationSprite
import mardek.content.animation.CombatantSkeleton
import mardek.content.animation.SkinnedAnimation
import mardek.content.particle.ParticleEffect
import mardek.content.particle.ParticleSprite
import mardek.content.sprite.BcSprite

@BitStruct(backwardCompatible = true)
class BattleContent {

	@BitField(id = 0)
	@ReferenceFieldTarget(label = "animation sprites")
	val animationSprites = ArrayList<AnimationSprite>()

	@BitField(id = 1)
	@ReferenceFieldTarget(label = "skinned animations")
	val skinnedAnimations = ArrayList<SkinnedAnimation>()

	@BitField(id = 2)
	@ReferenceFieldTarget(label = "battle backgrounds")
	val backgrounds = ArrayList<BattleBackground>()

	@BitField(id = 3)
	@ReferenceFieldTarget(label = "combatant skeletons")
	val skeletons = ArrayList<CombatantSkeleton>()

	@BitField(id = 4)
	@ReferenceFieldTarget(label = "monsters")
	val monsters = ArrayList<Monster>()

	@BitField(id = 5)
	@ReferenceFieldTarget(label = "particle sprites")
	val particleSprites = ArrayList<ParticleSprite>()

	@BitField(id = 6)
	@ReferenceFieldTarget(label = "particles")
	val particles = ArrayList<ParticleEffect>()

	@BitField(id = 7)
	@ReferenceFieldTarget(label = "enemy party layouts")
	val enemyPartyLayouts = ArrayList<PartyLayout>()

	@BitField(id = 8)
	val lootItemTexts = ArrayList<String>()

	@BitField(id = 9)
	val lootNoItemTexts = ArrayList<String>()

	@BitField(id = 10)
	val noMask = BcSprite(1, 1, 4)
}
