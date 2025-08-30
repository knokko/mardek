package mardek.importer.animation

import com.jpexs.decompiler.flash.tags.DoActionTag
import mardek.content.animation.AnimationSprite
import mardek.content.animation.SkinnedAnimation
import java.io.File

class AnimationImportContext(
	val shapesDirectory: File,
) {
	val shapeMapping = mutableMapOf<Pair<Int, Boolean>, AnimationSprite>()
	val spriteMapping = mutableMapOf<Int, SkinnedAnimation>()
	val scriptMapping = mutableMapOf<Int, Map<String, List<DoActionTag>>>()
}
