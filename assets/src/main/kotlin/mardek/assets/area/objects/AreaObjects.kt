package mardek.assets.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField

@BitStruct(backwardCompatible = false)
class AreaObjects(
	@BitField(ordering = 0)
	val characters: ArrayList<AreaCharacter>,

	// TODO Code reuse: create abstract StaticAreaObject class with (x, y) that most objects extend
	@BitField(ordering = 1)
	val decorations: ArrayList<AreaDecoration>,

	@BitField(ordering = 2)
	val doors: ArrayList<AreaDoor>,

	@BitField(ordering = 3)
	val objects: ArrayList<AreaObject>,

	@BitField(ordering = 4)
	val portals: ArrayList<AreaPortal>,

	@BitField(ordering = 5)
	val shops: ArrayList<AreaShop>,

	@BitField(ordering = 6)
	val switchGates: ArrayList<AreaSwitchGate>,

	@BitField(ordering = 7)
	val switchOrbs: ArrayList<AreaSwitchOrb>,

	@BitField(ordering = 8)
	val switchPlatforms: ArrayList<AreaSwitchPlatform>,

	@BitField(ordering = 9)
	val talkTriggers: ArrayList<AreaTalkTrigger>,

	@BitField(ordering = 10)
	val transitions: ArrayList<AreaTransition>,

	@BitField(ordering = 11)
	val walkTriggers: ArrayList<AreaTrigger>,
) {
	internal constructor() : this(
		ArrayList(), ArrayList(), ArrayList(), ArrayList(), ArrayList(), ArrayList(),
		ArrayList(), ArrayList(), ArrayList(), ArrayList(), ArrayList(), ArrayList()
	)
}
