package mardek.importer.audio

import mardek.content.audio.AudioContent
import mardek.content.audio.SoundEffect

private fun importHitSound(audio: AudioContent, number: Int, name: String) {
	audio.effects.add(SoundEffect("hit_$name", importSoundData("${number}_sfx_hit_$name")))
}

private fun importSimpleSound(audio: AudioContent, number: Int, name: String) {
	audio.effects.add(SoundEffect(name, importSoundData("${number}_sfx_$name")))
}

internal fun importSoundEffects(audio: AudioContent) {
	importHitSound(audio, 5403, "2HSWORDS")
	importHitSound(audio, 5404, "MARTIAL")
	importHitSound(audio, 5405, "POLEARMS")
	importHitSound(audio, 5415, "STAVES")
	importHitSound(audio, 5416, "GUNS")
	importHitSound(audio, 5418, "BLASTERS")

	importSimpleSound(audio, 1, "slash2")
	importSimpleSound(audio, 5406, "bite")
	importSimpleSound(audio, 5432, "flame1")
}
