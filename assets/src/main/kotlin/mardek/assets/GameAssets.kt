package mardek.assets

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.CollectionField
import com.github.knokko.bitser.io.BitInputStream
import com.github.knokko.bitser.serialize.Bitser
import mardek.assets.area.OptimizedArea
import java.io.BufferedInputStream

@BitStruct(backwardCompatible = false)
class GameAssets(
	@BitField(ordering = 0)
	@CollectionField
	val areas: ArrayList<OptimizedArea>,

	@BitField(ordering = 1)
	@CollectionField
	val playableCharacters: ArrayList<PlayableCharacter>
) {

	@Suppress("unused")
	private constructor() : this(arrayListOf(), arrayListOf())

	companion object {
		fun load(resourcePath: String): GameAssets {
			val input = BitInputStream(BufferedInputStream(GameAssets::class.java.classLoader.getResourceAsStream(resourcePath)))
			val assets = Bitser(false).deserialize(GameAssets::class.java, input)
			input.close()

			return assets
		}
	}
}
