package mardek.importer.story

import mardek.content.animation.ColorTransform
import mardek.content.story.CustomTimelineVariable
import mardek.content.story.StoryContent

internal fun hardcodeCustomVariables(content: StoryContent) {
	content.customVariables.add(CustomTimelineVariable<String>("TimeOfDay"))
	content.customVariables.add(CustomTimelineVariable<ColorTransform>("TimeOfDayAmbience"))
	content.customVariables.add(CustomTimelineVariable<String>("GoznorMusic"))
	content.customVariables.add(CustomTimelineVariable<Unit>("WithDeuganBeforeFallingStar"))
	content.customVariables.add(CustomTimelineVariable<String>("RohophSaucerMusic"))
	content.customVariables.add(CustomTimelineVariable<String>("CastleGoznorMusic"))
	content.customVariables.add(CustomTimelineVariable<String>("CastleGoznorHallMusic"))
	content.customVariables.add(CustomTimelineVariable<String>("CastleGoznorThroneMusic"))
	content.customVariables.add(CustomTimelineVariable<String>("GemMinesMurianceRoomMusic"))
	content.customVariables.add(CustomTimelineVariable<String>("CatacombsMoricRoomMusic"))
	content.customVariables.add(CustomTimelineVariable<String>("CanoniaMusic"))
	content.customVariables.add(CustomTimelineVariable<String>("CanoniaInnMusic"))
	content.customVariables.add(CustomTimelineVariable<String>("CanoniaCaveMusic"))
	content.customVariables.add(CustomTimelineVariable<String>("GrottoBossRoomMusic"))
	content.customVariables.add(CustomTimelineVariable<String>("CambriaArenaAreaMusic"))
	content.customVariables.add(CustomTimelineVariable<String>("MoricShipMusic"))
	content.customVariables.add(CustomTimelineVariable<String>("MoricShipBossRoomMusic"))
	content.customVariables.add(CustomTimelineVariable<String>("XantusiaCityHallMusic"))
	content.customVariables.add(CustomTimelineVariable<String>("DarkCrystalRoomMusic"))
	content.customVariables.add(CustomTimelineVariable<String>("AeropolisMusic"))
	content.customVariables.add(CustomTimelineVariable<String>("AeropolisInnMusic"))
	content.customVariables.add(CustomTimelineVariable<String>("WaterCrystalRoomMusic"))
	content.customVariables.add(CustomTimelineVariable<String>("FireCrystalRoomMusic"))
	content.customVariables.add(CustomTimelineVariable<String>("LostMonasteryMusic"))
	content.customVariables.add(CustomTimelineVariable<String>("LostMonasteryBossRoomMusic"))
	content.customVariables.add(CustomTimelineVariable<String>("EarthCrystalRoomMusic"))
}
