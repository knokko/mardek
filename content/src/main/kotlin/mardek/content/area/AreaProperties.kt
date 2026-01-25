package mardek.content.area

import com.github.knokko.bitser.BitEnum
import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ClassField
import mardek.content.animation.ColorTransform
import mardek.content.story.ConstantTimelineExpression
import mardek.content.story.TimelineExpression

/**
 * The basic properties that all areas have.
 */
@BitStruct(backwardCompatible = true)
class AreaProperties(

	/**
	 * The 'raw' name of the area, as imported from Flash. This name is needed for Flash importing, but otherwise not
	 * really.
	 */
	@BitField(id = 0)
	var rawName: String,

	/**
	 * The nice/display name of the area.
	 */
	@BitField(id = 1)
	val displayName: String,

	/**
	 * The *ambience* of the area, which is a color transformation on some areas (typically open-air areas),
	 * for instance a weak red tint over Goznor while it is dusk or night.
	 *
	 * - The Flash evening ambience is {ra:100,rb:0,ga:80,gb:0,ba:70,bb:0,aa:100,ab:0},
	 * which is equivalent to `ColorTransform(multiplyColor=rgb(1.0, 0.8, 0.7))`
	 * - The Flash night ambience is {ra:25,rb:0,ga:40,gb:0,ba:80,bb:0,aa:100,ab:0},
	 * which is equivalent to `ColorTransform(multiplyColor=rgb(0.25, 0.4, 0.8))`
	 */
	@BitField(id = 2)
	@ClassField(root = TimelineExpression::class)
	val ambience: TimelineExpression<ColorTransform>,

	/**
	 * The name of the music track that should be played while the player is in the area.
	 */
	@BitField(id = 3, optional = true)
	@ClassField(root = TimelineExpression::class)
	val musicTrack: TimelineExpression<String?>,

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

	/**
	 * When an area is a dreamworld area, this property determines its background color.
	 */
	@BitField(id = 6)
	val dreamType: AreaDreamType,

	/**
	 * Some areas have some kind of snow, which is specified by this field
	 */
	@BitField(id = 7)
	val snowType: AreaSnowType,
	// TODO CHAP3 Dreamshrine Entrance has some GlowTiles stuff
) {

	constructor() : this(
		"", "", ConstantTimelineExpression(), ConstantTimelineExpression(),
		null, null, AreaDreamType.AstralTunnel, AreaSnowType.None
	)
}

/**
 * The types of snow behavior that areas can have
 */
@BitEnum(mode = BitEnum.Mode.Ordinal)
enum class AreaSnowType(val code: Int) {

	/**
	 * This area doesn't have any snow
	 */
	None(0),

	/**
	 * This area has the same snow as Lifewood
	 */
	Lifewood(1),

	/**
	 * This area has the same 'snow' as Dreamwood
	 */
	Dreamwood(2)
}

/**
 * The types of dreamworld background that areas can have
 */
@BitEnum(mode = BitEnum.Mode.Ordinal)
enum class AreaDreamType(
	/**
	 * Basically the hex code for the background color of the area
	 */
	val code: String
) {

	/**
	 * This is not a dreamworld area
	 */
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

	/**
	 * The astral tunnel background
	 */
	AstralTunnel("19000"),

	/**
	 * The dreamwood cave background (not to be confused with the dreamwood serpent cave)
	 */
	DreamwoodCave("3342387")
}
