package mardek.importer.audio

import mardek.content.audio.AudioContent
import mardek.content.audio.SoundEffect

private fun importHitSound(audio: AudioContent, number: Int, name: String) {
	audio.effects.add(SoundEffect("hit_$name", importSoundData("${number}_sfx_hit_$name")))
}

internal fun importSoundEffects(audio: AudioContent) {
	importHitSound(audio, 5403, "2HSWORDS")
	importHitSound(audio, 5404, "MARTIAL")
	importHitSound(audio, 5405, "POLEARMS")
	importHitSound(audio, 5415, "STAVES")
	importHitSound(audio, 5416, "GUNS")
	importHitSound(audio, 5418, "BLASTERS")
}
