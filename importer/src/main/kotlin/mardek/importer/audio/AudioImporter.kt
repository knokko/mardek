package mardek.importer.audio

import mardek.content.audio.AudioContent

internal fun importAudioContent(audio: AudioContent) {
	importFixedSoundEffects(audio)
	importSoundEffects(audio)
}
