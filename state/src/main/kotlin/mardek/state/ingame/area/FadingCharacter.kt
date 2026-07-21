package mardek.state.ingame.area

import mardek.content.area.objects.AreaCharacter
import mardek.content.util.Time

/**
 * Represents a character (typically a boss) that was slain very recently. A fading red variant of its sprite should be
 * rendered a few seconds after it died.
 */
class FadingCharacter(
	/**
	 * The character that is fading
	 */
	val character: AreaCharacter,

	/**
	 * The state of the character at the time it started fading
	 */
	val lastState: AreaCharacterState,

	/**
	 * The area time at which the character *started* fading
	 */
	val startFadeTime: Time,
)
