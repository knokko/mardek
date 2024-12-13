package mardek.assets.area

import com.github.knokko.bitser.BitEnum

@BitEnum(mode = BitEnum.Mode.UniformOrdinal)
enum class WaterType {
	None,
	Skipped,
	Water,
	Lava,
	Waterfall
}
