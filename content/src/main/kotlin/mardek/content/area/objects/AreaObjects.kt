package mardek.content.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceFieldTarget

@BitStruct(backwardCompatible = true)
class AreaObjects(
	@BitField(id = 0)
	val characters: ArrayList<AreaCharacter>,

	// TODO Code reuse: create abstract StaticAreaObject class with (x, y) that most objects extend
	@BitField(id = 1)
	val decorations: ArrayList<AreaDecoration>,

	@BitField(id = 2)
	val doors: ArrayList<AreaDoor>,

	@BitField(id = 3)
	val objects: ArrayList<AreaObject>,

	@BitField(id = 4)
	val portals: ArrayList<AreaPortal>,

	@BitField(id = 5)
	val shops: ArrayList<AreaShop>,

	@BitField(id = 6)
	val switchGates: ArrayList<AreaSwitchGate>,

	@BitField(id = 7)
	val switchOrbs: ArrayList<AreaSwitchOrb>,

	@BitField(id = 8)
	val switchPlatforms: ArrayList<AreaSwitchPlatform>,

	@BitField(id = 9)
	val talkTriggers: ArrayList<AreaTalkTrigger>,

	@BitField(id = 10)
	val transitions: ArrayList<AreaTransition>,

	@BitField(id = 11)
	@ReferenceFieldTarget(label = "area triggers")
	val walkTriggers: ArrayList<AreaTrigger>,
) {
	internal constructor() : this(
		ArrayList(), ArrayList(), ArrayList(), ArrayList(), ArrayList(), ArrayList(),
		ArrayList(), ArrayList(), ArrayList(), ArrayList(), ArrayList(), ArrayList()
	)
}
