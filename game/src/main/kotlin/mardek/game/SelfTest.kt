package mardek.game

import mardek.content.Content

fun selfTest1() {
	val content = Content.load()
	println("There are ${content.areas.areas.size} areas in content.bin")
	if (content.areas.areas.size < 258) throw Error("Not enough areas: expected at least 258")
}
