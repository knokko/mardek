package mardek.assets.area

/**
 * Use the `ColorPacker` of `vk-boiler` to extract the RGBA from `colorA` and `colorB`
 */
class AreaAmbience(val colorA: Int, val colorB: Int) {

	override fun toString() = "Ambience(rawColorA=$colorA, rawColorB=$colorB)"

	override fun equals(other: Any?) = other is AreaAmbience && colorA == other.colorA && colorB == other.colorB

	override fun hashCode() = colorA + colorB

	companion object {
		/**
		 * This is a 'magic' instance of AreaAmbience that the renderer must treat like this flash code:
		 *
		 * ```
		 * return GameData.plotVars.SUNSET != "NIGHT" ? (!GameData.plotVars.SUNSET ? (!Default ? null : Default) :
		 * {ra:100,rb:0,ga:80,gb:0,ba:70,bb:0,aa:100,ab:0}) : {ra:25,rb:0,ga:40,gb:0,ba:80,bb:0,aa:100,ab:0};
		 * ```
		 */
		val GENERIC_EXTERNAL_AMBIENCE = AreaAmbience(1234, 5678)
	}
}
