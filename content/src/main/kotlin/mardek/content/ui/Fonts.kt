package mardek.content.ui

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField

@BitStruct(backwardCompatible = true)
class Fonts(

	/**
	 * Myriad Condensed Web, nothing special
	 */
	@BitField(id = 0)
	val basic1: Font,

	/**
	 * Nyala font, nothing special
	 */
	@BitField(id = 1)
	val basic2: Font,

	/**
	 * Trajan Pro, full uppercase font
	 */
	@BitField(id = 2)
	val large1: Font,

	/**
	 * Star Wars font, full uppercase
	 */
	@BitField(id = 3)
	val large2: Font,

	/**
	 * Star Wars font with only the 'I', 'K', 'P', and 'S' characters
	 */
	@BitField(id = 4)
	val ikps: Font,

	/**
	 * Other uppercase Trajan Pro font, looks nearly identical to `large1`
	 */
	@BitField(id = 5)
	val large3: Font,

	/**
	 * Fat Nyala font
	 */
	@BitField(id = 6)
	val fat: Font,

	/**
	 * The font used for Gaspar, which is fat and hard to read
	 */
	@BitField(id = 7)
	val gaspar: Font,

	/**
	 * The Bio-disc Thin font, which is somewhat 'square'
	 */
	@BitField(id = 8)
	val square: Font,

	/**
	 * The Eccentric Std font, which is full uppercase
	 */
	@BitField(id = 9)
	val eccentric: Font,

	/**
	 * The MicroMieps font, which looks somewhat weird
	 */
	@BitField(id = 10)
	val weird: Font,

	/**
	 * The Creedmore font, which looks somewhat 'digital', and has many diagonal lines
	 */
	@BitField(id = 11)
	val digital: Font,

	/**
	 * The Orange LET font, which gives me a somewhat... sloppy impression
	 */
	@BitField(id = 12)
	val sloppy: Font,

	/**
	 * The Chintzy CPU BRK font, which is rather fat and full uppercase
	 */
	@BitField(id = 13)
	val fatUppercase: Font,

	/**
	 * The FairydustB font, where each glyph has a 'continued stroke' at the start and end of each glyph
	 */
	@BitField(id = 14)
	val fairy: Font,
) {

	@Suppress("unused")
	private constructor() : this(
		Font(), Font(), Font(), Font(), Font(),
		Font(), Font(), Font(), Font(), Font(),
		Font(), Font(), Font(), Font(), Font(),
	)

	fun all() = arrayOf(
		basic1, basic2, large1, large2, ikps,
		large3, fat, gaspar, square, eccentric,
		weird, digital, sloppy, fatUppercase, fairy,
	)
}
