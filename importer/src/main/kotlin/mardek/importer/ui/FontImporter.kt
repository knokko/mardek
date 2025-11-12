package mardek.importer.ui

import mardek.content.ui.Font
import mardek.content.ui.Fonts
import mardek.importer.util.classLoader

private fun importFont(name: String): Font {
	val input = classLoader.getResourceAsStream("mardek/importer/fonts/$name.ttf")!!
	val font = Font()
	font.data = input.readAllBytes()
	input.close()
	return font
}

internal fun importFonts() = Fonts(
	basic1 = importFont("195_Myriad Condensed Web"),
	basic2 = importFont("274_Nyala"),
	large1 = importFont("319_Trajan Pro"),
	large2 = importFont("582_EPISODE I"),
	ikps = importFont("737_EPISODE I"),
	large3 = importFont("854_Trajan Pro"),
	fat = importFont("1735_Nyala"),
	gaspar = importFont("5297_OldCyr"),
	square = importFont("5305_Bio-disc Thin"),
	eccentric = importFont("5307_Eccentric Std"),
	weird = importFont("5310_MicroMieps"),
	digital = importFont("5312_Creedmore"),
	sloppy = importFont("5316_Orange LET"),
	fatUppercase = importFont("5320_Chintzy CPU BRK"),
	fairy = importFont("5325_FairydustB"),
)
