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

@BitStruct(backwardCompatible = true)
class Campaign(
	@BitField(id = 0)
	val combat: CombatAssets,

	@BitField(id = 1)
	val skills: SkillAssets,

	@BitField(id = 2)
	val inventory: InventoryAssets,

	@BitField(id = 3)
	val battle: BattleAssets,

	@BitField(id = 4)
	val areas: AreaAssets,

	@BitField(id = 5)
	@ReferenceFieldTarget(label = "playable characters")
	val playableCharacters: ArrayList<PlayableCharacter>,

	@BitField(id = 6)
	val ui: UiSprites,
) {

	@BitField(id = 7)
	@NestedFieldSetting(path = "v", writeAsBytes = true)
	val checkpoints = HashMap<String, ByteArray>()

	constructor() : this(
		CombatAssets(), SkillAssets(), InventoryAssets(),
		BattleAssets(), AreaAssets(), arrayListOf(), UiSprites()
	)

	companion object {
		fun load(resourcePath: String): Campaign {
			val input = BitInputStream(BufferedInputStream(Campaign::class.java.classLoader.getResourceAsStream(resourcePath)!!))
			val assets = Bitser(false).deserialize(Campaign::class.java, input, Bitser.BACKWARD_COMPATIBLE)
			input.close()

			return assets
		}
	}
}
