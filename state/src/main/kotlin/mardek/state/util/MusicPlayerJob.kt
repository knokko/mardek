package mardek.state.util

import mardek.content.audio.MusicTrack
import kotlin.time.Duration

/**
 * This is the return type of [mardek.state.GameState.determineMusic], and is used by the *state* module to tell the
 * *audio* module which music track should be played.
 *
 * Furthermore, it is used by the Music Player to pause music tracks, and to query information about music tracks.
 */
class MusicPlayerJob(

	/**
	 * The music track to be played, or `null` to play no music
	 */
	val track: MusicTrack?,

	/**
	 * Whether the audio player should be paused, only used by the Music Player
	 */
	val shouldPause: Boolean = false,

	/**
	 * This callback will be invoked by the audio player when `track != null`: it will tell how long [track] is,
	 * and how much of [track] has already been played.
	 */
	val infoCallback: (timePlayed: Duration, trackLength: Duration) -> Unit = { _, _ -> },
)
