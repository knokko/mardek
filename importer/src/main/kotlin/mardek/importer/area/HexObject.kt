package mardek.importer.area

import com.github.knokko.boiler.utilities.ColorPacker.rgb
import mardek.assets.area.objects.AreaLight

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
		val map = mapOf(
			Pair(rgb(255, 0, 0), HexObject("torch", 3, 2, 16, AreaLight("deepred", 12))),
			Pair(rgb(59, 51, 73), HexObject("SkullBrazier",3,0,32, AreaLight("deepred", -37))),
			Pair(rgb(255, 153, 0), HexObject("torch", 3, 0, 16, AreaLight("1", 12))),
			Pair(rgb(255, 255, 113), HexObject("torch", 3, 1, 16, AreaLight("yellow", 12))),
			Pair(rgb(188, 175, 113), HexObject("solakStatue", 9, 0, 32, null)),
			Pair(rgb(0, 204, 0), HexObject("YBrazier", 3, 0, 32, AreaLight("green", -37))), // could also be (204, 0, 0); double-check
			Pair(rgb(79, 62, 103), HexObject("torch", 3, 3, 16, AreaLight("purple2", 12))),
			Pair(rgb(74, 255, 102), HexObject("torch", 3, 4, 16, AreaLight("green", 12))),
			Pair(rgb(102, 255, 102), HexObject("GreenScreen", 12, 0, 16, null)),
			Pair(rgb(39, 222, 26), HexObject("GreenCrystal", 3, 0, 24, AreaLight("green", 0))),
			Pair(rgb(255, 237, 130), HexObject("YellowBrazier", 3, 0, 32, AreaLight("yellow", -37))),
			Pair(rgb(0, 0, 255), HexObject("torch", 3, 5, 16, AreaLight("blue3", 12))),
			Pair(rgb(255, 153, 51), HexObject("fierybrazier", 3, 0, 32, AreaLight("brazier", -6))),
			Pair(rgb(38, 79, 74), HexObject("CyanBrazier", 3, 0, 32, AreaLight("cyan", -6)))
		)
	}
}

