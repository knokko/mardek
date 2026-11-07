package mardek.editor.client

import com.github.knokko.boiler.window.WindowEventLoop
import mardek.exporting.ContentWrapper
import mardek.exporting.exportAndPrepareVk2d
import mardek.exporting.exportIcons
import mardek.exporting.exportTitleScreen
import mardek.game.MardekSdlInput
import mardek.game.MardekWindow
import mardek.game.createBoiler
import mardek.game.mainStartTime
import mardek.input.InputManager
import mardek.state.GameStateManager
import mardek.state.saves.SavesFolderManager
import mardek.state.settings.UserSettings
import mardek.state.title.TitleScreenState
import java.io.BufferedOutputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.deleteIfExists

private fun exportToTempFiles(
	contentWrapper: ContentWrapper,
	titleScreenVk2dFile: Path,
	titleScreenContentFile: Path,
	mainVk2dFile: Path,
	mainContentFile: Path,
	iconsFile: Path,
) {
	val titleScreenContentBytes = exportTitleScreen(
		contentWrapper,
		Files.newOutputStream(titleScreenVk2dFile),
		true,
	)
	Files.write(titleScreenContentFile, titleScreenContentBytes)

	val mainContentBytes = exportAndPrepareVk2d(
		BufferedOutputStream(Files.newOutputStream(mainVk2dFile)),
		true,
		null,
		contentWrapper.content,
	)
	Files.write(mainContentFile, ContentWrapper.compress(mainContentBytes, 5))

	exportIcons(contentWrapper, Files.newOutputStream(iconsFile), true)
	println("Exported client content")

	for (file in arrayOf(titleScreenVk2dFile, titleScreenContentFile, mainContentFile, mainVk2dFile, iconsFile)) {
		file.toFile().deleteOnExit()
	}
}

fun runEditorClientTest(contentWrapper: ContentWrapper) {
	println("Export client content...")
	val titleScreenVk2dFile = Files.createTempFile("title-", ".vk2d")
	val titleScreenContentFile = Files.createTempFile("title-", ".bits")
	val mainVk2dFile = Files.createTempFile("content-", ".vk2d")
	val mainContentFile = Files.createTempFile("content-", ".bits")
	val iconsFile = Files.createTempFile("icons-", ".bin")

	exportToTempFiles(contentWrapper, titleScreenVk2dFile, titleScreenContentFile, mainVk2dFile, mainContentFile, iconsFile)
	System.gc()

	val userSettings = UserSettings.load()
	println("Started with BoilerInstance after ${(System.nanoTime() - mainStartTime) / 1000_000L}ms")

	val fakeMainArgs = arrayOf<String>()

	val boiler = createBoiler(fakeMainArgs, userSettings.videoSettings)
	println("Created BoilerInstance after ${(System.nanoTime() - mainStartTime) / 1000_000L}ms")

	val input = InputManager()
	val state = GameStateManager(input, TitleScreenState(), SavesFolderManager())

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
	println("Started event loop after ${(System.nanoTime() - mainStartTime) / 1000_000L}ms")
	eventLoop.runMain()

	boiler.destroyInitialObjects()
	for (file in arrayOf(titleScreenVk2dFile, titleScreenContentFile, mainContentFile, mainVk2dFile, iconsFile)) {
		file.deleteIfExists()
	}
}
