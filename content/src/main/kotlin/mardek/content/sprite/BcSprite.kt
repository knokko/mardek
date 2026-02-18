package mardek.content.sprite

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FunctionContext
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting

@BitStruct(backwardCompatible = true)
class BcSprite(
	@BitField(id = 0)
	@IntegerField(expectUniform = false, minValue = 1, digitSize = 2)
	val width: Int,

	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 1, digitSize = 2)
	val height: Int,

	@BitField(id = 2)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 7, commonValues = [7])
	val version: Int,
) {
	var bufferedImage: Any? = null

	@BitField(id = 3, readsMethodResult = true)
	var data: ByteArray? = null

	@BitField(id = 4)
	@IntegerField(minValue = -1, expectUniform = false, digitSize = 3)
	var index = -1

	constructor() : this(0, 0, 0)

	@BitField(id = 3)
	@Suppress("unused")
	@NestedFieldSetting(path = "", optional = true, writeAsBytes = true)
	private fun saveData(context: FunctionContext): ByteArray? {
		return if (context.withParameters.containsKey("exporting")) null else data
	}
}
