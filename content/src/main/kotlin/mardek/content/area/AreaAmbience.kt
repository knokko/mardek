package mardek.content.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import mardek.content.BITSER

/**
 * The *ambience* of the area, which is probably a color overlay over some areas at certain time, for instance a dark
 * tint over Goznor while it is dusk or night. // TODO CHAP1 Figure this out
 *
 * - Goznor chapter 1 dusk ambience is                   {ra:100,rb:0,ga:80,gb:0,ba:70,bb:0,aa:100,ab:0};
 * - Goznor chapter 2 zombie ambience is                 {ra:25,rb:0,ga:40,gb:0,ba:80,bb:0,aa:100,ab:0};
 * - Goznor chapter 2 ambience during Canonia zombies is {ra:25,rb:0,ga:40,gb:0,ba:80,bb:0,aa:100,ab:0};
 * - Goznor chapter 2 genric sunset is                   {ra:100,rb:0,ga:80,gb:0,ba:70,bb:0,aa:100,ab:0};
 * - Goznor chapter 2 night ambience is                  {ra:25,rb:0,ga:40,gb:0,ba:80,bb:0,aa:100,ab:0};
 *
 * Use the `ColorPacker` of `vk-boiler` to extract the RGBA from `colorA` and `colorB`
 */
@BitStruct(backwardCompatible = true)
class AreaAmbience(
	@BitField(id = 0)
	@IntegerField(expectUniform = true)
	val colorA: Int,

	@BitField(id = 1)
	@IntegerField(expectUniform = true)
	val colorB: Int
) {

	@Suppress("unused")
	private constructor() : this(0, 0)

	override fun toString() = "Ambience(rawColorA=$colorA, rawColorB=$colorB)"

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)

	companion object {
		/**
		 * This is a 'magic' instance of AreaAmbience that the renderer must treat like this flash code:
		 *
		 * ```
		 * return GameData.plotVars.SUNSET != "NIGHT" ? (!GameData.plotVars.SUNSET ? (!Default ? null : Default) :
		 * {ra:100,rb:0,ga:80,gb:0,ba:70,bb:0,aa:100,ab:0}) : {ra:25,rb:0,ga:40,gb:0,ba:80,bb:0,aa:100,ab:0};
		 * ```
		 */
		val GENERIC_EXTERNAL_AMBIENCE = AreaAmbience(123456789, 123456789)
	}
}
