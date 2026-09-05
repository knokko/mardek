package mardek.content.audio

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.ReferenceFieldTarget
import java.io.Serializable

/**
 * The audio-related part of the `Content`
 */
@BitStruct(backwardCompatible = true)
class AudioContent : Serializable {

	/**
	 * This is the list of sound effects that can be used by e.g. skills, but are not needed by the hardcoded actions
	 * like UI.
	 */
	@BitField(id = 0)
	@ReferenceFieldTarget(label = "sound effects")
	val effects = ArrayList<SoundEffect>()

	/**
	 * This contains the sound effects that are needed by hardcoded actions, for instance the click sounds and the
	 * chest sounds.
	 *
	 * This should be `null` for the `TitleScreenContent`, and non-null for the real `Content`.
	 */
	@BitField(id = 1, optional = true)
	lateinit var fixedEffects: FixedSoundEffects

	/**
	 * All the (background) music tracks that can be played throughout the game (or the title screen).
	 * This contains the intro music, as well as all area music, character music, and battle music.
	 */
	@BitField(id = 2)
	@ReferenceFieldTarget(label = "music tracks")
	val musicTracks = ArrayList<MusicTrack>()

	/**
	 * All the [MusicCategory]s that the [musicTracks] can have.
	 *
	 * Currently, these are only relevant for the Music Player.
	 */
	@BitField(id = 3)
	@ReferenceFieldTarget(label = "music categories")
	val musicCategories = ArrayList<MusicCategory>()

	/**
	 * The music track that should be played on the title screen, or `null` to play no music
	 */
	@BitField(id = 4, optional = true)
	@ReferenceField(stable = false, label = "music tracks")
	var titleScreenTrack: MusicTrack? = null

	/**
	 * The music track that should be played on the game over screen, or `null` to play no music
	 */
	@BitField(id = 5, optional = true)
	@ReferenceField(stable = false, label = "music tracks")
	var gameOverTrack: MusicTrack? = null

	/**
	 * The music track that should be played during battles, except during battles with a custom music track
	 */
	@BitField(id = 6, optional = true)
	@ReferenceField(stable = false, label = "music tracks")
	var defaultBattleTrack: MusicTrack? = null

	/**
	 * The music track that should be played after winning a battle,
	 * except when that battle has a custom victory music track
	 */
	@BitField(id = 7, optional = true)
	@ReferenceField(stable = false, label = "music tracks")
	var defaultVictoryTrack: MusicTrack? = null
}
