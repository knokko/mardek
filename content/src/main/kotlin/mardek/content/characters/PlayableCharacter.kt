package mardek.content.characters

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.content.animations.BattleModel
import mardek.content.stats.CharacterClass
import mardek.content.stats.Element
import mardek.content.stats.StatModifier
import mardek.content.sprite.DirectionalSprites
import java.util.*
import kotlin.collections.ArrayList

@BitStruct(backwardCompatible = true)
class PlayableCharacter(
	@BitField(id = 0)
	val name: String,

	@BitField(id = 1)
	@ReferenceField(stable = false, label = "character classes")
	val characterClass: CharacterClass,

	@BitField(id = 2)
	@ReferenceField(stable = false, label = "elements")
	val element: Element,

	@BitField(id = 3)
	val baseStats: ArrayList<StatModifier>,

	@BitField(id = 4)
	val areaSprites: DirectionalSprites,

	@BitField(id = 5)
	val battleModel: BattleModel,
) {

	@BitField(id = 6)
	@StableReferenceFieldId
	val id = UUID.randomUUID()!!

	@Suppress("unused")
	private constructor() : this(
		"", CharacterClass(), Element(), ArrayList(0), DirectionalSprites(), BattleModel()
	)

	override fun toString() = name
}
