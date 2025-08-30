package mardek.importer.area

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import com.github.knokko.boiler.utilities.ColorPacker.rgba
import mardek.content.area.objects.AreaLight

class HexObject(
	val sheetName: String,
	val timePerFrame: Int,
	val sheetRow: Int,
	/**
	 * in pixels
	 */
	val height: Int,
	val light: AreaLight?
) {
	companion object {
		val RED_LIGHT_COLOR = rgba(254, 97, 97, 150)
		val ORANGE_LIGHT_COLOR = rgba(248, 171, 97, 150)
		val YELLOW_LIGHT_COLOR = rgba(251, 239, 100, 150)
		val TURQUOISE_LIGHT_COLOR = rgba(115, 225, 181, 150)
		val GREEN_LIGHT_COLOR = rgba(90, 250, 134, 150)
		val BLUE_LIGHT_COLOR = rgba(0, 0, 254, 80)

		val map = mapOf(
			Pair(rgb(255, 0, 0), HexObject(
				"torch", 3, 2, 16, AreaLight(RED_LIGHT_COLOR, 5)
			)),
			Pair(rgb(59, 51, 73), HexObject(
				"SkullBrazier",3,0,32, AreaLight(RED_LIGHT_COLOR, -12)
			)),
			Pair(rgb(255, 153, 0), HexObject(
				"torch", 3, 0, 16, AreaLight(ORANGE_LIGHT_COLOR, 5)
			)),
			Pair(rgb(255, 255, 113), HexObject(
				"torch", 3, 1, 16, AreaLight(YELLOW_LIGHT_COLOR, 5)
			)),
			Pair(rgb(188, 175, 113), HexObject("solakStatue", 9, 0, 32, null)),
			Pair(rgb(0, 204, 0), HexObject(
				"YBrazier", 3, 0, 32, AreaLight(GREEN_LIGHT_COLOR, -12))
			),
			Pair(rgb(79, 62, 103), HexObject(
				"torch", 3, 3, 16, AreaLight(TURQUOISE_LIGHT_COLOR, 5)
			)),
			Pair(rgb(74, 255, 102), HexObject(
				"torch", 3, 4, 16, AreaLight(GREEN_LIGHT_COLOR, 5)
			)),
			Pair(rgb(102, 255, 102), HexObject("GreenScreen", 12, 0, 16, null)),
			Pair(rgb(39, 222, 26), HexObject(
				"GreenCrystal", 3, 0, 24, AreaLight(GREEN_LIGHT_COLOR, 0)
			)),
			Pair(rgb(255, 237, 130), HexObject(
				"YellowBrazier", 3, 0, 32, AreaLight(YELLOW_LIGHT_COLOR, -12)
			)),
			Pair(rgb(0, 0, 255), HexObject(
				"torch", 3, 5, 16, AreaLight(BLUE_LIGHT_COLOR, 5)
			)),
			Pair(rgb(255, 153, 51), HexObject(
				"fierybrazier", 3, 0, 32, AreaLight(rgba(242, 150, 4, 80), -6)
			)),
			Pair(rgb(38, 79, 74), HexObject(
				"CyanBrazier", 3, 0, 32, AreaLight(rgba(140, 255, 255, 100), -6)
			))
		)
	}
}
