package mardek.content.ui

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting

@BitStruct(backwardCompatible = true)
class Font {

	@BitField(id = 1)
	@NestedFieldSetting(path = "", optional = true, writeAsBytes = true)
	var data: ByteArray? = null

	@BitField(id = 2)
	@IntegerField(minValue = -1, expectUniform = false)
	var index = -1

	fun copy(): Font {
		val copied = Font()
		copied.data = data
		copied.index = index
		return copied
	}
}
