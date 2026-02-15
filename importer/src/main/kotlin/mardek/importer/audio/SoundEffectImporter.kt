package mardek.importer.audio

import mardek.content.audio.AudioContent
import mardek.content.audio.SoundEffect
import java.util.UUID

private fun importHitSound(audio: AudioContent, number: Int, name: String) {
	audio.effects.add(SoundEffect(
		"hit_$name",
		importSoundData("${number}_sfx_hit_$name"),
		UUID.nameUUIDFromBytes("SoundEffectImporter.importHitSound$name".encodeToByteArray()),
	))
}

private fun importSimpleSound(audio: AudioContent, number: Int, name: String) {
	audio.effects.add(SoundEffect(
		name,
		importSoundData("${number}_sfx_$name"),
		UUID.nameUUIDFromBytes("SoundEffectImporter.importSimpleSound$name".encodeToByteArray())
	))
}

internal fun importSoundEffects(audio: AudioContent) {
	importHitSound(audio, 5403, "2HSWORDS")
	importHitSound(audio, 5404, "MARTIAL")
	importHitSound(audio, 5405, "POLEARMS")
	importHitSound(audio, 5415, "STAVES")
	importHitSound(audio, 5416, "GUNS")
	importHitSound(audio, 5418, "BLASTERS")

	importSimpleSound(audio, 1, "slash2")
	importSimpleSound(audio, 8, "bubble1b")
	importSimpleSound(audio, 26, "boom3")
	importSimpleSound(audio, 5395, "Bubble")
	importSimpleSound(audio, 5397, "buy")
	importSimpleSound(audio, 5398, "Cure")
	importSimpleSound(audio, 5399, "Earth1")
	importSimpleSound(audio, 5402, "Gust")
	importSimpleSound(audio, 5406, "bite")
	importSimpleSound(audio, 5407, "Explosion")
	importSimpleSound(audio, 5409, "monster_undead")
	importSimpleSound(audio, 5410, "MPRestore")
	importSimpleSound(audio, 5413, "Thunder")
	importSimpleSound(audio, 5414, "dragon_roar")
	importSimpleSound(audio, 5419, "weird")
	importSimpleSound(audio, 5422, "Poison")
	importSimpleSound(audio, 5425, "Curse")
	importSimpleSound(audio, 5426, "unlock")
	importSimpleSound(audio, 5431, "bolt1")
	importSimpleSound(audio, 5432, "flame1")
	importSimpleSound(audio, 5433, "stone1")
	importSimpleSound(audio, 5435, "laserbeam")
	importSimpleSound(audio, 5437, "harpsting")
	importSimpleSound(audio, 5439, "wolf_howl")
	importSimpleSound(audio, 5440, "ragechord")
	importSimpleSound(audio, 5441, "earthquake")
}
