package mardek.importer.portrait

import com.jpexs.decompiler.flash.tags.DefineSpriteTag
import com.jpexs.decompiler.flash.tags.DoActionTag
import mardek.content.Content
import mardek.importer.animation.AnimationImportContext
import mardek.importer.animation.getScript
import mardek.importer.animation.importSkinnedAnimation
import mardek.importer.area.FLASH
import mardek.importer.audio.importAudioContent
import mardek.importer.particle.importParticleEffects
import mardek.importer.stats.addElements
import mardek.importer.util.projectFolder
import java.io.File

fun main() {
	val content = Content()
	importAudioContent(content.audio)
	importParticleEffects(content)
	addElements(content)
	importPortraits(content)
}

internal fun importPortraits(content: Content) {
	val rootTag = FLASH.tags.find { it is DefineSpriteTag && it.exportName == "_portrait" }!! as DefineSpriteTag
	val magicScale = 1
	val context = AnimationImportContext(
		shapesDirectory = File("$projectFolder/flash/portrait-shapes-x$magicScale/")
	)
	content.portraits.magicScale = magicScale
	content.portraits.animations = importSkinnedAnimation(rootTag, context)

	for (sprite in context.shapeMapping.values) {
		content.portraits.animationSprites.add(sprite)
	}

	for (animation in context.spriteMapping.values) {
		content.portraits.skinnedAnimations.add(animation)
	}

	// 1729 portrait is the root
	//   1241 HuM (human male) is a child of 1729
	//     in frame 1, 1241 places neck (883), torso (947), and head (1240)
	//     all other frames simply transform those three, but the difference is barely visible
	//       1240 is a child of HuM
	//         every frame is named to a facial expression (norm, susp, grin, etc...)
	//           1198 (hair) is a child of 1240
	//             its frames are named after characters or categories (e.g. bald, mardek, soldier)
	//               the children are typically shapes

	var script: String? = null
	for (childTag in rootTag.tags) {
		if (childTag is DoActionTag) {
			if (script != null) throw IllegalStateException()
			script = getScript(childTag)
		}
	}

	parsePortraitScript(content, script!!)
}
