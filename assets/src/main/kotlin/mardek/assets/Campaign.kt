package mardek.assets

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceFieldTarget
import com.github.knokko.bitser.io.BitInputStream
import com.github.knokko.bitser.serialize.Bitser
import mardek.assets.area.AreaAssets
import mardek.assets.battle.BattleAssets
import mardek.assets.characters.PlayableCharacter
import mardek.assets.combat.CombatAssets
import mardek.assets.inventory.InventoryAssets
import mardek.assets.skill.SkillAssets
import mardek.assets.ui.UiSprites
import java.io.BufferedInputStream

@BitStruct(backwardCompatible = false)
class Campaign(
	@BitField(ordering = 0)
	val combat: CombatAssets,

	@BitField(ordering = 1)
	val skills: SkillAssets,

	@BitField(ordering = 2)
	val inventory: InventoryAssets,

	@BitField(ordering = 3)
	val battle: BattleAssets,

	@BitField(ordering = 4)
	val areas: AreaAssets,

	@BitField(ordering = 5)
	@ReferenceFieldTarget(label = "playable characters")
	val playableCharacters: ArrayList<PlayableCharacter>,

	@BitField(ordering = 6)
	val ui: UiSprites,
) {

	@BitField(ordering = 7)
	@NestedFieldSetting(path = "v", writeAsBytes = true)
	val checkpoints = HashMap<String, ByteArray>()

	constructor() : this(
		CombatAssets(), SkillAssets(), InventoryAssets(),
		BattleAssets(), AreaAssets(), arrayListOf(), UiSprites()
	)

	companion object {
		fun load(resourcePath: String): Campaign {
			val input = BitInputStream(BufferedInputStream(Campaign::class.java.classLoader.getResourceAsStream(resourcePath)!!))
			val assets = Bitser(false).deserialize(Campaign::class.java, input)
			input.close()

			return assets
		}
	}
}
