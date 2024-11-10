package mardek.importer.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.CollectionField
import mardek.assets.area.OptimizedArea

@BitStruct(backwardCompatible = false)
class AreaContainer {

	@BitField(ordering = 0)
	@CollectionField
	val areas = ArrayList<OptimizedArea>()
}
