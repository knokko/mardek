package mardek.importer.actions

import com.github.knokko.bitser.SimpleLazyBits
import com.jpexs.decompiler.flash.tags.DefineSpriteTag
import com.jpexs.decompiler.flash.tags.DoActionTag
import com.jpexs.decompiler.flash.tags.ShowFrameTag
import mardek.content.Content
import mardek.content.action.ActionContent
import mardek.content.action.Cutscene
import mardek.content.action.CutscenePayload
import mardek.importer.animation.AnimationImportContext
import mardek.importer.animation.getScript
import mardek.importer.animation.importSkinnedAnimation
import mardek.importer.area.FLASH
import mardek.importer.util.projectFolder
import java.io.File
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

internal fun importCutscenes(content: Content) {
	importIntroCutscene(content.actions)
	importFallingStarCutscene(content)
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
	val chapter1IntroTexts = mutableListOf<CutscenePayload.TextEntry>()
	for (tag in chapter1IntroTag.tags) {
		if (tag is ShowFrameTag) frameCounter += 1
		if (tag is DoActionTag) {
			val script = getScript(tag)
			if (script.startsWith("storytext = ")) {
				val storyText = script.substring("storytext = \"".length, script.lastIndexOf('"'))
					.replace("\\'", "'")
				// Note that we skip the first 30 frames because we only take "cut1a"
				// The first 30 frames are just translucent black
				chapter1IntroTexts.add(CutscenePayload.TextEntry(frameCounter - 29, storyText, 1))
				if (storyText.contains("greatest  Kingdoms!")) {
					chapter1IntroTexts.add(CutscenePayload.TextEntry(frameCounter + 120, "(Well, what kingdom?)", 0))
					chapter1IntroTexts.add(CutscenePayload.TextEntry(frameCounter + 180, "(It doesn't matter!)", 2))
				}
			}
		}
	}

	val cutscene = Cutscene(
		id = UUID.fromString("9fbfa37e-b304-4e94-aee8-037ca3b6b4ad"),
		name = "Chapter 1 intro",
		payload = SimpleLazyBits(CutscenePayload(
			frames = chapter1IntroUseful,
			magicScale = magicScale,
			musicTrack = "Intro",
			subtitles = chapter1IntroTexts.toTypedArray(),
		)),
		sounds = emptyArray(),
	)
	for (sprite in context.shapeMapping.values) {
		cutscene.payload.get().sprites.add(sprite)
	}

	for (animation in context.spriteMapping.values) {
		cutscene.payload.get().innerAnimations.add(animation)
	}

	content.cutscenes.add(cutscene)
}

private fun importFallingStarCutscene(content: Content) {
	val magicScale = 2

	val context = AnimationImportContext(
		shapesDirectory = File("$projectFolder/flash/all-shapes-x$magicScale/")
	)

	val fallingStarTag = FLASH.tags.find { it is DefineSpriteTag && it.exportName == "CUTSCENE2" }!! as DefineSpriteTag
	val fallingStarFull = importSkinnedAnimation(fallingStarTag, context)
	val fallingStarUseful = fallingStarFull.skins["cut1b"]!!

	val cutscene = Cutscene(
		id = UUID.fromString("79f85f55-5321-415d-a12e-1d58f32f3191"),
		name = "Falling Star",
		payload = SimpleLazyBits(CutscenePayload(
			frames = fallingStarUseful,
			magicScale = magicScale,
			musicTrack = null,
			subtitles = emptyArray(),
		)),
		sounds = arrayOf(Cutscene.SoundEntry(
			delay = 11.seconds, sound = content.audio.effects.find { it.flashName == "boom3" }!!
		))
	)

	for (sprite in context.shapeMapping.values) {
		cutscene.payload.get().sprites.add(sprite)
	}

	for (animation in context.spriteMapping.values) {
		cutscene.payload.get().innerAnimations.add(animation)
	}

	content.actions.cutscenes.add(cutscene)
}

internal fun addDummyCutscenes(content: ActionContent) {
	content.cutscenes.add(Cutscene(
		id = UUID.fromString("9fbfa37e-b304-4e94-aee8-037ca3b6b4ad"),
		name = "Chapter 1 intro",
		payload = SimpleLazyBits(CutscenePayload()),
		sounds = emptyArray(),
	))
	content.cutscenes.add(Cutscene(
		id = UUID.fromString("79f85f55-5321-415d-a12e-1d58f32f3191"),
		name = "Falling Star",
		payload = SimpleLazyBits(CutscenePayload()),
		sounds = emptyArray(),
	))
}
