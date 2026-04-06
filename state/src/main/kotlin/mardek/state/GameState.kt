package mardek.state

import mardek.content.Content
import mardek.input.InputManager
import mardek.state.ingame.CampaignState
import mardek.state.saves.SavesFolderManager
import kotlin.time.Duration

/**
 * This interface is implemented by all the 'root' game states (e.g. [mardek.state.ingame.InGameState] and
 * [mardek.state.title.TitleScreenState]).
 *
 * This interface is the type of [GameStateManager.currentState], which represents the entire game state.
 */
interface GameState {

	/**
	 * This method should be called instead of [update] when the game hasn't deserialized the [Content] yet.
	 * This typically happens during the first ~1 second of the game.
	 *
	 * The player cannot start the game before the content is deserialized, but can already navigate the
	 * title screen. Ideally, the game finishes deserializing the content before the player manages to click on
	 * "Load Game" or "Begin". (Otherwise, the player has to wait...)
	 */
	fun updateBeforeContent(input: InputManager, soundQueue: SoundQueue, saves: SavesFolderManager) = this

	/**
	 * This method should be called a fixed number of times per second on [GameStateManager.currentState].
	 *
	 * Unless the [Content] has not been deserialized yet: in that case, [updateBeforeContent] should be
	 * called instead.
	 */
	fun update(context: GameStateUpdateContext): GameState
}

/**
 * This class is used as the parameter type for [GameState.update].
 * Furthermore, it is the parent class of e.g. [CampaignState.UpdateContext].
 *
 * Using this 'parameter class' instead of adding all its fields as loose parameters makes it easier to add
 * 'parameters', since it is not needed to change all the method signatures.
 */
open class GameStateUpdateContext(

	/**
	 * The game [Content], which defines e.g. all the skills, items, and monsters of the game.
	 */
	val content: Content,

	/**
	 * The [InputManager] that tracks all the keyboard & mouse presses.
	 */
	val input: InputManager,

	/**
	 * The [SoundQueue] where all the sounds-to-be-played should be inserted. The `AudioUpdater` ensures that these
	 * sounds are actually played.
	 */
	val soundQueue: SoundQueue,

	/**
	 * The update timestep.
	 *
	 * When the [GameState.update] method is invoked e.g. 50 times per second, this should be 1s / 50 = 20ms.
	 */
	val timeStep: Duration,

	/**
	 * The [SavesFolderManager]
	 *
	 * - During real game sessions, this should use the real [mardek.state.saves.SAVES_DIRECTORY].
	 * - During unit tests, it probably uses some dummy directory instead.
	 */
	val saves: SavesFolderManager = SavesFolderManager(),
) {
	constructor(copy: GameStateUpdateContext) : this(
		copy.content, copy.input,
		copy.soundQueue, copy.timeStep,
		copy.saves,
	)
}
