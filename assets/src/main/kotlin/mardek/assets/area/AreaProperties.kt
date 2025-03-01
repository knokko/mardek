package mardek.assets.area

import com.github.knokko.bitser.BitEnum
import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceField
import mardek.assets.battle.BattleBackground

@BitStruct(backwardCompatible = true)
class AreaProperties(

	@BitField(id = 0)
	var rawName: String,

	@BitField(id = 1)
	val displayName: String,

	/**
	 * Format is {ra:100,rb:0,ga:80,gb:0,ba:70,bb:0,aa:100,ab:0}, it looks like some foreground gradient between
	 * colors (red=ra, green=ga, blue=ba, alpha=aa) to (red=rb, green=gb, blue=bb, alpha=ab).
	 *
	 * It's used in, among others, Goznor night, Goznor zombie outbreak, Sauls dungeon, and Goldfish
	 */
	@BitField(id = 2, optional = true)
	val ambience: AreaAmbience?,

	@BitField(id = 3, optional = true)
	val musicTrack: String?,

	/**
	 * All areas with the same "dungeon" will share their switch gate/platform state
	 */
	@BitField(id = 4, optional = true)
	val dungeon: String?,

	/**
	 * Upon visiting this area, the place with this name will be discovered in the encyclopedia
	 */
	@BitField(id = 5, optional = true)
	val encyclopediaName: String?,

	@BitField(id = 6)
	val dreamType: AreaDreamType,

	@BitField(id = 7)
	val snowType: AreaSnowType,
	// TODO Dreamshrine Entrance has some GlowTiles stuff
) {

	internal constructor() : this(
		"", "", null, null, null,
		null, AreaDreamType.AstralTunnel, AreaSnowType.None
	)
}

@BitEnum(mode = BitEnum.Mode.Ordinal)
enum class AreaSnowType(val code: Int) {
	None(0),
	Lifewood(1),
	Dreamwood(2)
}

@BitEnum(mode = BitEnum.Mode.Ordinal)
enum class AreaDreamType(
	/**
	 * Basically the hex code for the background color of the area
	 */
	val code: String
) {
	None(""),
	/**
	 * Uses 4849778, or #4A0072
	 */
	DreamcaveAndDreamshrineAndGrottoAndEarthTemple("true"),
	/**
	 * Nearly black
	 */
	SerpentCave("1"),
	/**
	 * No dream border
	 */
	DreamwoodAndCanoniaShaman("2"),
	AstralTunnel("19000"),
	DreamwoodCave("3342387")
}
