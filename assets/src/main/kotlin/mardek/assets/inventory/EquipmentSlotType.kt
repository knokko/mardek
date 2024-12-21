package mardek.assets.inventory

import com.github.knokko.bitser.BitEnum

@BitEnum(mode = BitEnum.Mode.UniformOrdinal)
enum class EquipmentSlotType {

	MainHand,
	OffHand,
	Head,
	Body,
	Accessory
}
