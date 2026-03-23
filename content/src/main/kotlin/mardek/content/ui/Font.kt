package mardek.content.ui

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FunctionContext
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting

/**
 * Represents a TrueType font (which we need for text rendering)
 */
@BitStruct(backwardCompatible = true)
class Font {

	/**
	 * The raw TTF data of the font. Note that this will be `null` while in-game, but non-null while editing
	 * or importing.
	 */
	@BitField(id = 0, readsMethodResult = true)
	var data: ByteArray? = null

	/**
	 * The index of this sprite into the font list of the renderer. The value of this is only meaningful while
	 * in-game; it should be -1 during editing and importing. This variable should get the right value during
	 * exporting.
	 */
	@BitField(id = 1)
	@IntegerField(minValue = -1, expectUniform = false)
	var index = -1

	/**
	 * Creates a shallow copy of this font. This should only be used during exporting.
	 */
	fun copy(): Font {
		val copied = Font()
		copied.data = data
		copied.index = index
		return copied
	}

	@BitField(id = 0)
	@Suppress("unused")
	@NestedFieldSetting(path = "", optional = true, writeAsBytes = true)
	private fun saveData(context: FunctionContext): ByteArray? {
		return if (context.withParameters.containsKey("exporting")) null else data
	}
}
