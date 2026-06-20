package mardek.content.stats

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.StableReferenceFieldId
import com.github.knokko.bitser.field.StringField
import mardek.content.particle.ParticleEffect
import mardek.content.sprite.BcSprite
import java.util.UUID

/**
 * Represents one of the elements of MARDEK. The vanilla elements are FIRE, WATER, EARTH, AIR, LIGHT, DARK, AETHER,
 * FIG, PHYSICAL, THAUMA, and DIVINE.
 */
@BitStruct(backwardCompatible = true)
class Element(

	/**
	 * The unique ID of this element, which is used for (de)serialization
	 */
	@BitField(id = 0)
	@StableReferenceFieldId
	val id: UUID,

	/**
	 * The 'raw' name of the element. For all elements except AETHER, this is equal to [properName].
	 * The raw name of AETHER is ETHER, whereas its proper name is **A**ETHER.
	 *
	 * The raw name is only useful during Flash importing.
	 */
	@BitField(id = 1, optional = true)
	val rawName: String,

	/**
	 * The [CombatStat] that is 'related' to this element. For instance, `STR` is the bonus stat of `FIRE` in vanilla.
	 *
	 * This field is currently unused, and I'm not sure what it does in vanilla MARDEK.
	 */
	@BitField(id = 2, optional = true)
	val bonusStat: CombatStat?,

	/**
	 * The primary letter/character of the element, typically the first character of its name (e.g. `'F'.codepoint`
	 * for `FIRE`).
	 *
	 * This is currently only used to generate the names of the Null Element Once status effects and the names of the
	 * Elemental Shell status effects. This field can probably be deleted when the importer is deleted.
	 */
	@BitField(id = 3)
	@StringField(length = IntegerField(expectUniform = true, minValue = 1, maxValue = 1))
	val primaryChar: String,

	/**
	 * The display/proper name of the element. This is usually the same as [rawName], except that the proper name of
	 * AETHER is "AETHER" whereas its raw name is "ETHER", and that the proper name of "NONE" is "PHYSICAL".
	 */
	@BitField(id = 4)
	val properName: String = rawName,

	/**
	 * The primary color of the element (e.g. orange for fire and white for light).
	 *
	 * This color is used in several places in the UI.
	 */
	@BitField(id = 5)
	@IntegerField(expectUniform = true)
	val color: Int,

	/**
	 * The sprite of the element symbol, with extra thick lines (because the lines are ridiculously small in the
	 * SVG).
	 *
	 * This sprite is typically used when we need to render the element symbol in a small area.
	 */
	@BitField(id = 6)
	val thickSprite: BcSprite,

	/**
	 * The sprite of the element symbol, with thicker lines than [thinSprite], but thinner lines than [thickSprite].
	 *
	 * This sprite is typically used when we need to render the element symbol in a rather small area.
	 */
	@BitField(id = 7)
	val mediumSprite: BcSprite,

	/**
	 * The sprite of the element symbol, with thicker lines than the original SVG, but thinner lines than
	 * [thickSprite] and [mediumSprite].
	 *
	 * This sprite is typically used when we need to render the element symbol in a large area.
	 */
	@BitField(id = 8)
	val thinSprite: BcSprite,

	/**
	 * The sprite that is used to render the swing effect of greatswords (and some other weapons) when a melee skill
	 * is used. It is basically a gradient sprite of the elements color, and is rendered very close to the greatsword
	 * during the strike animation.
	 */
	@BitField(id = 9, optional = true)
	val swingEffect: BcSprite?,

	/**
	 * The particle effect that should be emitted at the hand of the caster whenever someone casts a magic/ranged skill
	 * with this element.
	 */
	@BitField(id = 10, optional = true)
	@ReferenceField(stable = false, label = "particles")
	val spellCastEffect: ParticleEffect?,

	/**
	 * The gradient-like sprite that will be rendered behind the caster whenever someone casts a magic/ranged skill
	 * with this element.
	 */
	@BitField(id = 11, optional = true)
	val spellCastBackground: BcSprite?,

	/**
	 * The color that is used to render the name of speakers in the chat log. This is often very similar or identical
	 * to [color], but not for "DARK", which has a purple chat log color, but a standard ~black color.
	 */
	@BitField(id = 12)
	@IntegerField(expectUniform = true)
	val chatLogColor: Int,
) {

	/**
	 * The element against which this element is usually weak (e.g. `FIRE.weakAgainst == WATER`)., or `null` if this
	 * element is not weak against anything (e.g. `PHYSICAL` and `THAUMA`).
	 *
	 * Players of this element will have a 20% vulnerability against the `weakAgainst` of their own element.
	 */
	@BitField(id = 32, optional = true)
	@ReferenceField(stable = false, label = "elements")
	var weakAgainst: Element? = null
		private set

	constructor() : this(
		UUID.randomUUID(), "", null, "", "",
		0, BcSprite(), BcSprite(), BcSprite(), BcSprite(),
		null, null, 0,
	)

	/**
	 * This method should only be used by the importer
	 */
	fun setWeakAgainst(element: Element) {
		if (weakAgainst != null) throw IllegalStateException("$this is already weak against $weakAgainst")
		weakAgainst = element
	}

	override fun toString() = properName

	override fun hashCode() = id.hashCode()

	override fun equals(other: Any?) = other is Element && id == other.id
}
