package mardek.importer.world

import mardek.content.Content
import mardek.content.world.WorldMap
import mardek.content.world.WorldMapEdge
import mardek.content.world.WorldMapNode
import mardek.importer.util.loadBc7Sprite
import java.util.UUID

internal fun hardcodeWorldMap(content: Content) {

	val nodes = mutableListOf<WorldMapNode>()

	fun addNode(name: String, x: Int, y: Int): WorldMapNode {
		val node = WorldMapNode(
			id = UUID.nameUUIDFromBytes("WorldMapAreaNode$name".encodeToByteArray()),
			area = content.areas.areas.find { it.properties.displayName == name }!!,
			x = x, y = y
		)
		nodes.add(node)
		return node
	}

	val heroesDen = addNode("Heroes' Den", 470, 340)
	val goznor = addNode("Goznor", 600, 250)
	val soothwood = addNode("Soothwood", 390, 200)
	val crashSite = addNode("Crash Site", 210, 400)

	content.worldMaps.add(WorldMap(
		name = "Belfan",
		sprite = loadBc7Sprite("mardek/importer/world/Belfan.png"),
		nodes = nodes.toTypedArray(),
		edges = arrayOf(
			WorldMapEdge(goznor, heroesDen),
			WorldMapEdge(goznor, soothwood),
			WorldMapEdge(crashSite, soothwood),
		)
	))
}
