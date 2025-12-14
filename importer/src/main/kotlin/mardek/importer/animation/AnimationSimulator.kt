package mardek.importer.animation

import com.jpexs.decompiler.flash.tags.DefineSpriteTag
import com.jpexs.decompiler.flash.tags.DoActionTag
import com.jpexs.decompiler.flash.tags.FrameLabelTag
import com.jpexs.decompiler.flash.tags.ShowFrameTag
import com.jpexs.decompiler.flash.tags.base.PlaceObjectTypeTag
import com.jpexs.decompiler.flash.tags.base.RemoveTag
import com.jpexs.decompiler.flash.tags.base.SoundStreamHeadTypeTag
import com.jpexs.decompiler.flash.types.ColorTransform

internal class RawAnimationNode(
	val tag: PlaceObjectTypeTag,
	val childID: Int,
	val instanceName: String?,
	val colors: ColorTransform?,
	val clipDepth: Int,
) {
	override fun toString() = "RawAnimationNode($childID, $instanceName, $colors, $clipDepth)"
}

internal class RawAnimationState(
	val nodes: MutableList<RawAnimationNode?>,
	val scripts: MutableList<DoActionTag>,
) {
	fun copy() = RawAnimationState(nodes.toMutableList(), scripts.toMutableList())

	fun placeObject(tag: PlaceObjectTypeTag) {
		while (nodes.size <= tag.depth) nodes.add(null)

		val existingNode = nodes[tag.depth]
		val childID = if (tag.characterId < 0) existingNode!!.childID else tag.characterId
		val instanceName = tag.instanceName ?: existingNode?.instanceName
		val colorTransform = tag.colorTransform ?: existingNode?.colors
		val clipDepth = if (tag.clipDepth < 0 && existingNode != null) existingNode.clipDepth else tag.clipDepth

		nodes[tag.depth] = RawAnimationNode(tag, childID, instanceName, colorTransform, clipDepth)
	}

	fun removeObject(tag: RemoveTag) {
		if (nodes.size <= tag.depth) throw IllegalArgumentException("Can't delete object at missing depth in $tag")
		nodes[tag.depth] = null
	}

	fun addScript(tag: DoActionTag) {
		scripts.add(tag)
	}
}

internal class RawAnimation {
	val frames = mutableListOf<RawAnimationState>()
}

private class AnimationSimulator {
	val currentState = RawAnimationState(mutableListOf(), mutableListOf())
	var currentAnimation = RawAnimation()
	var currentSkinName = ""
	val skins = mutableMapOf<String, RawAnimation>()
	var onlyOneFramePerSkin = false

	fun showFrame() {
		currentAnimation.frames.add(currentState.copy())
		if (onlyOneFramePerSkin) {
			skins[currentSkinName] = currentAnimation
			currentAnimation = RawAnimation()
			currentSkinName = (skins.size + 1).toString()
		}
		currentState.scripts.clear()
	}

	fun setLabel(name: String) {
		if (onlyOneFramePerSkin) {
			currentSkinName = name
		} else {
			skins[currentSkinName] = currentAnimation
			currentSkinName = name
			currentAnimation = RawAnimation()
		}
	}

	fun addScript(tag: DoActionTag) {
		val script = getScript(tag)
		if (
			script.trim() == "GotoAptFrame(this);" ||
			script.contains("!_parent._parent.stats.ethnicity") ||
			script.contains("this.noFlicker") ||
			script.contains("!_parent._parent.stats.face2") ||
			script.contains("&& isNaN(_parent._parent.stats.face)")
		) {
			onlyOneFramePerSkin = true
			if (currentSkinName != "") throw IllegalStateException()
			currentSkinName = "1"
		}
		if (script.startsWith("char2lips")) onlyOneFramePerSkin = true
		if (script.trim() == "gotoAndPlay(2);") currentAnimation.frames.removeFirst()
		currentState.addScript(tag)
	}

	fun finish() {
		if (!onlyOneFramePerSkin) skins[currentSkinName] = currentAnimation
	}
}

internal fun simulateAnimations(parent: DefineSpriteTag): Map<String, RawAnimation> {
	val simulator = AnimationSimulator()
	for (tag in parent.tags) {
		when (tag) {
			is ShowFrameTag -> simulator.showFrame()
			is FrameLabelTag -> simulator.setLabel(tag.name)
			is PlaceObjectTypeTag -> simulator.currentState.placeObject(tag)
			is RemoveTag -> simulator.currentState.removeObject(tag)
			is DoActionTag -> simulator.addScript(tag)
			is SoundStreamHeadTypeTag -> {}
			else -> println("WARNING: unexpected tag $tag in $parent")
		}
	}
	simulator.finish()
	return simulator.skins
}
