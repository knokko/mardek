package mardek.assets.area.objects

class AreaObjects(
	val transitions: List<AreaTransition>,
	val walkTriggers: List<AreaTrigger>,
	val talkTriggers: List<AreaTalkTrigger>,
	val shops: List<AreaShop>,
	val decorations: List<AreaDecoration>,
	val objects: List<AreaObject>,
	val characters: List<AreaCharacter>,
	val portals: List<AreaPortal>,
	val doors: List<AreaDoor>,
	val switchOrbs: List<AreaSwitchOrb>,
	val switchPlatforms: List<AreaSwitchPlatform>,
	val switchGates: List<AreaSwitchGate>,
) {
}
