package mardek.content

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceFieldTarget
import com.github.knokko.bitser.io.BitInputStream
import com.github.knokko.bitser.serialize.Bitser
import mardek.content.action.ActionContent
import mardek.content.area.AreaContent
import mardek.content.audio.AudioContent
import mardek.content.battle.BattleContent
import mardek.content.characters.PlayableCharacter
import mardek.content.stats.StatsContent
import mardek.content.inventory.ItemsContent
import mardek.content.portrait.PortraitContent
import mardek.content.skill.SkillsContent
import mardek.content.ui.Fonts
import mardek.content.ui.UiSprites
import java.io.BufferedInputStream

@BitStruct(backwardCompatible = true)
class Content {
	@BitField(id = 0)
	val audio = AudioContent()

	@BitField(id = 1)
	val stats = StatsContent()

	@BitField(id = 2)
	val skills = SkillsContent()

	@BitField(id = 3)
	val items = ItemsContent()

	@BitField(id = 4)
	val battle = BattleContent()

	@BitField(id = 5)
	val areas = AreaContent()

	@BitField(id = 6)
	val actions = ActionContent()

	@BitField(id = 7)
	@ReferenceFieldTarget(label = "playable characters")
	val playableCharacters = ArrayList<PlayableCharacter>()

	@BitField(id = 8)
	val portraits = PortraitContent()

	@BitField(id = 9)
	lateinit var ui: UiSprites

	@BitField(id = 10)
	lateinit var fonts: Fonts

	@BitField(id = 11)
	@NestedFieldSetting(path = "v", writeAsBytes = true)
	val checkpoints = HashMap<String, ByteArray>()

	companion object {
		fun load(resourcePath: String, bitser: Bitser): Content {
			val input = BitInputStream(BufferedInputStream(Content::class.java.classLoader.getResourceAsStream(resourcePath)!!))
			val assets = bitser.deserialize(Content::class.java, input, Bitser.BACKWARD_COMPATIBLE)
			input.close()

			return assets
		}
	}
}
