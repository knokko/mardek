package mardek.importer.particle

import mardek.content.Content
import mardek.content.particle.ParticleEffect
import mardek.content.particle.ParticleInheritance
import mardek.content.particle.ParticleQuake
import mardek.importer.area.parseFlashString
import mardek.importer.audio.getSoundByName
import mardek.importer.util.parseActionScriptNestedList
import mardek.importer.util.parseActionScriptObject
import mardek.importer.util.parseActionScriptObjectList
import mardek.importer.util.parseActionScriptResource
import java.lang.Float.parseFloat
import java.lang.Integer.parseInt

internal const val FLASH_FRAMES_PER_SECOND = 30
internal const val FLASH_FRAME = 1f / FLASH_FRAMES_PER_SECOND

internal fun importParticleEffects(content: Content) {
	val code = parseActionScriptResource("mardek/importer/particle/particles.txt")
	val rawParticles = parseActionScriptObject(code.variableAssignments["PFXList"]!!)
	for ((name, rawEffect) in rawParticles) {
		if (rawEffect.contains("derive:")) continue
		content.battle.particles.add(parseParticleEffect(content, name.replace("pfx_", ""), rawEffect))
	}
	for ((name, rawEffect) in rawParticles) {
		if (!rawEffect.contains("derive:")) continue
		content.battle.particles.add(parseParticleEffect(content, name.replace("pfx_", ""), rawEffect))
	}
}

private fun parseParticleEffect(content: Content, name: String, rawEffect: String): ParticleEffect {
	val effectProperties = parseActionScriptObject(rawEffect)

	val rawInitialSound = effectProperties["sfx"]
	val initialSound = if (rawInitialSound != null && rawInitialSound != "null" && rawInitialSound != "\"cure1\"" && rawInitialSound != "\"jolt\"" && rawInitialSound != "\"sunbeam\"") {
		val soundName = parseFlashString(rawInitialSound, "effect sound (initial)")!!
		getSoundByName(content, soundName)
	} else null

	val rawDamageDelay = effectProperties["dmgdelay"]
	val damageDelay = if (rawDamageDelay != null) FLASH_FRAME * parseInt(rawDamageDelay) else 0f

	val rawDamageSound = effectProperties["delayedSfx"]
	val damageSound = if (rawDamageSound != null) {
		val soundName = parseFlashString(rawDamageSound, "effect sound (damage)")!!
		getSoundByName(content, soundName)
	} else null

	val rawQuake = effectProperties["quake"]
	val quake = if (rawQuake != null) {
		val quakeList = parseActionScriptNestedList(rawQuake)
		if (quakeList !is ArrayList<*>) throw IllegalArgumentException("Unexpected quake $rawQuake")
		ParticleQuake(
			strength = parseInt(quakeList[0].toString()),
			duration = FLASH_FRAME * parseInt(quakeList[1].toString()),
			decay = parseFloat(quakeList[2].toString()) * FLASH_FRAMES_PER_SECOND
		)
	} else null

	val rawSoundDelays = effectProperties["countSfx"]
	val extraSoundDelays = if (rawSoundDelays != null) {
		val rawDelayList = parseActionScriptNestedList(rawSoundDelays)
		if (rawDelayList !is ArrayList<*>) throw IllegalArgumentException("Unexpected extra delays $rawSoundDelays")
		rawDelayList.map { rawDelay -> FLASH_FRAME * parseInt(rawDelay.toString()) }.toFloatArray()
	} else FloatArray(0)

	val rawDerived = effectProperties["derive"]
	val inheritance = if (rawDerived != null) {
		val parentName = parseFlashString(rawDerived, "derived particle name")!!
		val parent = content.battle.particles.find { it.name == parentName }!!
		val rawTypes = effectProperties["newTypes"]
		val overrideSprites = if (rawTypes != null) {
			val rawTypeList = parseActionScriptNestedList(rawTypes)
			if (rawTypeList !is ArrayList<*>) throw IllegalArgumentException("Unexpected newTypes $rawTypes")
			val overrideElements = ArrayList(rawTypeList.map { parseInt(it.toString()) })
			while (overrideElements.size > parent.emitters.size) overrideElements.removeLast()
			val rawSheets = effectProperties["newSheets"]
			val overrideSheets = if (rawSheets != null) {
				val rawSheetList = parseActionScriptNestedList(rawSheets)
				if (rawSheetList !is ArrayList<*>) throw IllegalArgumentException("Unexpected newSheets $rawTypes")
				rawSheetList.map { parseInt(it.toString()) }
			} else null
			overrideElements.mapIndexed { index, newElement ->
				val dummyEmitter = mutableMapOf(Pair("type", newElement.toString()))
				if (overrideSheets == null) dummyEmitter["sheet"] = parent.emitters[index].sprite.sprite.width.toString()
				else dummyEmitter["sheet"] = overrideSheets[index].toString()
				getParticleSprite(content, dummyEmitter)
			}.toTypedArray()
		} else null
		ParticleInheritance(parent = parent, overrideSprites = overrideSprites)
	} else null

	val rawEmitters = effectProperties["emitters"]
	val emitters = if (rawEmitters != null) {
		val emitterProperties = parseActionScriptObjectList(rawEmitters)
		ArrayList(emitterProperties.map { importParticleEmitter(content, it) })
	} else ArrayList(0)

	return ParticleEffect(
		name = name,
		damageDelay = damageDelay,
		initialSound = initialSound,
		damageSound = damageSound,
		quake = quake,
		extraSoundDelays = extraSoundDelays,
		inheritance = inheritance,
		emitters = emitters,
	)
}
