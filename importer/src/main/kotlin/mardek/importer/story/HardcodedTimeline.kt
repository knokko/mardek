package mardek.importer.story

import mardek.content.Content
import mardek.content.story.Timeline
import mardek.content.story.TimelineNode
import java.util.UUID

internal fun hardcodeTimeline(content: Content) {
	val chapter1Node = hardcodeChapter1Timeline(content)
	val mainRootNode = TimelineNode(
		id = UUID.fromString("3e4f759f-80a6-457e-a955-00d1dbb4fd4a"),
		name = "MainRootNode",
		children = arrayOf(chapter1Node),
		variables = emptyArray(),
		isAbstract = true,
	)
	content.story.timelines.add(Timeline(
		id = UUID.fromString("61f6297c-eb96-4f9c-8dae-439c7385ba6d"),
		name = "MainTimeline",
		root = mainRootNode,
		needsActivation = false,
	))
}
