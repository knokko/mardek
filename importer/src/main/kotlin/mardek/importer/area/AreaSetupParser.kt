package mardek.importer.area

import mardek.assets.area.AreaFlags
import kotlin.streams.toList

fun parseAreaSetup(rawParameters: String): Map<String, String> {
	val content = rawParameters.codePoints().toList()

	val entries = mutableMapOf<String, String>()

	val keyStorage = StringBuilder()
	val valueStorage = StringBuilder()

	var state = State.Key
	var depth = 0

	for (character in content) {
		if (depth == 1) {
			if (character == ':'.code) {
				parseAssert(state == State.Key, "Unexpected : in state $state at depth 0")
				state = State.Value
				continue
			}
			if (character == ','.code || (character == '}'.code && keyStorage.isNotEmpty())) {
				parseAssert(state == State.Value, "Unexpected , in state $state at depth 0")
				state = State.Key
				entries[keyStorage.toString()] = valueStorage.toString()
				keyStorage.clear()
				valueStorage.clear()
				if (character == '}'.code) depth = 0
				continue
			}
		}

		if (depth > 0) {
			if (state == State.Key) keyStorage.appendCodePoint(character)
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

private enum class State {
	Key,
	Value
}