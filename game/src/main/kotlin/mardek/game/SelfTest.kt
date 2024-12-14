package mardek.game

import mardek.assets.Campaign

fun selfTest1() {
	val campaign = Campaign.load("mardek/game/campaign.bin")
	println("There are ${campaign.areas.areas.size} areas in campaign.bin")
	if (campaign.areas.areas.size < 258) throw Error("Not enough areas: expected at least 258")
}
