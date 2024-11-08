package mardek.assets.area.objects

class AreaObjects(
	val characters: List<AreaCharacter>,
	val decorations: List<AreaDecoration>,
	val doors: List<AreaDoor>,
	val objects: List<AreaObject>,
	val portals: List<AreaPortal>,
	val shops: List<AreaShop>,
	val switchGates: List<AreaSwitchGate>,
	val switchOrbs: List<AreaSwitchOrb>,
	val switchPlatforms: List<AreaSwitchPlatform>,
	val talkTriggers: List<AreaTalkTrigger>,
	val transitions: List<AreaTransition>,
	val walkTriggers: List<AreaTrigger>,
) {
}
