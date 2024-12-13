package mardek.assets

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceFieldTarget
import com.github.knokko.bitser.io.BitInputStream
import com.github.knokko.bitser.serialize.Bitser
import mardek.assets.area.Area
import mardek.assets.area.AreaAssets
import mardek.assets.characters.PlayableCharacter
import mardek.assets.combat.CombatAssets
import mardek.assets.skill.SkillAssets
import java.io.BufferedInputStream

@BitStruct(backwardCompatible = false)
class Campaign(
	@BitField(ordering = 0)
	val combat: CombatAssets,

	@BitField(ordering = 1)
	val skills: SkillAssets,

	@BitField(ordering = 2)
	val areas: AreaAssets,

	@BitField(ordering = 3)
	@ReferenceFieldTarget(label = "playable characters")
	val playableCharacters: ArrayList<PlayableCharacter>,

	@BitField(ordering = 4)
	@NestedFieldSetting(path = "v", writeAsBytes = true)
	val checkpoints: HashMap<String, ByteArray>,
) {

	@Suppress("unused")
	private constructor() : this(CombatAssets(), SkillAssets(), AreaAssets(), arrayListOf(), HashMap())

	companion object {
		fun load(resourcePath: String): Campaign {
			val input = BitInputStream(BufferedInputStream(Campaign::class.java.classLoader.getResourceAsStream(resourcePath)))
			val assets = Bitser(false).deserialize(Campaign::class.java, input)
			input.close()

			return assets
		}
	}
}
