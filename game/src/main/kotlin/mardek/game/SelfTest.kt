package mardek.game

import mardek.assets.GameAssets

fun selfTest1() {
	val assets = GameAssets.load("mardek/game/areas.bin")
	println("There are ${assets.areas.size} areas in areas.bin")
	if (assets.areas.size < 258) throw Error("Not enough areas: expected at least 258")
}
