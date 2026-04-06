package mardek.state

import mardek.content.Content
import mardek.input.InputManager
import mardek.state.saves.SavesFolderManager
import mardek.state.util.Rectangle
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration

/**
 * This class is a rather simple wrapper around a [GameState] field that can be re-assigned.
 * This is the type of the `MardekWindow.gameState` field.
 *
 * This class also contains an [InputManager] and [SoundQueue], which are used to exchange data between the window
 * event listener, audio player, and game state.
 *
 * Furthermore, it contains some fields (e.g. [crossLocation]) that are used to exchange data between the window
 * event listener and the renderer.
 */
class GameStateManager(
	private val input: InputManager,

	/**
	 * The current state of the game. It can be updated by either:
	 * - Re-assigning this field, which is typically used for major transitions (e.g. Title Screen -> In-game).
	 * - Or by changing the value behind the field, which is typically used for minor transitions
	 * (e.g. the player walks in an area)
	 *
	 * Note that [lock] should be held while this state is being read or updated.
	 */
	var currentState: GameState,
	private val saves: SavesFolderManager,
) {

	/**
	 * The location where the top-right cross icon was rendered. Clicking this will close the window.
	 *
	 * This field will be written by the renderer, and read by the window event listener.
	 */
	@Volatile
	var crossLocation: Rectangle? = null

	/**
	 * The location where the top-right maximize/minimize icon was rendered. Clicking this will maximize the window if
	 * it was *not* maximized, or minimize the window if it was maximized.
	 *
	 * This field will be written by the renderer, and read by the window event listener.
	 */
	@Volatile
	var maximizeLocation: Rectangle? = null

	/**
	 * The location where the top-right minus icon was rendered. Clicking this will hide the window, after which the
	 * player can make it re-appear using the OS taskbar.
	 *
	 * This field will be written by the renderer, and read by the window event listener.
	 */
	@Volatile
	var minusLocation: Rectangle? = null

	/**
	 * Whether the mouse cursor is hovering over [crossLocation]. When `true`, the renderer will highlight the icon.
	 *
	 * This field will be written by the window event listener, and read by the renderer.
	 */
	@Volatile
	var hoveringCross = false

	/**
	 * Whether the mouse cursor is hovering over [maximizeLocation]. When `true`, the renderer will highlight the icon.
	 *
	 * This field will be written by the window event listener, and read by the renderer.
	 */
	@Volatile
	var hoveringMaximize = false

	/**
	 * Whether the mouse cursor is hovering over [minusLocation]. When `true`, the renderer will highlight the icon.
	 *
	 * This field will be written by the window event listener, and read by the renderer.
	 */
	@Volatile
	var hoveringMinus = false

	/**
	 * This field facilitates the communication from the game state to the audio player.
	 *
	 * The game state will insert sounds into this queue, and the audio player will play the sounds that are
	 * inserted into this queue.
	 */
	val soundQueue = SoundQueue()

	/**
	 * The lock that should be held while [currentState] is being read or updated
	 */
	fun lock(): Any = this

	/**
	 * Updates/advances [currentState] by [timeStep].
	 *
	 * This method should be called periodically by the `MardekWindow`.
	 *
	 * Note that [lock] should be held while calling this method.
	 */
	fun update(content: CompletableFuture<Content>, timeStep: Duration) {
		if (content.isDone) {
			val context = GameStateUpdateContext(content.get(), input, soundQueue, timeStep, saves)
			this.currentState = this.currentState.update(context)
		} else {
			this.currentState = this.currentState.updateBeforeContent(input, soundQueue, saves)
		}
	}
}
