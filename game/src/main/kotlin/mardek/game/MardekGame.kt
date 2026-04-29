package mardek.game

import com.github.knokko.boiler.window.WindowEventLoop
import mardek.input.InputManager
import mardek.state.VideoSettings
import mardek.state.GameStateManager
import mardek.state.saves.SavesFolderManager
import mardek.state.title.TitleScreenState
import org.lwjgl.sdl.SDL
import org.lwjgl.util.zstd.Zstd
import org.lwjgl.vulkan.VK

val mainStartTime = System.nanoTime()

fun main(args: Array<String>) {
	if (args.contains("self-test1")) {
		selfTest1()
		return
	}
	if (neededToRestartProcess()) return
	configureLogging(args)

	// Starting these threads seems to reduce the start-up time slightly.
	// They basically force the JVM to start loading SDL, Vulkan, and Zstd earlier
	Thread { SDL.getLibrary() }.start()
	Thread { VK.getFunctionProvider() }.start()
	Thread { Zstd.ZSTD_versionNumber() }.start()

	val videoSettings = VideoSettings.load()
	println("Started with BoilerInstance after ${(System.nanoTime() - mainStartTime) / 1000_000L}ms")
	val boiler = createBoiler(args, videoSettings)
	println("Created BoilerInstance after ${(System.nanoTime() - mainStartTime) / 1000_000L}ms")

	val input = InputManager()
	val state = GameStateManager(input, TitleScreenState(), SavesFolderManager())

	val window = MardekWindow(state, videoSettings, boiler.window())
	val inputListener = MardekSdlInput(boiler.window(), state, input)
	inputListener.register()

	val eventLoop = WindowEventLoop(0.01, inputListener::update)
	eventLoop.addWindow(window)
	println("Started event loop after ${(System.nanoTime() - mainStartTime) / 1000_000L}ms")
	eventLoop.runMain()

	boiler.destroyInitialObjects()
}
