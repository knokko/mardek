package mardek.importer.area

import mardek.content.area.AreaFlags
import kotlin.streams.toList

fun parseAreaSetup(rawParameters: String): Map<String, String> {
	val content = rawParameters.codePoints().toList()

	val entries = mutableMapOf<String, String>()

	val keyStorage = StringBuilder()
	val valueStorage = StringBuilder()

	val STATE_KEY = 0
	val STATE_VALUE = 1

	var state = STATE_KEY
	var depth = 0

	for (character in content) {
		if (depth == 1) {
			if (character == ':'.code) {
				parseAssert(state == STATE_KEY, "Unexpected : in state $state at depth 0")
				state = STATE_VALUE
				continue
			}
			if (character == ','.code || (character == '}'.code && keyStorage.isNotEmpty())) {
				parseAssert(state == STATE_VALUE, "Unexpected , in state $state at depth 0")
				state = STATE_KEY
				entries[keyStorage.toString()] = valueStorage.toString()
				keyStorage.clear()
				valueStorage.clear()
				if (character == '}'.code) depth = 0
				continue
			}
		}

		if (depth > 0) {
			if (state == STATE_KEY) keyStorage.appendCodePoint(character)
			else valueStorage.appendCodePoint(character)
		}

		if (character == '{'.code) depth += 1
		if (character == '}'.code) depth -= 1
	}

	parseAssert(depth == 0, "Ended at unexpected depth $depth")
	return entries
}

fun parseAreaFlags(entries: Map<String, String>): AreaFlags {
	return AreaFlags(
		canWarp = entries["WARP"] == "1",
		hasClearMap = entries["clearmap"] == "1",
		noMovingCamera = entries["noscroll"] == "true",
		hideParty = entries["hideparty"] == "true",
		noSwitch = entries["NoSwitch"] == "true",
		noMap = entries["NO_MAP"] == "true",
		// miasma can be either "true" or "1",
		// I don't know whether it means anything, or whether Tobias was just inconsistent
		miasma = entries["MIASMA"] == "true" || entries["MIASMA"] == "1",
		noStorage = entries["NoStorage"] == "true"
	)
}
