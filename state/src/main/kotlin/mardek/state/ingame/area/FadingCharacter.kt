package mardek.state.ingame.area

import mardek.content.area.objects.AreaCharacter

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
) {

	/**
	 * The time (from `System.nanoTime()`) at which the character *started* fading
	 */
	val startFadeTime = System.nanoTime()
}
