package mardek.editor.client

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.Bitser
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FunctionContext
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.StableReferenceFieldId
import com.github.knokko.bitser.field.StringField
import com.github.knokko.bitser.io.BitInputStream
import mardek.content.BITSER
import mardek.content.Content
import mardek.content.particle.ParticleEffect
import mardek.content.sprite.BcSprite
import mardek.content.sprite.KimSprite
import mardek.content.stats.CombatStat
import mardek.content.stats.Element
import mardek.content.ui.Font
import mardek.exporting.ContentWrapper
import mardek.exporting.DecompressingInputStream
import java.io.File
import java.io.Serializable
import java.util.Collections
import java.util.HashMap
import java.util.IdentityHashMap
import java.util.UUID

private fun measure(target: Any) {
	val allKimSprites = Collections.newSetFromMap(IdentityHashMap<KimSprite, Boolean>())
	val allBcSprites = Collections.newSetFromMap(IdentityHashMap<BcSprite, Boolean>())
	val allFonts = Collections.newSetFromMap(IdentityHashMap<Font, Boolean>())
	val collectionMapping = HashMap<Class<*>, Collection<Any>>()
	collectionMapping[KimSprite::class.java] = allKimSprites
	collectionMapping[BcSprite::class.java] = allBcSprites
	collectionMapping[Font::class.java] = allFonts
	BITSER.collectInstances(target, collectionMapping, hashMapOf())
	println("sizes are ${allKimSprites.size}, ${allBcSprites.size}, ${allFonts.size}")
}

fun main() {
	val input1 = File("/home/knokko/mardek/editor-server/content.bits")
	val fatContent = BITSER.deserialize(
		ContentWrapper::class.java,
		BitInputStream(DecompressingInputStream(input1)),
		Bitser.BACKWARD_COMPATIBLE,
	)

	measure(fatContent)

	val fatContent2 = BITSER.stupidDeepCopy(fatContent, Bitser.BACKWARD_COMPATIBLE)
	measure(fatContent2)

	val content3 = BITSER.stupidDeepCopy(fatContent2.content)
	measure(content3)

	// attempt 1: 3733, 24619, 15
	// attempt 2: 3733, 24568, 15
	// attempt 3: 3733, 271, 15

	// attempt 4: 3733, 24620, 15
}

@BitStruct(backwardCompatible = true)
class Dummy2(
	@BitField(id = 0)
	val elements: Array<Element>,
) : Serializable

fun main2() {
	val input1 = File("/home/knokko/mardek/editor-server/content.bits")
	val fatContent = BITSER.deserialize(
		ContentWrapper::class.java,
		BitInputStream( DecompressingInputStream(input1)),
		Bitser.BACKWARD_COMPATIBLE,
	)

	val original = Dummy2(elements = fatContent.content.stats.elements.toTypedArray())
	val copied = BITSER.deepCopy(original)
	BITSER.toBytes(copied)
}

@BitStruct(backwardCompatible = true)
class DummyElement3(
	@BitField(id = 0, optional = true)
	val swingEffect: BcSprite?,
) : Serializable

@BitStruct(backwardCompatible = true)
class Dummy3(

	@BitField(id = 0)
	val elements: Array<DummyElement3>,
) : Serializable

fun main3() {
	val original = Dummy3(elements = arrayOf(DummyElement3(null)))
	val copied = BITSER.deepCopy(original)
	BITSER.toBytes(copied)
}

fun main4() {
	val input1 = File("/home/knokko/mardek/editor-server/content.bits")
	val fatContent = BITSER.deserialize(
		ContentWrapper::class.java,
		BitInputStream( DecompressingInputStream(input1)),
		Bitser.BACKWARD_COMPATIBLE,
	)

	val original = Dummy2(elements = arrayOf(fatContent.content.stats.elements.find { it.rawName == "DIVINE" }!!))
	val copied = BITSER.deepCopy(original)
	BITSER.toBytes(copied)
}


/**
 * Represents one of the elements of MARDEK. The vanilla elements are FIRE, WATER, EARTH, AIR, LIGHT, DARK, AETHER,
 * FIG, PHYSICAL, THAUMA, and DIVINE.
 */
@BitStruct(backwardCompatible = true)
class DummyElement5(

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
	 * The 'blink color' when combatants take damage from an attack with this element.
	 *
	 * This is usually the same as [color], but not always.
	 * In particular, the [color] of PHYSICAL is grey, whereas its [damageColor] is red.
	 */
	@BitField(id = 6)
	@IntegerField(expectUniform = true)
	val damageColor: Int = color,

	/**
	 * The sprite of the element symbol, with extra thick lines (because the lines are ridiculously small in the
	 * SVG).
	 *
	 * This sprite is typically used when we need to render the element symbol in a small area.
	 */
	@BitField(id = 7)
	val thickSprite: BcSprite,

	/**
	 * The sprite of the element symbol, with thicker lines than [thinSprite], but thinner lines than [thickSprite].
	 *
	 * This sprite is typically used when we need to render the element symbol in a rather small area.
	 */
	@BitField(id = 8)
	val mediumSprite: BcSprite,

	/**
	 * The sprite of the element symbol, with thicker lines than the original SVG, but thinner lines than
	 * [thickSprite] and [mediumSprite].
	 *
	 * This sprite is typically used when we need to render the element symbol in a large area.
	 */
	@BitField(id = 9)
	val thinSprite: BcSprite,

	/**
	 * The sprite that is used to render the swing effect of greatswords (and some other weapons) when a melee skill
	 * is used. It is basically a gradient sprite of the elements color, and is rendered very close to the greatsword
	 * during the strike animation.
	 */
	@BitField(id = 10, optional = true)
	val swingEffect: BcSprite?,

	/**
	 * The particle effect that should be emitted at the hand of the caster whenever someone casts a magic/ranged skill
	 * with this element.
	 */
	@BitField(id = 11, optional = true)
	@ReferenceField(stable = false, label = "particles")
	val spellCastEffect: ParticleEffect?,

	/**
	 * The gradient-like sprite that will be rendered behind the caster whenever someone casts a magic/ranged skill
	 * with this element.
	 */
	@BitField(id = 12, optional = true)
	val spellCastBackground: BcSprite?,

	/**
	 * The color that is used to render the name of speakers in the chat log. This is often very similar or identical
	 * to [color], but not for "DARK", which has a purple chat log color, but a standard ~black color.
	 */
	@BitField(id = 13)
	@IntegerField(expectUniform = true)
	val chatLogColor: Int,
) : Serializable {

	/**
	 * The element against which this element is usually weak (e.g. `FIRE.weakAgainst == WATER`)., or `null` if this
	 * element is not weak against anything (e.g. `PHYSICAL` and `THAUMA`).
	 *
	 * Players of this element will have a 20% vulnerability against the `weakAgainst` of their own element.
	 */
	@BitField(id = 14, optional = true)
	@ReferenceField(stable = false, label = "elements")
	var weakAgainst: Element? = null
		private set

	constructor() : this(
		UUID.randomUUID(), "", null, "", "", 0,
		0, BcSprite(), BcSprite(), BcSprite(),
		BcSprite(), null, null, 0,
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

@BitStruct(backwardCompatible = true)
class Dummy5(
	@BitField(id = 0)
	val elements: Array<DummyElement5>,
) : Serializable

fun main5() {
	val input1 = File("/home/knokko/mardek/editor-server/content.bits")
	val fatContent = BITSER.deserialize(
		ContentWrapper::class.java,
		BitInputStream( DecompressingInputStream(input1)),
		Bitser.BACKWARD_COMPATIBLE,
	)

	val originalDivine = fatContent.content.stats.elements.find { it.rawName == "DIVINE" }!!
	val dummyDivine = DummyElement5(
		id = originalDivine.id,
		rawName = originalDivine.rawName,
		bonusStat = originalDivine.bonusStat,
		primaryChar = originalDivine.primaryChar,
		properName = originalDivine.properName,
		color = originalDivine.color,
		damageColor = originalDivine.damageColor,
		thickSprite = originalDivine.thickSprite,
		mediumSprite = originalDivine.mediumSprite,
		thinSprite = originalDivine.thinSprite,
		swingEffect = originalDivine.swingEffect,
		spellCastEffect = originalDivine.spellCastEffect,
		spellCastBackground = originalDivine.spellCastBackground,
		chatLogColor = originalDivine.chatLogColor,
	)
	val original = Dummy5(elements = arrayOf(dummyDivine))
	val copied = BITSER.deepCopy(original)
	BITSER.toBytes(copied)
}

fun main6() {
	val input1 = File("/home/knokko/mardek/editor-server/content.bits")
	val fatContent = BITSER.deserialize(
		ContentWrapper::class.java,
		BitInputStream( DecompressingInputStream(input1)),
		Bitser.BACKWARD_COMPATIBLE,
	)

	val originalDivine = fatContent.content.stats.elements.find { it.rawName == "DIVINE" }!!
	val dummyDivine = DummyElement5(
		id = originalDivine.id,
		rawName = originalDivine.rawName,
		bonusStat = originalDivine.bonusStat,
		primaryChar = originalDivine.primaryChar,
		properName = originalDivine.properName,
		color = originalDivine.color,
		damageColor = originalDivine.damageColor,
		thickSprite = originalDivine.thickSprite,
		mediumSprite = originalDivine.mediumSprite,
		thinSprite = originalDivine.thinSprite,
		swingEffect = originalDivine.swingEffect,
		spellCastEffect = originalDivine.spellCastEffect,
		spellCastBackground = originalDivine.spellCastBackground,
		chatLogColor = originalDivine.chatLogColor,
	)
	val copied = BITSER.deepCopy(dummyDivine)
	BITSER.toBytes(copied)
}

/**
 * Represents one of the elements of MARDEK. The vanilla elements are FIRE, WATER, EARTH, AIR, LIGHT, DARK, AETHER,
 * FIG, PHYSICAL, THAUMA, and DIVINE.
 */
@BitStruct(backwardCompatible = true)
class DummyElement7(
	@BitField(id = 0, optional = true)
	val swingEffect: BcSprite?,
) : Serializable {
	constructor() : this(
		BcSprite(),
	)

	override fun hashCode() = 1

	override fun equals(other: Any?) = other is Element
}

fun main7() {
	val input1 = File("/home/knokko/mardek/editor-server/content.bits")
	val fatContent = BITSER.deserialize(
		ContentWrapper::class.java,
		BitInputStream( DecompressingInputStream(input1)),
		Bitser.BACKWARD_COMPATIBLE,
	)

	val originalDivine = fatContent.content.stats.elements.find { it.rawName == "DIVINE" }!!
	val dummyDivine = DummyElement7(
		swingEffect = originalDivine.swingEffect,
	)
	val copied = BITSER.deepCopy(dummyDivine)
	BITSER.toBytes(copied)
}

fun main8() {
	val dummyDivine = DummyElement7(swingEffect = null)
	val copied = BITSER.deepCopy(dummyDivine)
	BITSER.toBytes(copied)
}

@BitStruct(backwardCompatible = true)
class DummySprite9(

	/**
	 * The height of the sprite, in pixels
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 1, digitSize = 2)
	val height: Int,
) : Serializable {
	constructor() : this(0)
}


@BitStruct(backwardCompatible = true)
class DummyElement9(
	@BitField(id = 0, optional = true)
	val swingEffect: DummySprite9?,
) : Serializable {
	constructor() : this(
		DummySprite9(),
	)

	override fun hashCode() = 1

	override fun equals(other: Any?) = other is Element
}

fun main9() {
	val dummyDivine = DummyElement9(swingEffect = null)
	val copied = BITSER.deepCopy(dummyDivine)
	BITSER.toBytes(copied)
}
