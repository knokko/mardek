package mardek.content.action

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.ui.Font

@BitStruct(backwardCompatible = true)
class DialogueTextStyle(

	@BitField(id = 0)
	val name: String,

	@BitField(id = 1)
	@ReferenceField(stable = false, label = "fonts")
	val font: Font,

	@BitField(id = 2)
	@IntegerField(expectUniform = true)
	val fillColor: Int,

	@BitField(id = 3)
	@IntegerField(expectUniform = true)
	val strokeColor: Int,

	@BitField(id = 4)
	@FloatField(expectMultipleOf = 0.01, errorTolerance = 0.005)
	val strokeWidth: Float,

	@BitField(id = 5)
	@FloatField(expectMultipleOf = 0.01, errorTolerance = 0.005)
	val strokeDistancePower: Float,
) {

	@Suppress("unused")
	private constructor() : this(
		"", Font(), 0,
		0, 0f, 0f,
	)
}
