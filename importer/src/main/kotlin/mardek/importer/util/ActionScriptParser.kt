package mardek.importer.util

import mardek.importer.area.AreaParseException
import mardek.importer.area.HexObject
import mardek.importer.area.parseAssert
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.streams.toList

@Suppress("UNCHECKED_CAST")
fun parseActionScriptNestedList(rawList: String): Any {
	val content = rawList.codePoints().toList()
	var depth = 0
	var insideString = false

	val stack = ArrayList<Any>(1)
	val currentString = StringBuilder()

	fun getCurrentList(): ArrayList<Any> {
		var current = stack
		var currentDepth = 0
		while (currentDepth < depth) {
			current = current.last() as ArrayList<Any>
			currentDepth += 1
		}
		return current
	}

	for (character in content) {
		if (character == '"'.code) insideString = !insideString

		if (!insideString) {
			if (character == '['.code) {
				getCurrentList().add(ArrayList<Any>())
				depth += 1
				continue
			}
			if (character == ','.code || character == ']'.code) {
				val nextString = currentString.toString().trim()
				if (nextString.isNotEmpty()) getCurrentList().add(nextString)
				currentString.clear()
				if (character == ']'.code) depth -= 1
				continue
			}
		}

		currentString.appendCodePoint(character)
	}

	if (depth != 0) throw IllegalStateException("Ended at unexpected depth $depth with stack $stack")
	val lastString = currentString.toString().trim()
	if (lastString.isNotEmpty()) getCurrentList().add(lastString)

	return stack[0]
}

fun parseActionScriptObjectList(rawList: String): List<Map<String, String>> {

	val content = rawList.codePoints().toList()
	var depth = 0

	val STATE_KEY = 0
	val STATE_VALUE = 1
	var state = STATE_KEY

	var insideString = false

	val keyStorage = StringBuilder()
	val valueStorage = StringBuilder()
	val objectList = mutableListOf<Map<String, String>>()
	val currentObject = mutableMapOf<String, String>()

	for ((index, character) in content.withIndex()) {
		if (character == '"'.code && content[index - 1] != '\\'.code) insideString = !insideString

		if (depth == 2 && !insideString) {
			if (character == ','.code || character == '}'.code) {
				val key = keyStorage.toString().trim()
				if (key.isNotEmpty()) {
					currentObject[key] = valueStorage.toString().trim()
					keyStorage.clear()
					valueStorage.clear()
				}
				state = STATE_KEY
				if (character == ','.code) continue
			}

			if (character == ':'.code) {
				state = STATE_VALUE
				continue
			}
		}

		if ((character == '}'.code || character == ']'.code || character == ')'.code) && !insideString) {
			depth -= 1
			if (depth == 1) {
				objectList.add(HashMap(currentObject))
				currentObject.clear()
			}
		}

		if (depth >= 2) {
			if (state == STATE_KEY) keyStorage.appendCodePoint(character)
			else valueStorage.appendCodePoint(character)
		}

		if (!insideString && (character == '{'.code || character == '['.code || character == '('.code)) depth += 1
	}

	parseAssert(depth == 0, "Expected to end with depth 0, but got $depth")
	return objectList.toList()
}

fun parseActionScriptCode(lines: List<String>): ActionScriptCode {
	val content = lines.flatMap { it.replace("\r", "").replace("\n", "").codePoints().toList() }
	var depth = 0

	val variableAssignments = mutableMapOf<String, String>()
	val functionCalls = mutableListOf<Pair<String, String>>()

	val memory1 = StringBuilder()
	val memory2 = StringBuilder()
	var state = ParseState.Initial

	for (character in content) {
		if (depth == 0) {
			if (character == ' '.code && !state.isDeep) {
				val mem1 = memory1.toString()
				if (mem1 != "var" && mem1 != "else") {
					state = when (state) {
						ParseState.Initial -> ParseState.Equals
						ParseState.BeforeValue -> ParseState.Value
						ParseState.BeforeIfBody -> ParseState.BeforeIfBody
						else -> throw AreaParseException("Unexpected whitespace at state $state at depth 0")
					}
					continue
				}
			}
			if (character == '('.code && !state.isDeep) {
				parseAssert(state == ParseState.Initial, "Unexpected ( at state $state at depth 0")
				val isControl = when (memory1.toString().trim()) {
					"if" -> true
					"else if" -> true
					"while" -> true
					"for" -> true
					else -> false
				}
				state = if (isControl) {
					depth += 1
					ParseState.IfCondition
				} else ParseState.Parameters
				continue
			}
			if (character == '{'.code && (state == ParseState.BeforeIfBody ||
						(state == ParseState.Initial && memory1.toString().trim() == "else"))
			) {
				state = ParseState.InsideIfBody
				depth += 1
				continue
			}
			if (character == ')'.code && state == ParseState.Parameters) continue
			if (character == '='.code && state != ParseState.Value) {
				parseAssert(state == ParseState.Equals, "Unexpected = at state $state at depth 0")
				state = ParseState.BeforeValue
				continue
			}
			if (character == ';'.code) {
				when (state) {
					ParseState.Value -> variableAssignments[memory1.toString()] = memory2.toString()
					ParseState.Parameters -> functionCalls.add(Pair(memory1.toString(), memory2.toString()))
					ParseState.Initial -> parseAssert(memory1.startsWith("var "), "Unexpected ; at depth 0 after $memory1")
					else -> throw AreaParseException("Unexpected ; at depth 0 at state $state")
				}
				memory1.clear()
				memory2.clear()
				state = ParseState.Initial
				continue
			}
		}

		if (character == '['.code || character == '{'.code) depth += 1
		if (character == ']'.code || character == '}'.code) depth -= 1
		if (character == '('.code && state == ParseState.IfCondition) depth += 1
		if (character == ')'.code && state == ParseState.IfCondition) {
			depth -= 1
			if (depth == 0) state = ParseState.BeforeIfBody
		}
		if (character == '}'.code && state == ParseState.InsideIfBody && depth == 0) {
			state = ParseState.Initial
			memory1.clear()
			memory2.clear()
			continue
		}

		if (state == ParseState.BeforeIfBody) continue

		val memory = when (state) {
			ParseState.Initial -> memory1
			ParseState.Value -> memory2
			ParseState.Parameters -> memory2
			ParseState.IfCondition -> memory1
			ParseState.InsideIfBody -> memory2
			else -> throw AreaParseException("Unexpected state $state with memory $memory1 and $memory2")
		}
		memory.appendCodePoint(character)
	}

	parseAssert(depth == 0, "Expected to end at depth = 0, but ended up at depth = $depth")
	return ActionScriptCode(variableAssignments, functionCalls)
}

private enum class ParseState(val isDeep: Boolean) {
	Initial(false),
	Equals(false),
	BeforeValue(false),
	Value(true),
	Parameters(true),
	IfCondition(true),
	BeforeIfBody(false),
	InsideIfBody(true),
}

class ActionScriptCode(val variableAssignments: Map<String, String>, val functionCalls: List<Pair<String, String>>)

fun parseActionScriptResource(path: String): ActionScriptCode {
	val scanner = Scanner(HexObject::class.java.classLoader.getResourceAsStream(path))
	val lines = mutableListOf<String>()
	while (scanner.hasNextLine()) lines.add(scanner.nextLine())
	scanner.close()

	return parseActionScriptCode(lines)
}

fun parseActionScriptObject(rawObject: String): Map<String, String> {
	val smallList = parseActionScriptObjectList("[$rawObject]")
	if (smallList.size != 1) throw IllegalArgumentException("Expected a list of size 1, but got $smallList")
	return smallList[0]
}
