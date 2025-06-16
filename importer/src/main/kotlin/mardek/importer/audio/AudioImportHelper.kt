package mardek.importer.audio

import mardek.content.Content
import mardek.importer.ui.BcPacker

internal fun importSoundData(name: String): ByteArray {
	val input = BcPacker::class.java.classLoader.getResourceAsStream("mardek/importer/audio/$name.ogg")!!
	val bytes = input.readAllBytes()
	input.close()
	return bytes
}

internal fun getSoundByName(content: Content, name: String) = when (name) {
	"slam" -> content.audio.fixedEffects.battle.critical
	"Slam" -> content.audio.fixedEffects.battle.critical
	"punch" -> content.audio.fixedEffects.battle.punch
	"menuOpen" -> content.audio.fixedEffects.ui.openMenu
	else -> content.audio.effects.find { it.flashName == name }!!
}

internal fun getOptionalSoundByName(content: Content, name: String?) = if (name == null) null
else getSoundByName(content, name)
