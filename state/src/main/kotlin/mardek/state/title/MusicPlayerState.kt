package mardek.state.title

import mardek.content.Content
import mardek.content.audio.AudioContent
import mardek.content.audio.MusicTrack
import mardek.content.ui.TitleScreenContent
import mardek.input.InputKey
import mardek.input.InputKeyEvent
import mardek.input.InputManager
import mardek.input.MouseMoveEvent
import mardek.state.GameState
import mardek.state.GameStateUpdateContext
import mardek.state.SoundQueue
import mardek.state.saves.SavesFolderManager
import mardek.state.util.MusicPlayerJob
import mardek.state.util.Rectangle
import kotlin.time.Duration

/**
 * This class tracks the interaction of the player with the Music Player.
 * It tracks e.g. which music category is being browsed, which music track is selected, etc...
 *
 * However, it does *not* track which music tracks have been discovered yet. Use [SavesFolderManager.unlockMusicTrack]
 * and [SavesFolderManager.isMusicTrackUnlocked] for that.
 */
class MusicPlayerState(): GameState {

	/**
	 * The time instant where the player opened the music player. When this is very recent, we render a fade-in.
	 */
	val openedAt = System.nanoTime()

	/**
	 * The time instant where the player left the music player, or 0 when the player hasn't left yet.
	 * Once this field becomes non-zero, the fade-out will start, and the player will soon return to the title screen.
	 */
	var closedAt = 0L
		private set

	/**
	 * The selected music category index into [mardek.content.audio.AudioContent.musicCategories],
	 * or `-1` to display all music tracks.
	 */
	var selectedCategoryIndex = -1
		private set

	/**
	 * The index of the selected music track, *within the selected music category*.
	 *
	 * For instance, when the selected category is Battle Music and `selectedMusicIndex == 0`,
	 * the first *battle music* track is selected.
	 */
	var selectedMusicIndex = 0
		private set

	/**
	 * The index of the first shown music track, *within the selected music category*.
	 *
	 * When `firstMusicIndexOnScreen == x`, music tracks `x` to `x + 8` (inclusive) will be shown on the screen,
	 * since only 9 music tracks will fit on the screen.
	 *
	 * For instance, when the selected category is Dungeon Music and `firstMusicIndexOnScreen == 2`:
	 * - The first and second battle music track will be 'shown' *above* the screen, making them invisible
	 * - The third battle music track will be shown at the top of the screen
	 * - The fourth battle music track will be shown below the third
	 * - ...
	 * - The eleventh battle music track will be shown at the bottom of the screen
	 * - The twelfth battle music track will be 'shown' *below* the screen, making it invisible.
	 * The same holds for the thirteenth track, and all later tracks.
	 */
	var firstMusicIndexOnScreen = 0
		private set

	/**
	 * The index of the last/lowest visible music track. This is simply [firstMusicIndexOnScreen] + 8
	 */
	val lastMusicIndexOnScreen: Int
		get() = firstMusicIndexOnScreen + 8

	/**
	 * The music track that is currently being played (or paused).
	 *
	 * This will be `null` until the player plays the first music track.
	 */
	var playingTrack: MusicTrack? = null
		private set

	/**
	 * Whether [playingTrack] is currently paused. This field is meaningless when `playerTrack == null`.
	 */
	var isPaused = false
		private set

	/**
	 * The region where the pause/play button was rendered, or `null` before the first frame
	 */
	var pauseButton: Rectangle? = null

	/**
	 * The region where the back button was rendered, or `null` before the first frame
	 */
	var backButton: Rectangle? = null

	/**
	 * The X-coordinate of the last-known mouse cursor location
	 */
	var mouseX = -1
		private set

	/**
	 * The Y-coordinate of the last-known mouse cursor location
	 */
	var mouseY = -1
		private set

	/**
	 * The music play time since the start of the music track.
	 *
	 * This value will increase while the song is playing, but not while [isPaused].
	 * It will be reset to 0 (or the looping time) when the end [playingTrack] is reached,
	 * and the music player restarts from the beginning (or the looping time).
	 */
	var timePlaying = Duration.ZERO
		private set

	/**
	 * The duration of [playingTrack]
	 */
	var playingTrackDuration = Duration.ZERO
		private set

	/**
	 * Gets all the music tracks in `titleContent.audio.musicTracks` that belong to the currently-selected category.
	 *
	 * When no category is selected (`selectedCategoryIndex == -1`), this method will return
	 * `titleContent.audio.musicTracks`.
	 */
	fun getMusicTracksInSelectedCategory(titleContent: TitleScreenContent): List<MusicTrack> {
		return if (selectedCategoryIndex >= 0) {
			val category = titleContent.audio.musicCategories[selectedCategoryIndex]
			titleContent.audio.musicTracks.filter { it.category === category }
		} else titleContent.audio.musicTracks
	}

	override fun updateBeforeContent(
		input: InputManager,
		soundQueue: SoundQueue,
		saves: SavesFolderManager,
		titleContent: TitleScreenContent,
	): GameState {
		if (closedAt != 0L) return if (System.nanoTime() - closedAt > FADE_OUT_TIME) TitleScreenState() else this

		while (true) {
			val event = input.consumeEvent() ?: break
			if (event is InputKeyEvent && event.didPress) {
				if (event.key == InputKey.Cancel || event.key == InputKey.Escape) {
					closedAt = System.nanoTime()
					return this
				}

				if (event.key == InputKey.MoveLeft || event.key == InputKey.MoveRight) {
					if (event.key == InputKey.MoveLeft) {
						selectedCategoryIndex = if (selectedCategoryIndex >= 0) selectedCategoryIndex - 1
						else titleContent.audio.musicCategories.size - 1
					}

					if (event.key == InputKey.MoveRight) {
						selectedCategoryIndex += 1
						if (selectedCategoryIndex >= titleContent.audio.musicCategories.size) selectedCategoryIndex = -1
					}

					selectedMusicIndex = 0
					firstMusicIndexOnScreen = 0
				}

				if (event.key == InputKey.MoveUp || event.key == InputKey.MoveDown) {
					val tracks = getMusicTracksInSelectedCategory(titleContent)

					if (event.key == InputKey.MoveUp && selectedMusicIndex > 0) selectedMusicIndex -= 1
					if (event.key == InputKey.MoveDown && selectedMusicIndex + 1 < tracks.size) selectedMusicIndex += 1

					if (selectedMusicIndex < firstMusicIndexOnScreen) firstMusicIndexOnScreen = selectedMusicIndex
					if (selectedMusicIndex > lastMusicIndexOnScreen) {
						firstMusicIndexOnScreen = selectedMusicIndex - 8
					}
				}

				if (event.key == InputKey.Interact || event.key == InputKey.ToggleMenu) {
					val tracks = getMusicTracksInSelectedCategory(titleContent)
					val nextTrack = tracks[selectedMusicIndex]

					if (playingTrack !== nextTrack) {
						if (saves.isMusicTrackUnlocked(nextTrack)) {
							playingTrack = nextTrack
							isPaused = false
						}
					} else isPaused = !isPaused
				}

				if (event.key == InputKey.Click) {
					val backButtonRegion = this.backButton
					if (backButtonRegion != null && backButtonRegion.contains(mouseX, mouseY)) {
						closedAt = System.nanoTime()
						return this
					}

					val pauseButtonRegion = this.pauseButton
					if (pauseButtonRegion != null && pauseButtonRegion.contains(mouseX, mouseY)) {
						if (playingTrack != null) isPaused = !isPaused
					}
				}
			}

			if (event is MouseMoveEvent) {
				mouseX = event.newX
				mouseY = event.newY
			}
		}

		return this
	}

	override fun update(context: GameStateUpdateContext) = updateBeforeContent(
		context.input, context.soundQueue, context.saves, context.titleContent
	)

	override fun determineMusic(content: Content?, audioContent: AudioContent): MusicPlayerJob {
		val currentTrack = playingTrack ?: return MusicPlayerJob(null)
		return MusicPlayerJob(currentTrack, isPaused) { secondsPlayed, totalSeconds ->
			this.timePlaying = secondsPlayed
			this.playingTrackDuration = totalSeconds
		}
	}

	companion object {

		/**
		 * The duration of the fade-out after closing the music player, in nanoseconds
		 */
		const val FADE_OUT_TIME = 400_000_000L

		/**
		 * The duration of te fade-in after opening the music player, in nanoseconds
		 */
		const val FADE_IN_TIME = FADE_OUT_TIME
	}
}
