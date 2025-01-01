package mardek.assets.characters

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.assets.animations.BattleModel
import mardek.assets.combat.CharacterClass
import mardek.assets.combat.Element
import mardek.assets.combat.StatModifier
import mardek.assets.sprite.DirectionalSprites
import java.util.*
import kotlin.collections.ArrayList

@BitStruct(backwardCompatible = false)
class PlayableCharacter(
	@BitField(ordering = 0)
	val name: String,

	@BitField(ordering = 1)
	@ReferenceField(stable = false, label = "character classes")
	val characterClass: CharacterClass,

	@BitField(ordering = 2)
	@ReferenceField(stable = false, label = "elements")
	val element: Element,

	@BitField(ordering = 3)
	val baseStats: ArrayList<StatModifier>,

	@BitField(ordering = 4)
	val areaSprites: DirectionalSprites,

	@BitField(ordering = 5)
	val battleModel: BattleModel,
) {

	@BitField(ordering = 6)
	@StableReferenceFieldId
	val id = UUID.randomUUID()!!

	@Suppress("unused")
	private constructor() : this(
		"", CharacterClass(), Element(), ArrayList(0), DirectionalSprites(), BattleModel()
	)

	override fun toString() = name
}
