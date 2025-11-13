package mardek.content.audio

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceFieldTarget

/**
 * Contains the sound effects needed by hardcoded actions like the UI
 */
@BitStruct(backwardCompatible = true)
class FixedSoundEffects(

	/**
	 * The sound effects needed for the UI
	 */
	@BitField(id = 0)
	val ui: UiSoundEffects,

	/**
	 * The sound effects for core battle mechanics
	 */
	@BitField(id = 1)
	val battle: BattleSoundEffects,

	/**
	 * The sound effect that is played when the player opens a chest
	 */
	@BitField(id = 2)
	val openChest: SoundEffect,

	/**
	 * The sound effect that is played when the player interacts with a save crystal.
	 */
	@BitField(id = 3)
	@ReferenceFieldTarget(label = "sound effects")
	val saveCrystal: SoundEffect,
) {
	constructor() : this(UiSoundEffects(), BattleSoundEffects(), SoundEffect(), SoundEffect())
}

/**
 * Contains the sound effects that are needed for the UI.
 */
@BitStruct(backwardCompatible = true)
class UiSoundEffects(

	/**
	 * The sound played when the player successfully clicks or interacts
	 */
	@BitField(id = 0)
	val clickConfirm: SoundEffect,

	/**
	 * The sound played when the player cancels something in the UI, or goes back
	 */
	@BitField(id = 1)
	val clickCancel: SoundEffect,

	/**
	 * The sound played when the player attempts to do something that is not possible/allowed.
	 */
	@BitField(id = 2)
	val clickReject: SoundEffect,

	/**
	 * The primary sound effect that is used when the player scrolls through some list in the UI.
	 */
	@BitField(id = 3)
	val scroll1: SoundEffect,

	/**
	 * The secondary sound effect that is used when the player scrolls through some list in the UI. This one is
	 * typically used when the player can scroll both vertically and horizontally. In such cases, `scroll1` is
	 * typically used for the 'primary' direction, whereas `scroll2` is used for the other/secondary direction.
	 */
	@BitField(id = 4)
	val scroll2: SoundEffect,

	/**
	 * The sound effect that is played when the player toggles a reaction/passive skill.
	 */
	@BitField(id = 5)
	val toggleSkill: SoundEffect,

	/**
	 * The sound effect that is played when the player opens the in-game menu.
	 */
	@BitField(id = 6)
	@ReferenceFieldTarget(label = "sound effects")
	val openMenu: SoundEffect,
) {
	internal constructor() : this(
		SoundEffect(), SoundEffect(), SoundEffect(), SoundEffect(), SoundEffect(),
		SoundEffect(), SoundEffect()
	)
}

/**
 * The core sound effects in combat
 */
@BitStruct(backwardCompatible = true)
class BattleSoundEffects(

	/**
	 * The sound effect that is played when the player runs away
	 */
	@BitField(id = 0)
	val flee: SoundEffect,

	/**
	 * The fallback melee attack sound effect
	 */
	@BitField(id = 1)
	@ReferenceFieldTarget(label = "sound effects")
	val punch: SoundEffect,

	/**
	 * The sound effect that is played when someone misses an attack
	 */
	@BitField(id = 2)
	val miss: SoundEffect,

	/**
	 * The sound effect that is played when someone scores a critical hit. Some skills (e.g. Smite Evil) always use this
	 * sound effect, even when they don't crit.
	 */
	@BitField(id = 3)
	@ReferenceFieldTarget(label = "sound effects")
	val critical: SoundEffect,

	/**
	 * The sound effect that is played when a random battle has been encountered: when the blue or red exclamation mark
	 * above the player head pops up.
	 */
	@BitField(id = 4)
	val encounter: SoundEffect,

	/**
	 * The sound effect when a (random) battle begins
	 */
	@BitField(id = 5)
	val engage: SoundEffect,
) {
	internal constructor() : this(
		SoundEffect(), SoundEffect(), SoundEffect(), SoundEffect(),
		SoundEffect(), SoundEffect()
	)
}
