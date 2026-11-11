package mardek.editor.client

import com.github.knokko.boiler.window.WindowEventLoop
import mardek.exporting.exportAndPrepareVk2d
import mardek.exporting.exportIcons
import mardek.exporting.exportTitleScreen
import mardek.game.MardekSdlInput
import mardek.game.MardekWindow
import mardek.game.createBoiler
import mardek.input.InputManager
import mardek.state.GameStateManager
import mardek.state.saves.SavesFolderManager
import mardek.state.settings.UserSettings
import mardek.state.title.TitleScreenState
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.fileSize

private fun exportToTempFiles(
	testRunState: TestRunState,
	titleScreenVk2dFile: Path,
	titleScreenContentFile: Path,
	mainVk2dFile: Path,
	mainContentFile: Path,
	iconsFile: Path,
) {
	val contentWrapper = testRunState.content!!
	exportTitleScreen(
		dataFile = titleScreenContentFile.toFile(),
		vk2dFile = titleScreenVk2dFile.toFile(),
		wrapped = testRunState.content!!,
	)
	println("after title screen")
	testRunState.reportProgress()

	exportAndPrepareVk2d(
		dataFile = mainContentFile.toFile(),
		vk2dFile = mainVk2dFile.toFile(),
		compressionLevel = 15,
		cacheDirectory = null,
		target = contentWrapper.content,
	)
	println("main 1")
	testRunState.reportProgress()

	exportIcons(contentWrapper, Files.newOutputStream(iconsFile), true)
	println("Exported client content: sizes are ${mainContentFile.fileSize()} and ${mainVk2dFile.fileSize()}")

	for (file in arrayOf(titleScreenVk2dFile, titleScreenContentFile, mainContentFile, mainVk2dFile, iconsFile)) {
		file.toFile().deleteOnExit()
	}
}

fun runEditorClientTest(testRunState: TestRunState) {
	val titleScreenVk2dFile = Files.createTempFile("title-", ".vk2d")
	val titleScreenContentFile = Files.createTempFile("title-", ".bits")
	val mainVk2dFile = Files.createTempFile("content-", ".vk2d")
	val mainContentFile = Files.createTempFile("content-", ".bits")
	val iconsFile = Files.createTempFile("icons-", ".bin")

	testRunState.reportProgress()
	exportToTempFiles(testRunState, titleScreenVk2dFile, titleScreenContentFile, mainVk2dFile, mainContentFile, iconsFile)
	System.gc()
	println("main suspect")
	testRunState.reportProgress()

	val userSettings = UserSettings.load()
	val fakeMainArgs = arrayOf<String>()
	val boiler = createBoiler(fakeMainArgs, userSettings.videoSettings)

	val input = InputManager()
	val state = GameStateManager(input, TitleScreenState(), SavesFolderManager())
	testRunState.reportProgress()

	val window = MardekWindow(
		state, userSettings, boiler.window(),
		titleScreenContentFile = titleScreenContentFile.toFile(),
		titleScreenVk2dFile = titleScreenVk2dFile.toFile(),
		mainContentFile = mainContentFile.toFile(),
		mainVk2dFile = mainVk2dFile.toFile(),
	)
	val inputListener = MardekSdlInput(boiler.window(), state, input, iconsFile = iconsFile.toFile())
	inputListener.register()

	val eventLoop = WindowEventLoop(0.01, inputListener::update)
	eventLoop.addWindow(window)
	testRunState.reportProgress()
	eventLoop.runMain()
	testRunState.reportProgress()

	inputListener.destroy()
	boiler.destroyInitialObjects()
	for (file in arrayOf(titleScreenVk2dFile, titleScreenContentFile, mainContentFile, mainVk2dFile, iconsFile)) {
		file.deleteIfExists()
	}
}
