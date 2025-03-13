package mardek.content.area

import com.github.knokko.bitser.BitEnum

@BitEnum(mode = BitEnum.Mode.Ordinal)
enum class WaterType {
	None,
	Skipped,
	Water,
	Lava,
	Waterfall
}
