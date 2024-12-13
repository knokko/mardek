package mardek.assets.area

import com.github.knokko.bitser.BitEnum
import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField

@BitStruct(backwardCompatible = false)
class AreaProperties(

	@BitField(ordering = 0)
	var rawName: String,

	@BitField(ordering = 1)
	val displayName: String,

	/**
	 * Format is {ra:100,rb:0,ga:80,gb:0,ba:70,bb:0,aa:100,ab:0}, it looks like some foreground gradient between
	 * colors (red=ra, green=ga, blue=ba, alpha=aa) to (red=rb, green=gb, blue=bb, alpha=ab).
	 *
	 * It's used in, among others, Goznor night, Goznor zombie outbreak, Sauls dungeon, and Goldfish
	 */
	@BitField(ordering = 2, optional = true)
	val ambience: AreaAmbience?,

	@BitField(ordering = 3, optional = true)
	val musicTrack: String?,

	/**
	 * All areas with the same "dungeon" will share their switch gate/platform state
	 */
	@BitField(ordering = 4, optional = true)
	val dungeon: String?,

	/**
	 * Upon visiting this area, the place with this name will be discovered in the encyclopedia
	 */
	@BitField(ordering = 5, optional = true)
	val encyclopediaName: String?,

	@BitField(ordering = 6)
	val dreamType: AreaDreamType,

	@BitField(ordering = 7)
	val chestType: AreaChestType,

	@BitField(ordering = 8)
	val snowType: AreaSnowType,
	// TODO Dreamshrine Entrance has some GlowTiles stuff
) {

	internal constructor() : this(
		"", "", null, null, null, null,
		AreaDreamType.AstralTunnel, AreaChestType.Default, AreaSnowType.None
	)
}

@BitEnum(mode = BitEnum.Mode.UniformOrdinal)
enum class AreaChestType(val code: Int) {
	Default(0),
	MoricSaucer(1),
	DarkTemple(2),
	DreamRealm(3),
	BoneDemonAndRedDragon(5)
}

@BitEnum(mode = BitEnum.Mode.UniformOrdinal)
enum class AreaSnowType(val code: Int) {
	None(0),
	Lifewood(1),
	Dreamwood(2)
}

@BitEnum(mode = BitEnum.Mode.UniformOrdinal)
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
