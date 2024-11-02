package mardek.assets.area.objects

class AreaObjects(
	val transitions: List<AreaTransition>,
	val walkTriggers: List<AreaTrigger>,
	val talkTriggers: List<AreaTalkTrigger>,
	val decorations: List<AreaDecoration>, // TODO Hm... I'm afraid I need the tilesheet for this
	val objects: List<AreaObject>,
	val characters: List<AreaCharacter>,
	val portals: List<AreaPortal>,
	val doors: List<AreaDoor>,
	val switchOrbs: List<AreaSwitchOrb>,
	val switchPlatforms: List<AreaSwitchPlatform>,
	val switchGates: List<AreaSwitchGate>,
) {
}
