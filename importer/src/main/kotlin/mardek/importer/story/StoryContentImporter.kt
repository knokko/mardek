package mardek.importer.story

import mardek.content.story.StoryContent

internal fun importSimpleStoryContent(content: StoryContent) {
	importQuests(content)
	hardcodeCustomVariables(content)
	hardcodeGlobalExpressions(content)
}
