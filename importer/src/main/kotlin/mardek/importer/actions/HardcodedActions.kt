package mardek.importer.actions

import mardek.content.Content
import mardek.content.action.ActionSequence

private val hardcoded = mutableMapOf<String, MutableList<ActionSequence>>()

internal fun hardcodeActionSequences(content: Content) {
	hardcoded[""] = mutableListOf()
	hardcodeSaveCrystalActions(content, hardcoded)
	hardcodeDragonLairActions(hardcoded)
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
