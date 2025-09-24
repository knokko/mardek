package mardek.content.characters

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.content.animation.CombatantAnimations
import mardek.content.portrait.PortraitInfo
import mardek.content.stats.CharacterClass
import mardek.content.stats.Element
import mardek.content.stats.StatModifier
import mardek.content.sprite.DirectionalSprites
import mardek.content.stats.CreatureType
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
	val animations: CombatantAnimations,

	@BitField(id = 6)
	val creatureType: CreatureType,

	@BitField(id = 7)
	@ReferenceField(stable = false, label = "portrait info")
	val portraitInfo: PortraitInfo,
) {

	@BitField(id = 8)
	@StableReferenceFieldId
	val id = UUID.randomUUID()!!

	constructor() : this(
		"", CharacterClass(), Element(), ArrayList(0),
		DirectionalSprites(), CombatantAnimations(), CreatureType(), PortraitInfo()
	)

	override fun toString() = name
}
