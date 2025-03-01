package mardek.assets.inventory

import com.github.knokko.bitser.BitEnum

@BitEnum(mode = BitEnum.Mode.Ordinal)
enum class EquipmentSlotType {

	MainHand,
	OffHand,
	Head,
	Body,
	Accessory
}
