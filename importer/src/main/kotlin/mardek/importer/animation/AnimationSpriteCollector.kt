package mardek.importer.animation

import mardek.content.BITSER
import mardek.content.Content
import mardek.content.battle.BattleContent
import mardek.content.sprite.BcSprite
import java.util.Collections
import java.util.IdentityHashMap

fun collectAnimationSpriteReferences(content: Content) {
	val animationBcSprites = Collections.newSetFromMap(IdentityHashMap<BcSprite, Boolean>())
	val referenceMap = HashMap<Class<*>, Collection<Any>>()
	referenceMap[BcSprite::class.java] = animationBcSprites
	BITSER.collectInstances(content, emptyMap(), referenceMap)

	content.battle.sharedAnimationSprites.addAll(animationBcSprites)
	println("Got ${content.battle.sharedAnimationSprites.size} animation sprites")
}
