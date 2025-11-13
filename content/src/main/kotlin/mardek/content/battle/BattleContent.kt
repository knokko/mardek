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

/**
 * The battle-related part of the `Content`
 */
@BitStruct(backwardCompatible = true)
class BattleContent {

	/**
	 * All sprites used by non-skeleton animations are stored in this list. Most skeleton animations however have
	 * their own list of sprites.
	 */
	@BitField(id = 0)
	@ReferenceFieldTarget(label = "animation sprites")
	val animationSprites = ArrayList<AnimationSprite>()

	/**
	 * All inner animations used by non-skeleton animations are stored in this list. Most skeleton animations however
	 * have their own list of inner animations.
	 */
	@BitField(id = 1)
	@ReferenceFieldTarget(label = "skinned animations")
	val skinnedAnimations = ArrayList<SkinnedAnimation>()

	/**
	 * All the battle backgrounds
	 */
	@BitField(id = 2)
	@ReferenceFieldTarget(label = "battle backgrounds")
	val backgrounds = ArrayList<BattleBackground>()

	/**
	 * All the animation/combatant skeletons that players and monsters can have.
	 */
	@BitField(id = 3)
	@ReferenceFieldTarget(label = "combatant skeletons")
	val skeletons = ArrayList<CombatantSkeleton>()

	/**
	 * All the enemies/monsters
	 */
	@BitField(id = 4)
	@ReferenceFieldTarget(label = "monsters")
	val monsters = ArrayList<Monster>()

	/**
	 * All the sprites that can be used by in-battle particle effects
	 */
	@BitField(id = 5)
	@ReferenceFieldTarget(label = "particle sprites")
	val particleSprites = ArrayList<ParticleSprite>()

	/**
	 * All the in-battle particle effects
	 */
	@BitField(id = 6)
	@ReferenceFieldTarget(label = "particles")
	val particles = ArrayList<ParticleEffect>()

	/**
	 * All party layouts that enemies/monsters can use
	 */
	@BitField(id = 7)
	@ReferenceFieldTarget(label = "enemy party layouts")
	val enemyPartyLayouts = ArrayList<PartyLayout>()

	/**
	 * The pool of random texts rendered near the top of the battle loot screen *when the enemies have dropped at least
	 * one item*, for instance "You have acquired:" and "You find where your foes once stood".
	 */
	@BitField(id = 8)
	val lootItemTexts = ArrayList<String>()

	/**
	 * The pool of random texts rendered near the top of the battle loot screen *when the enemies have not dropped any
	 * items*, for instance "No spoils here!" and "Today was not your lucky day, item-wise.".
	 */
	@BitField(id = 9)
	val lootNoItemTexts = ArrayList<String>()

	/**
	 * The `noMask` sprite is a very simple 1x1 bc4 sprite where its only pixel/alpha value is 1.0.
	 *
	 * All combatant sprites are drawn with an *alpha* mask, and the alpha/opacity of any pixel on a combatant sprite
	 * is multiplied by the corresponding pixel on the mask texture. This is useful for a couple of sprites, but the
	 * vast majority of sprites will use `noMask` as mask texture, which means that nothing is masked (all alpha values
	 * will be multiplied by 1.0).
	 */
	@BitField(id = 10)
	val noMask = BcSprite(1, 1, 4)
}
