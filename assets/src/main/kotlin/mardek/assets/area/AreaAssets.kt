package mardek.assets.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceFieldTarget
import mardek.assets.area.objects.SwitchColor
import mardek.assets.sprite.ArrowSprite
import mardek.assets.sprite.DirectionalSprites
import mardek.assets.sprite.ObjectSprites

@BitStruct(backwardCompatible = false)
class AreaAssets {

	@BitField(ordering = 0)
	@ReferenceFieldTarget(label = "tilesheets")
	val tilesheets = ArrayList<Tilesheet>()

	@BitField(ordering = 1)
	@ReferenceFieldTarget(label = "switch colors")
	val switchColors = ArrayList<SwitchColor>()

	@BitField(ordering = 2)
	@ReferenceFieldTarget(label = "character sprites")
	val characterSprites = ArrayList<DirectionalSprites>()

	@BitField(ordering = 3)
	@ReferenceFieldTarget(label = "object sprites")
	val objectSprites = ArrayList<ObjectSprites>()

	@BitField(ordering = 4)
	@ReferenceFieldTarget(label = "arrow sprites")
	val arrowSprites = ArrayList<ArrowSprite>()

	@BitField(ordering = 5)
	@ReferenceFieldTarget(label = "chest sprites")
	val chestSprites = ArrayList<ChestSprite>()

	@BitField(ordering = 6)
	@ReferenceFieldTarget(label = "level ranges")
	val levelRanges = ArrayList<SharedLevelRange>()

	@BitField(ordering = 7)
	@ReferenceFieldTarget(label = "enemy selections")
	val enemySelections = ArrayList<SharedEnemySelections>()

	@BitField(ordering = 8)
	@ReferenceFieldTarget(label = "areas")
	val areas = ArrayList<Area>()
}
