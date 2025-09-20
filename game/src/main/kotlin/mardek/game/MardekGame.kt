package mardek.game

import com.github.knokko.boiler.window.WindowEventLoop
import mardek.input.InputManager
import mardek.state.VideoSettings
import mardek.state.GameStateManager
import mardek.state.title.TitleScreenState

val mainStartTime = System.nanoTime()

fun main(args: Array<String>) {
	if (args.contains("self-test1")) {
		selfTest1()
		return
	}

	val videoSettings = VideoSettings.load()
	val boiler = createBoiler(args, videoSettings)

	val input = InputManager()
	val state = GameStateManager(input, TitleScreenState())

	val window = MardekWindow(state, videoSettings, boiler.window())
	val inputListener = MardekSdlInput(boiler.window(), state, input)
	inputListener.register()

	val eventLoop = WindowEventLoop(0.01, inputListener::update)
	eventLoop.addWindow(window)
	eventLoop.runMain()

	boiler.destroyInitialObjects()
}
