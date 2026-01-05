package mardek.importer.actions

import mardek.content.Content
import mardek.content.action.ActionNode
import mardek.content.action.ActionSequence
import mardek.content.action.ActionTargetAreaCharacter
import mardek.content.action.ActionTimelineTransition
import mardek.content.action.ActionToArea
import mardek.content.action.ChoiceActionNode
import mardek.content.action.FixedActionNode
import mardek.content.area.objects.AreaCharacter
import java.util.UUID

class HardcodedActions {

	private val hardcoded = mutableMapOf<String, MutableList<ActionSequence>>()

	internal fun hardcodeActionSequences(content: Content) {
		hardcoded[""] = mutableListOf()
		hardcodeSaveCrystalActions(content, hardcoded)
		hardcodeDragonLairActions(content, hardcoded)
	}

	internal fun getHardcodedAreaActions(areaName: String, sequenceName: String) = hardcoded[areaName]?.find {
		it.name == sequenceName
	}

	internal fun getHardcodedGlobalActionSequence(name: String) = getHardcodedAreaActions("", name)

	internal fun storeHardcodedActionSequences(content: Content) {
		val allActionNodes = mutableListOf<ActionNode>()

		for ((areaName, actionSequences) in hardcoded) {
			for (sequence in actionSequences) {
				generateUUIDs(sequence)
				allActionNodes.addAll(getAllActionNodesFromSequence(sequence))
			}
			if (areaName == "") {
				content.actions.global.addAll(actionSequences)
			} else {
				val area = content.areas.areas.find { it.properties.rawName == areaName }!!
				area.actions.addAll(actionSequences)
			}
		}

		val characterMapping = mutableMapOf<UUID, AreaCharacter>()
		for (area in content.areas.areas) {
			for (character in area.objects.characters) characterMapping[character.id] = character
		}

		for (node in allActionNodes) {
			if (node is FixedActionNode) {
				val action = node.action
				for (target in action.getTargets()) {
					if (target is ActionTargetAreaCharacter) target.resolve(characterMapping)
				}
				if (action is ActionToArea) action.resolve(content.areas.areas)
				if (action is ActionTimelineTransition) action.resolve(content.story.timelines)
			}
			if (node is ChoiceActionNode) {
				val speaker = node.speaker
				if (speaker is ActionTargetAreaCharacter) speaker.resolve(characterMapping)
			}
		}
	}
}

