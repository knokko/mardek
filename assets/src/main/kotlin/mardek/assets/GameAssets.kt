package mardek.assets

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.io.BitInputStream
import com.github.knokko.bitser.serialize.Bitser
import mardek.assets.area.OptimizedArea
import mardek.assets.characters.PlayableCharacter
import mardek.assets.combat.CombatAssets
import mardek.assets.skill.SkillAssets
import java.io.BufferedInputStream

@BitStruct(backwardCompatible = false)
class GameAssets(
	@BitField(ordering = 0)
	val combat: CombatAssets,

	@BitField(ordering = 1)
	val skills: SkillAssets,

	@BitField(ordering = 2)
	val areas: ArrayList<OptimizedArea>,

	@BitField(ordering = 3)
	val playableCharacters: ArrayList<PlayableCharacter>
) {

	@Suppress("unused")
	private constructor() : this(CombatAssets(), SkillAssets(), arrayListOf(), arrayListOf())

	companion object {
		fun load(resourcePath: String): GameAssets {
			val input = BitInputStream(BufferedInputStream(GameAssets::class.java.classLoader.getResourceAsStream(resourcePath)))
			val assets = Bitser(false).deserialize(GameAssets::class.java, input)
			input.close()

			return assets
		}
	}
}
