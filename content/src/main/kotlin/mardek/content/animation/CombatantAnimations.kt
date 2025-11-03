package mardek.content.animation

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceField

/**
 * Represents the skeleton + animations + skin of a combatant, which is pretty much all the information that is needed
 * to render a combatant.
 */
@BitStruct(backwardCompatible = true)
class CombatantAnimations(

	/**
	 * The skeleton and animations of the combatant. The skeleton contains all the animations, as well as the sprites
	 * for every skin.
	 *
	 * Many monsters share their skeleton with a couple of other monsters (e.g. zombie monster and the standard monster
	 * share their skeleton). When the `skeleton` is shared, each monster will have a different `skin` that causes the
	 * renderer to pick different sprites.
	 */
	@BitField(id = 0)
	@ReferenceField(stable = false, label = "combatant skeletons")
	val skeleton: CombatantSkeleton,

	/**
	 * The skin of the monster, if applicable. Skins are mostly meaningful when multiple monsters share the same
	 * skeleton.
	 */
	@BitField(id = 1, optional = true)
	val skin: String?,
) {
	constructor() : this(CombatantSkeleton(), "")

	operator fun get(name: String) = skeleton[name]
}
