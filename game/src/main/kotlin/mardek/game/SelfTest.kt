package mardek.game

import mardek.content.BITSER
import mardek.content.Content

fun selfTest1() {
	val content = Content.load("mardek/game/content.bits", BITSER)
	println("There are ${content.areas.areas.size} areas in content.bin")
	if (content.areas.areas.size < 258) throw Error("Not enough areas: expected at least 258")
}
