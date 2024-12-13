package mardek.game

import mardek.assets.Campaign

fun selfTest1() {
	val assets = Campaign.load("mardek/game/areas.bin")
	println("There are ${assets.areas.areas.size} areas in areas.bin")
	if (assets.areas.areas.size < 258) throw Error("Not enough areas: expected at least 258")
}
