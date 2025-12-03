package mardek.importer.actions

import com.github.knokko.bitser.SimpleLazyBits
import com.jpexs.decompiler.flash.tags.DefineSpriteTag
import com.jpexs.decompiler.flash.tags.DoActionTag
import com.jpexs.decompiler.flash.tags.ShowFrameTag
import mardek.content.action.ActionContent
import mardek.content.action.Cutscene
import mardek.content.animation.AnimationFrames
import mardek.importer.animation.AnimationImportContext
import mardek.importer.animation.getScript
import mardek.importer.animation.importSkinnedAnimation
import mardek.importer.area.FLASH
import mardek.importer.util.projectFolder
import java.io.File
import java.util.UUID

internal fun importCutscenes(content: ActionContent) {
	importIntroCutscene(content)
}

private fun importIntroCutscene(content: ActionContent) {
	val magicScale = 4

	val context = AnimationImportContext(
		shapesDirectory = File("$projectFolder/flash/all-shapes-x$magicScale/")
	)

	val chapter1IntroTag = FLASH.tags.find { it is DefineSpriteTag && it.exportName == "CUTSCENE1" }!! as DefineSpriteTag
	val chapter1IntroFull = importSkinnedAnimation(chapter1IntroTag, context)
	val chapter1IntroUseful = chapter1IntroFull.skins["cut1a"]!!

	var frameCounter = 1
	val chapter1IntroTexts = mutableListOf<Cutscene.TextEntry>()
	for (tag in chapter1IntroTag.tags) {
		if (tag is ShowFrameTag) frameCounter += 1
		if (tag is DoActionTag) {
			val script = getScript(tag)
			if (script.startsWith("storytext = ")) {
				val storyText = script.substring("storytext = \"".length, script.lastIndexOf('"'))
					.replace("\\'", "'")
				// Note that we skip the first 30 frames because we only take "cut1a"
				// The first 30 frames are just translucent black
				chapter1IntroTexts.add(Cutscene.TextEntry(frameCounter - 29, storyText))
			}
		}
	}

	val cutscene = Cutscene(
		name = "Chapter 1 intro",
		frames = chapter1IntroUseful,
		magicScale = magicScale,
		musicTrack = "Intro",
		subtitles = chapter1IntroTexts.toTypedArray(),
		id = UUID.fromString("9fbfa37e-b304-4e94-aee8-037ca3b6b4ad"),
	)
	for (sprite in context.shapeMapping.values) {
		cutscene.sprites.add(sprite)
	}

	for (animation in context.spriteMapping.values) {
		cutscene.innerAnimations.add(animation)
	}

	content.cutscenes.add(SimpleLazyBits(cutscene))
}

internal fun addDummyCutscenes(content: ActionContent) {
	content.cutscenes.add(SimpleLazyBits(Cutscene(
		name = "Chapter 1 intro",
		frames = AnimationFrames(),
		magicScale = 0,
		musicTrack = "",
		subtitles = emptyArray(),
		id = UUID.fromString("9fbfa37e-b304-4e94-aee8-037ca3b6b4ad"),
	)))
}
