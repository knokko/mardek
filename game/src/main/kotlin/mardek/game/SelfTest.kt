package mardek.game

fun selfTest1() {
	val content = loadContent()
	println("There are ${content.areas.areas.size} areas in content.bin")
	if (content.areas.areas.size < 258) throw Error("Not enough areas: expected at least 258")
}
