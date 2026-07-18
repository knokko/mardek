package mardek.importer.story

import mardek.content.Content

internal fun importSimpleStoryContent(content: Content) {
	importQuests(content.story)
	hardcodeCustomVariables(content.story)
	hardcodeGlobalExpressions(content)
}
