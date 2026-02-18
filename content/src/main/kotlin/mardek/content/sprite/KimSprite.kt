package mardek.content.sprite

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FunctionContext
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.compressor.Kim1Decompressor
import com.github.knokko.compressor.Kim2Decompressor
import com.github.knokko.compressor.Kim3Compressor

@BitStruct(backwardCompatible = true)
class KimSprite(
	@BitField(id = 0, readsMethodResult = true)
	var data: IntArray?,

	@BitField(id = 1)
	@IntegerField(expectUniform = true, minValue = 1, maxValue = 3, commonValues = [3])
	val version: Int,
) {

	@BitField(id = 2)
	@IntegerField(minValue = -1, expectUniform = false, digitSize = 3)
	var index = -1

	@BitField(id = 3)
	@IntegerField(expectUniform = true, commonValues=[ // Don't question it: it just saves storage space
		83951632, 100728848, 50397200, 67174416, 117506064, 151060496, 16842768, 134283280,
		167837712, 184614928, 33619984, 201392144, 218169360, 234946576, 251723792
	])
	val header = if (data == null) 0 else data!![0]

	val width: Int
		get() = when (version) {
			1 -> Kim1Decompressor.width(header)
			2 -> Kim2Decompressor.getWidth(header)
			else -> Kim3Compressor.getWidth(header)
		}

	val height: Int
		get() = when (version) {
			1 -> Kim1Decompressor.height(header)
			2 -> Kim2Decompressor.getWidth(header)
			else -> Kim3Compressor.getHeight(header)
		}

	init {
		if (version != 1 && version != 2 && version != 3) throw IllegalArgumentException("Unknown KIM version $version")
	}

	constructor() : this(null, 1)

	@BitField(id = 0)
	@Suppress("unused")
	@NestedFieldSetting(path = "", optional = true, writeAsBytes = true)
	private fun saveData(context: FunctionContext): IntArray? {
		return if (context.withParameters.containsKey("exporting")) null
		else data
	}
}
