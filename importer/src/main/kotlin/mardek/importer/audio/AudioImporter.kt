package mardek.importer.audio

import mardek.content.audio.AudioContent

internal fun importAudioContent(audio: AudioContent) {
	importFixedSoundEffects(audio)
	importSoundEffects(audio)
	hardcodeMusic(audio)
	audio.titleScreenTrack = audio.musicTracks.find { it.fileName == "Theme" }!!
	audio.gameOverTrack = audio.musicTracks.find { it.fileName == "GameOver" }!!
	audio.defaultBattleTrack = audio.musicTracks.find { it.fileName == "battle" }!!
	audio.defaultVictoryTrack = audio.musicTracks.find { it.fileName == "VictoryFanfare" }!!
}
