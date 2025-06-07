package mardek.content.animations

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ClassField

@BitStruct(backwardCompatible = true)
class SkeletonPart(
	@BitField(id = 0)
	@ClassField(root = SkeletonPartContent::class)
	val content: SkeletonPartContent
) {
	constructor() : this(SkeletonPartCastSparkle())
}

sealed class SkeletonPartContent {
	companion object {

		@Suppress("unused")
		val BITSER_HIERARCHY = arrayOf(
			SkeletonPartSkins::class.java,
			SkeletonPartCastSparkle::class.java,
			SkeletonPartSwingEffect::class.java,
		)
	}
}

@BitStruct(backwardCompatible = true)
class SkeletonPartSkins(
	@BitField(id = 0)
	val skins: Array<BodyPart>
) : SkeletonPartContent() {

	@Suppress("unused")
	private constructor() : this(emptyArray())
}

@BitStruct(backwardCompatible = true)
class SkeletonPartCastSparkle : SkeletonPartContent()

@BitStruct(backwardCompatible = true)
class SkeletonPartSwingEffect : SkeletonPartContent()
