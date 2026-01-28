package mardek.importer.world

import mardek.content.Content
import mardek.content.area.Direction
import mardek.content.world.WorldMap
import mardek.content.world.WorldMapEdge
import mardek.content.world.WorldMapNode
import mardek.importer.area.timeOfDayMusic
import mardek.importer.util.loadBc7Sprite
import java.util.UUID

internal fun hardcodeWorldMap(content: Content) {

	fun entrance(name: String, x: Int, y: Int, direction: Direction): WorldMapNode.Entrance {
		return WorldMapNode.Entrance(
			id = UUID.nameUUIDFromBytes("$name $x $y".toByteArray()),
			area = content.areas.areas.find { it.properties.rawName == name }!!,
			x = x, y = y, direction = direction,
		)
	}

	val heroesDenEntrance = entrance("heroes_den", 6, 13, Direction.Up)
	val goznorEntrance = entrance("goznor", 18, 21, Direction.Up)
	val soothwoodEastEntrance = entrance("soothwood", 35, 22, Direction.Left)
	val soothwoodWestEntrance = entrance("soothwood", 6, 4, Direction.Right)
	val crashSiteEntrance = entrance("crashsite", 6, 21, Direction.Up)

	val nodes = mutableListOf<WorldMapNode>()

	fun addNode(x: Int, y: Int, entrances: Array<WorldMapNode.Entrance>): WorldMapNode {
		val node = WorldMapNode(
			id = UUID.nameUUIDFromBytes("WorldMapAreaNode${entrances[0].area.properties.displayName}".encodeToByteArray()),
			x = x, y = y, entrances = entrances,
		)
		nodes.add(node)
		return node
	}

	fun addDummyNode(name: String) {
		// TODO CHAP3 Stop using this
		val area = content.areas.areas.find { it.properties.rawName == name }!!
		val entrance = WorldMapNode.Entrance(
			id = UUID.nameUUIDFromBytes("WorldMapEntrance${name}".toByteArray()),
			area = area, x = 1, y = 1, direction = Direction.Up
		)
		val node = WorldMapNode(
			id = UUID.nameUUIDFromBytes("WorldMapAreaNode${name}".encodeToByteArray()),
			x = 1, y = 1, entrances = arrayOf(entrance),
		)
		nodes.add(node)
	}

	addDummyNode("gc_hall")
	addDummyNode("gemmine")
	addDummyNode("guardpost")
	addDummyNode("glens1")
	addDummyNode("lakequr")
	addDummyNode("canonia")
	addDummyNode("canonia_dreamcave")
	addDummyNode("canonia_woods")
	addDummyNode("pcave1")
	addDummyNode("trilobiteville")
	addDummyNode("sunTemple1")
	addDummyNode("warport1")
	addDummyNode("warport2")
	addDummyNode("aeropolis_S")
	addDummyNode("DesertPath")
	addDummyNode("lifewood1")
	addDummyNode("volcano1")
	addDummyNode("HouseInWoods")

	val heroesDen = addNode(470, 340, arrayOf(heroesDenEntrance))
	val goznor = addNode(600, 250, arrayOf(goznorEntrance))
	val soothwood = addNode(390, 200, arrayOf(soothwoodEastEntrance, soothwoodWestEntrance))
	val crashSite = addNode(210, 400, arrayOf(crashSiteEntrance))

	content.worldMaps.add(WorldMap(
		id = UUID.fromString("6357d438-0bcf-4b74-9039-7966417f7ea1"),
		name = "Belfan",
		sprite = loadBc7Sprite("mardek/importer/world/Belfan.png"),
		music = timeOfDayMusic(content, "WorldMap"),
		nodes = nodes.toTypedArray(),
		edges = arrayOf(
			WorldMapEdge(goznor, goznorEntrance, heroesDen, heroesDenEntrance),
			WorldMapEdge(goznor, goznorEntrance, soothwood, soothwoodEastEntrance),
			WorldMapEdge(crashSite, crashSiteEntrance, soothwood, soothwoodWestEntrance),
		)
	))
}
