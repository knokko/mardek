package mardek.importer.audio

import mardek.importer.ui.BcPacker

internal fun importSoundData(name: String): ByteArray {
	val input = BcPacker::class.java.classLoader.getResourceAsStream("mardek/importer/audio/$name.ogg")!!
	val bytes = input.readAllBytes()
	input.close()
	return bytes
}
