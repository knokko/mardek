package mardek.content.ui

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FunctionContext
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting

@BitStruct(backwardCompatible = true)
class Font {

	@BitField(id = 0, readsMethodResult = true)
	var data: ByteArray? = null

	@BitField(id = 1)
	@IntegerField(minValue = -1, expectUniform = false)
	var index = -1

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
