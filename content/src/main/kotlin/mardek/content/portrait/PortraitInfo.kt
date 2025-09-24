package mardek.content.portrait

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.stats.Element

/**
 * A portrait info entry extracted from the DoAction script of frame 1729 (_portrait)
 */
@BitStruct(backwardCompatible = true)
class PortraitInfo(
	/**
	 * The key/name of the portrait, without the leading "s_"
	 */
	@BitField(id = 0)
	val flashName: String,

	/**
	 * The skin used for Sprite 1729 (_portrait), for instance "huF" for Human Females
	 */
	@BitField(id = 1)
	val rootSkin: String,

	/**
	 * The skin used for Sprite 989 (face), for instance "arab"
	 */
	@BitField(id = 2)
	val faceSkin: String,

	/**
	 * The skin used for Sprite 1198 (hair), for instance "sharla"
	 */
	@BitField(id = 3)
	val hairSkin: String,

	/**
	 * The skin used for Sprite 1084 (eye), for instance "blue"
	 */
	@BitField(id = 4)
	val eyeSkin: String,

	/**
	 * The skin used for Sprite 1118 (eyebrow), for instance "brown"
	 */
	@BitField(id = 5)
	val eyeBrowSkin: String,

	/**
	 * The skin used for Sprite 1059 (mouth), for instance "zombie"
	 */
	@BitField(id = 6)
	val mouthSkin: String,

	/**
	 * The skin used for Sprite 947 (torso), for instance "Pyromancer"
	 */
	@BitField(id = 7)
	val armorSkin: String,

	/**
	 * The skin used for Sprite 1569 (torso), for instance "purple"
	 */
	@BitField(id = 8, optional = true)
	val robeSkin: String?,

	/**
	 * Probably the skin for e.g. the face mask of Guardian Emela
	 */
	@BitField(id = 9, optional = true)
	val faceMask: String?,

	/**
	 * The skin used for Sprite 997, for instance "pallid"
	 */
	@BitField(id = 10)
	val ethnicitySkin: String,

	/**
	 * Some characters have fancy colored text in dialogues (e.g. all guardians and GdM)
	 */
	@BitField(id = 11, optional = true)
	val voiceStyle: String?,

	/**
	 * Not sure what this is: some elemental background?
	 * Seems to be related to `voiceStyle`?
	 */
	@BitField(id = 12, optional = true)
	@ReferenceField(stable = false, label = "elements")
	val elementalBackground: Element?,
) {

	constructor() : this(
		"", "", "", "", "", "", "",
		"", null, null, "", null, null
	)

	override fun toString() = "Portrait($flashName)"
}
