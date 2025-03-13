package mardek.content

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceFieldTarget
import com.github.knokko.bitser.io.BitInputStream
import com.github.knokko.bitser.serialize.Bitser
import mardek.content.area.AreaContent
import mardek.content.battle.BattleContent
import mardek.content.characters.PlayableCharacter
import mardek.content.combat.StatsContent
import mardek.content.inventory.ItemsContent
import mardek.content.skill.SkillsContent
import mardek.content.ui.UiSprites
import java.io.BufferedInputStream

@BitStruct(backwardCompatible = true)
class Content {
	@BitField(id = 0)
	val stats = StatsContent()

	@BitField(id = 1)
	val skills = SkillsContent()

	@BitField(id = 2)
	val items = ItemsContent()

	@BitField(id = 3)
	val battle = BattleContent()

	@BitField(id = 4)
	val areas = AreaContent()

	@BitField(id = 5)
	@ReferenceFieldTarget(label = "playable characters")
	val playableCharacters = ArrayList<PlayableCharacter>()

	@BitField(id = 6)
	lateinit var ui: UiSprites

	@BitField(id = 7)
	@NestedFieldSetting(path = "v", writeAsBytes = true)
	val checkpoints = HashMap<String, ByteArray>()

	companion object {
		fun load(resourcePath: String): Content {
			val input = BitInputStream(BufferedInputStream(Content::class.java.classLoader.getResourceAsStream(resourcePath)!!))
			val assets = Bitser(false).deserialize(Content::class.java, input, Bitser.BACKWARD_COMPATIBLE)
			input.close()

			return assets
		}
	}
}
