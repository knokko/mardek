package mardek.importer.battle

import com.jpexs.decompiler.flash.tags.DefineSpriteTag
import mardek.content.animation.AnimationNode
import mardek.content.battle.BattleContent
import mardek.content.battle.BattleBackground
import mardek.importer.animation.AnimationImportContext
import mardek.importer.animation.importSkinnedAnimation
import mardek.importer.area.FLASH
import mardek.importer.util.projectFolder
import java.io.File

internal fun importBattleBackgrounds(content: BattleContent) {
	val backgroundsTag1 = FLASH.tags.find { it.uniqueId == "2186" }!! as DefineSpriteTag
	val backgroundsTag2 = FLASH.tags.find { it.uniqueId == "2204" }!! as DefineSpriteTag

	val context = AnimationImportContext(
		shapesDirectory = File("$projectFolder/flash/background-shapes-x4/")
	)
	val backgrounds1 = importSkinnedAnimation(backgroundsTag1, context)
	val backgrounds2 = importSkinnedAnimation(backgroundsTag2, context)

	val combined = mutableMapOf<String, Array<AnimationNode>>()
	for ((name, frames) in backgrounds1.skins) {
		if (name.isEmpty()) continue
		val firstFrame = frames.frames[0]
		combined[name] = firstFrame.nodes
	}

	for ((name, frames) in backgrounds2.skins) {
		if (name.isEmpty()) continue

		val firstFrame = frames.frames[0]
		val existing = combined[name]
		if (existing == null) {
			combined[name] = firstFrame.nodes
		} else {
			combined[name] = (existing + firstFrame.nodes).sortedBy { it.depth }.toTypedArray()
		}
	}

	for ((name, nodes) in combined) {
		content.backgrounds.add(BattleBackground(name, nodes, 4))
	}

	for (sprite in context.shapeMapping.values) {
		content.animationSprites.add(sprite)
	}

	for (animation in context.spriteMapping.values) {
		content.skinnedAnimations.add(animation)
	}
}
