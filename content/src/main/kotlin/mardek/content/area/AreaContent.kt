package mardek.content.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceFieldTarget
import mardek.content.area.objects.SwitchColor
import mardek.content.sprite.ArrowSprite
import mardek.content.sprite.DirectionalSprites
import mardek.content.sprite.ObjectSprites

/**
 * The part of the `Content` that contains all the area-related content: it contains all tiles, areas, and area-related
 * sprites.
 */
@BitStruct(backwardCompatible = true)
class AreaContent {

	/**
	 * All the tilesheets: these contain all the tiles that areas can have
	 */
	@BitField(id = 0)
	@ReferenceFieldTarget(label = "tilesheets")
	val tilesheets = ArrayList<Tilesheet>()

	/**
	 * The colors that switch orbs/gates/platforms can have
	 */
	@BitField(id = 1)
	@ReferenceFieldTarget(label = "switch colors")
	val switchColors = ArrayList<SwitchColor>()

	/**
	 * All the sprites that area characters can have (both player and non-player characters)
	 */
	@BitField(id = 2)
	@ReferenceFieldTarget(label = "character sprites")
	val characterSprites = ArrayList<DirectionalSprites>()

	/**
	 * All the sprites that area objects can have
	 */
	@BitField(id = 3)
	@ReferenceFieldTarget(label = "object sprites")
	val objectSprites = ArrayList<ObjectSprites>()

	/**
	 * The sprites that area transition 'arrows' can have
	 */
	@BitField(id = 4)
	@ReferenceFieldTarget(label = "arrow sprites")
	val arrowSprites = ArrayList<ArrowSprite>()

	/**
	 * The sprites that can be used by chests
	 */
	@BitField(id = 5)
	@ReferenceFieldTarget(label = "chest sprites")
	val chestSprites = ArrayList<ChestSprite>()

	/**
	 * The monster level ranges that areas can have. Storing them here makes it easy to share the same level range
	 * between multiple areas (especially useful for different areas in the same dungeon)
	 */
	@BitField(id = 6)
	@ReferenceFieldTarget(label = "level ranges")
	val levelRanges = ArrayList<SharedLevelRange>()

	/**
	 * The monster selections (for random battles) that areas can have. Just like `levelRanges`, storing them here
	 * makes it easy to share them between areas.
	 */
	@BitField(id = 7)
	@ReferenceFieldTarget(label = "enemy selections")
	val enemySelections = ArrayList<SharedEnemySelections>()

	/**
	 * The list of areas
	 */
	@BitField(id = 8)
	@ReferenceFieldTarget(label = "areas")
	val areas = ArrayList<Area>()
}
