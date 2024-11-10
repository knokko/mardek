package mardek.assets.area.sprites

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.CollectionField

@BitStruct(backwardCompatible = false)
class DirectionalSpritesheet(
	@BitField(ordering = 0)
	val flashName: String
) {

	// TODO Save these conditionally
//	@BitField(ordering = 2, optional = true)
//	@CollectionField(writeAsBytes = true)
	var sprites: Array<KimImage>? = null

	@BitField(ordering = 1, optional = true)
	@CollectionField(writeAsBytes = true)
	var indices: IntArray? = null

	@Suppress("unused")
	private constructor() : this("")
}
