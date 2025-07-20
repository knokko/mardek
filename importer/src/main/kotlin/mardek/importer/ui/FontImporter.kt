package mardek.importer.ui

import mardek.content.ui.Font
import mardek.content.ui.Fonts

private fun importFont(name: String): Font {
	val input = BcPacker::class.java.classLoader.getResourceAsStream("mardek/importer/fonts/$name.ttf")!!
	val font = Font()
	font.data = input.readAllBytes()
	input.close()
	return font
}

internal fun importFonts() = Fonts(
	basic = importFont("195_Myriad Condensed Web"),
	basicLarge = importFont("582_EPISODE I")
)
