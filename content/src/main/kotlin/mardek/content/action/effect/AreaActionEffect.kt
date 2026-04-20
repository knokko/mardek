package mardek.content.action.effect

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField

/**
 * An effect that can be played during an area actions suspension (e.g. souls of Rohoph or Moric)
 */
@BitStruct(backwardCompatible = true)
class AreaActionEffect(

	/**
	 * The name of the effect, which is used only for debugging and editing
	 */
	@BitField(id = 0)
	val name: String,

	/**
	 * The actual effect emitters
	 */
	@BitField(id = 1)
	val emitters: Array<AreaEffectsEmitter>,
) {

	internal constructor() : this("", emptyArray())
}
