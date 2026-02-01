package mardek.importer.actions

import com.github.knokko.bitser.Bitser
import mardek.content.Content
import mardek.content.action.ActionSequence
import mardek.content.action.ActionTargetAreaCharacter
import mardek.content.action.ActionTimelineTransition
import mardek.content.action.ActionToArea
import mardek.content.area.objects.AreaCharacter
import java.util.UUID

class HardcodedActions {

	private val hardcoded = mutableMapOf<String, MutableList<ActionSequence>>()

	internal fun hardcodeActionSequences(content: Content) {
		hardcoded[""] = mutableListOf()
		hardcoded["goznor"] = mutableListOf()
		hardcodeSaveCrystalActions(content, hardcoded)
		hardcodeDoorActions(hardcoded)
		hardcodeDragonLairActions(content, hardcoded)
	}

	internal fun getHardcodedAreaActions(areaName: String, sequenceName: String) = hardcoded[areaName]?.find {
		it.name == sequenceName
	}

	internal fun getHardcodedGlobalActionSequence(name: String) = getHardcodedAreaActions("", name)

	internal fun storeHardcodedActionSequences(content: Content) {
		for ((areaName, actionSequences) in hardcoded) {
			if (areaName == "") {
				content.actions.global.addAll(actionSequences)
			} else {
				val area = content.areas.areas.find { it.properties.rawName == areaName }!!
				area.actions.addAll(actionSequences)
			}
		}
	}

	internal fun resolveIncompleteActions(content: Content) {
		val bitser = Bitser(false)
		val allAreaTargets = mutableListOf<ActionTargetAreaCharacter>()
		val allToAreaActions = mutableListOf<ActionToArea>()
		val allTimelineTransitions = mutableListOf<ActionTimelineTransition>()
		val collectedInstances = mutableMapOf<Class<*>, Collection<*>>()
		collectedInstances[ActionTargetAreaCharacter::class.java] = allAreaTargets
		collectedInstances[ActionToArea::class.java] = allToAreaActions
		collectedInstances[ActionTimelineTransition::class.java] = allTimelineTransitions
		bitser.collectInstances(content, collectedInstances)

		val characterMapping = mutableMapOf<UUID, AreaCharacter>()
		for (area in content.areas.areas) {
			for (character in area.objects.characters) characterMapping[character.id] = character
		}

		for (target in allAreaTargets) target.resolve(characterMapping)
		for (toArea in allToAreaActions) toArea.resolve(content.areas.areas)
		for (transition in allTimelineTransitions) transition.resolve(content.story.timelines)
	}
}

