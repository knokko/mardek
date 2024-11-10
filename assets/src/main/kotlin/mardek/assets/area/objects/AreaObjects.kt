package mardek.assets.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.CollectionField

@BitStruct(backwardCompatible = false)
class AreaObjects(
	@BitField(ordering = 0)
	@CollectionField
	val characters: ArrayList<AreaCharacter>,

	// TODO Code reuse: create abstract StaticAreaObject class with (x, y) that most objects extend
	@BitField(ordering = 1)
	@CollectionField
	val decorations: ArrayList<AreaDecoration>,

	@BitField(ordering = 2)
	@CollectionField
	val doors: ArrayList<AreaDoor>,

	@BitField(ordering = 3)
	@CollectionField
	val objects: ArrayList<AreaObject>,

	@BitField(ordering = 4)
	@CollectionField
	val portals: ArrayList<AreaPortal>,

	@BitField(ordering = 5)
	@CollectionField
	val shops: ArrayList<AreaShop>,

	@BitField(ordering = 6)
	@CollectionField
	val switchGates: ArrayList<AreaSwitchGate>,

	@BitField(ordering = 7)
	@CollectionField
	val switchOrbs: ArrayList<AreaSwitchOrb>,

	@BitField(ordering = 8)
	@CollectionField
	val switchPlatforms: ArrayList<AreaSwitchPlatform>,

	@BitField(ordering = 9)
	@CollectionField
	val talkTriggers: ArrayList<AreaTalkTrigger>,

	@BitField(ordering = 10)
	@CollectionField
	val transitions: ArrayList<AreaTransition>,

	@BitField(ordering = 11)
	@CollectionField
	val walkTriggers: ArrayList<AreaTrigger>,
) {
	internal constructor() : this(
		ArrayList(), ArrayList(), ArrayList(), ArrayList(), ArrayList(), ArrayList(),
		ArrayList(), ArrayList(), ArrayList(), ArrayList(), ArrayList(), ArrayList()
	)
}
