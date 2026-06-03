package mardek.content.ui

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceFieldTarget

/**
 * All the fonts used by the game/engine
 */
@BitStruct(backwardCompatible = true)
class Fonts(

	/**
	 * Myriad Condensed Web, which is a rather simple and a bit boring.
	 */
	@BitField(id = 0)
	@ReferenceFieldTarget(label = "fonts")
	val basic1: Font,

	/**
	 * Nyala font, which looks a bit more MARDEK-style than [basic1]. This is the most-used font in the game.
	 */
	@BitField(id = 1)
	@ReferenceFieldTarget(label = "fonts")
	val basic2: Font,

	/**
	 * Trajan Pro, which is a full-uppercase font. This is used on text that needs to 'stand out'.
	 */
	@BitField(id = 2)
	@ReferenceFieldTarget(label = "fonts")
	val large1: Font,

	/**
	 * Star Wars font, which is a full-uppercase font, and quite similar to [large1].
	 */
	@BitField(id = 3)
	@ReferenceFieldTarget(label = "fonts")
	val large2: Font,

	/**
	 * Star Wars font with only the 'I', 'K', 'P', and 'S' characters.
	 * This font is currently unused in this engine, and I have no clue what vanilla MARDEK uses it for.
	 */
	@BitField(id = 4)
	@ReferenceFieldTarget(label = "fonts")
	val ikps: Font,

	/**
	 * Other uppercase Trajan Pro font, looks nearly identical to `large1`.
	 * This font is currently unused in this engine, and I have no clue what vanilla MARDEK uses it for.
	 */
	@BitField(id = 5)
	@ReferenceFieldTarget(label = "fonts")
	val large3: Font,

	/**
	 * A fat version of the Nyala font ([basic2]), which is used for text that needs to stand out.
	 */
	@BitField(id = 6)
	@ReferenceFieldTarget(label = "fonts")
	val fat: Font,

	/**
	 * The OldCyr font, which is used by GdM members.
	 */
	@BitField(id = 7)
	@ReferenceFieldTarget(label = "fonts")
	val gdm: Font,

	/**
	 * The Bio-disc Thin font, which is somewhat 'square'.
	 * I don't know what it is used for in vanilla MARDEK. Maybe the spaceship?
	 */
	@BitField(id = 8)
	@ReferenceFieldTarget(label = "fonts")
	val square: Font,

	/**
	 * The Eccentric Std font, which is full uppercase.
	 * I don't know what it is used for in vanilla MARDEK. Maybe the fairies?
	 */
	@BitField(id = 9)
	@ReferenceFieldTarget(label = "fonts")
	val eccentric: Font,

	/**
	 * The MicroMieps font, which looks somewhat weird.
	 * * I don't know what it is used for in vanilla MARDEK. Maybe the fairies?
	 */
	@BitField(id = 10)
	@ReferenceFieldTarget(label = "fonts")
	val weird: Font,

	/**
	 * The Creedmore font, which looks somewhat 'digital', and has many diagonal lines.
	 * I don't know what it is used for in vanilla MARDEK. Maybe the spaceship?
	 */
	@BitField(id = 11)
	@ReferenceFieldTarget(label = "fonts")
	val digital: Font,

	/**
	 * The Orange LET font, which gives me a somewhat... sloppy impression.
	 * I don't know what this font is used for in vanilla MARDEK.
	 */
	@BitField(id = 12)
	@ReferenceFieldTarget(label = "fonts")
	val sloppy: Font,

	/**
	 * The Chintzy CPU BRK font, which is rather fat and full uppercase
	 * I don't know what this font is used for in vanilla MARDEK.
	 */
	@BitField(id = 13)
	@ReferenceFieldTarget(label = "fonts")
	val fatUppercase: Font,

	/**
	 * The FairydustB font, where each glyph has a 'continued stroke' at the start and end of each glyph.
	 */
	@BitField(id = 14)
	@ReferenceFieldTarget(label = "fonts")
	val fairy: Font,
) {

	@Suppress("unused")
	private constructor() : this(
		Font(), Font(), Font(), Font(), Font(),
		Font(), Font(), Font(), Font(), Font(),
		Font(), Font(), Font(), Font(), Font(),
	)

	/**
	 * Gets an array containing all the fonts.
	 */
	fun all() = arrayOf(
		basic1, basic2, large1, large2, ikps,
		large3, fat, gdm, square, eccentric,
		weird, digital, sloppy, fatUppercase, fairy,
	)
}
