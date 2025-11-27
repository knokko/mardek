package mardek.content.inventory

import com.github.knokko.bitser.BitEnum

/**
 * The types of slots in which players (or monsters) can equip weapons, armor, and accessories.
 */
@BitEnum(mode = BitEnum.Mode.Ordinal)
enum class EquipmentSlotType {

	/**
	 * The main hand equipment slot, normally used for weapons.
	 */
	MainHand,

	/**
	 * The offhand equipment slot, which is often used for shields.
	 */
	OffHand,

	/**
	 * The head equipment slot, which is often used for hats/helmets.
	 */
	Head,

	/**
	 * The body equipment slot, normally used for armor or robes.
	 */
	Body,

	/**
	 * The accessory equipment slot.
	 */
	Accessory
}
