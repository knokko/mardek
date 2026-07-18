package mardek.importer.story

import mardek.content.animation.ColorTransform
import mardek.content.audio.MusicTrack
import mardek.content.story.CustomTimelineVariable
import mardek.content.story.StoryContent

internal fun hardcodeCustomVariables(content: StoryContent) {
	content.customVariables.add(CustomTimelineVariable<String>("TimeOfDay"))
	content.customVariables.add(CustomTimelineVariable<ColorTransform>("TimeOfDayAmbience"))
	content.customVariables.add(CustomTimelineVariable<MusicTrack?>("GoznorMusic"))
	content.customVariables.add(CustomTimelineVariable<Unit>("WithDeuganBeforeFallingStar"))
	content.customVariables.add(CustomTimelineVariable<Boolean>("SpawnPoshGoblin"))
	content.customVariables.add(CustomTimelineVariable<Boolean>("SpawnMugbert"))
	content.customVariables.add(CustomTimelineVariable<Unit>("WithDeuganAfterRohoph"))
	content.customVariables.add(CustomTimelineVariable<MusicTrack?>("CastleGoznorMusic"))
	content.customVariables.add(CustomTimelineVariable<MusicTrack?>("CastleGoznorHallMusic"))
	content.customVariables.add(CustomTimelineVariable<MusicTrack?>("CastleGoznorThroneMusic"))
	content.customVariables.add(CustomTimelineVariable<MusicTrack?>("GemMinesMurianceRoomMusic"))
	content.customVariables.add(CustomTimelineVariable<MusicTrack?>("CatacombsMoricRoomMusic"))
	content.customVariables.add(CustomTimelineVariable<MusicTrack?>("CanoniaMusic"))
	content.customVariables.add(CustomTimelineVariable<MusicTrack?>("CanoniaInnMusic"))
	content.customVariables.add(CustomTimelineVariable<MusicTrack?>("CanoniaCaveMusic"))
	content.customVariables.add(CustomTimelineVariable<MusicTrack?>("GrottoBossRoomMusic"))
	content.customVariables.add(CustomTimelineVariable<MusicTrack?>("CambriaArenaAreaMusic"))
	content.customVariables.add(CustomTimelineVariable<MusicTrack?>("MoricShipMusic"))
	content.customVariables.add(CustomTimelineVariable<MusicTrack?>("MoricShipBossRoomMusic"))
	content.customVariables.add(CustomTimelineVariable<MusicTrack?>("XantusiaCityHallMusic"))
	content.customVariables.add(CustomTimelineVariable<MusicTrack?>("DarkCrystalRoomMusic"))
	content.customVariables.add(CustomTimelineVariable<MusicTrack?>("AeropolisMusic"))
	content.customVariables.add(CustomTimelineVariable<MusicTrack?>("AeropolisInnMusic"))
	content.customVariables.add(CustomTimelineVariable<MusicTrack?>("WaterCrystalRoomMusic"))
	content.customVariables.add(CustomTimelineVariable<MusicTrack?>("FireCrystalRoomMusic"))
	content.customVariables.add(CustomTimelineVariable<MusicTrack?>("LostMonasteryMusic"))
	content.customVariables.add(CustomTimelineVariable<MusicTrack?>("LostMonasteryBossRoomMusic"))
	content.customVariables.add(CustomTimelineVariable<MusicTrack?>("EarthCrystalRoomMusic"))
}
