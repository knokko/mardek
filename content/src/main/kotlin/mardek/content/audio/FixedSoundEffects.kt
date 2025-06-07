package mardek.content.audio

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceFieldTarget

@BitStruct(backwardCompatible = true)
class FixedSoundEffects(
	@BitField(id = 0)
	val ui: UiSoundEffects,

	@BitField(id = 1)
	val battle: BattleSoundEffects,

	@BitField(id = 2)
	val openChest: SoundEffect,
) {
	constructor() : this(UiSoundEffects(), BattleSoundEffects(), SoundEffect())
}

@BitStruct(backwardCompatible = true)
class UiSoundEffects(
	@BitField(id = 0)
	val clickConfirm: SoundEffect,

	@BitField(id = 1)
	val clickCancel: SoundEffect,

	@BitField(id = 2)
	val clickReject: SoundEffect,

	@BitField(id = 3)
	val scroll: SoundEffect,

	@BitField(id = 4)
	val partyScroll: SoundEffect,

	@BitField(id = 5)
	val toggleSkill: SoundEffect,

	@BitField(id = 6)
	@ReferenceFieldTarget(label = "sound effects")
	val openMenu: SoundEffect,
) {
	internal constructor() : this(
		SoundEffect(), SoundEffect(), SoundEffect(), SoundEffect(), SoundEffect(),
		SoundEffect(), SoundEffect()
	)
}

@BitStruct(backwardCompatible = true)
class BattleSoundEffects(
	@BitField(id = 0)
	val flee: SoundEffect,

	@BitField(id = 1)
	@ReferenceFieldTarget(label = "sound effects")
	val punch: SoundEffect,

	@BitField(id = 2)
	val miss: SoundEffect,

	@BitField(id = 3)
	@ReferenceFieldTarget(label = "sound effects")
	val critical: SoundEffect,

	@BitField(id = 4)
	val encounter: SoundEffect,

	@BitField(id = 5)
	val engage: SoundEffect,
) {
	internal constructor() : this(
		SoundEffect(), SoundEffect(), SoundEffect(), SoundEffect(),
		SoundEffect(), SoundEffect()
	)
}
