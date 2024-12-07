package mardek.assets.area.sprites

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.NestedFieldSetting

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
	@NestedFieldSetting(path = "", writeAsBytes = true)
	var indices: IntArray? = null

	internal constructor() : this("")
}
