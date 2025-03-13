package mardek.content.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceFieldTarget
import mardek.content.area.objects.SwitchColor
import mardek.content.sprite.ArrowSprite
import mardek.content.sprite.DirectionalSprites
import mardek.content.sprite.ObjectSprites

@BitStruct(backwardCompatible = true)
class AreaContent {

	@BitField(id = 0)
	@ReferenceFieldTarget(label = "tilesheets")
	val tilesheets = ArrayList<Tilesheet>()

	@BitField(id = 1)
	@ReferenceFieldTarget(label = "switch colors")
	val switchColors = ArrayList<SwitchColor>()

	@BitField(id = 2)
	@ReferenceFieldTarget(label = "character sprites")
	val characterSprites = ArrayList<DirectionalSprites>()

	@BitField(id = 3)
	@ReferenceFieldTarget(label = "object sprites")
	val objectSprites = ArrayList<ObjectSprites>()

	@BitField(id = 4)
	@ReferenceFieldTarget(label = "arrow sprites")
	val arrowSprites = ArrayList<ArrowSprite>()

	@BitField(id = 5)
	@ReferenceFieldTarget(label = "chest sprites")
	val chestSprites = ArrayList<ChestSprite>()

	@BitField(id = 6)
	@ReferenceFieldTarget(label = "level ranges")
	val levelRanges = ArrayList<SharedLevelRange>()

	@BitField(id = 7)
	@ReferenceFieldTarget(label = "enemy selections")
	val enemySelections = ArrayList<SharedEnemySelections>()

	@BitField(id = 8)
	@ReferenceFieldTarget(label = "areas")
	val areas = ArrayList<Area>()
}
