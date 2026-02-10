package mardek.game

import org.lwjgl.system.Platform

internal fun neededToRestartProcess(): Boolean {
	if (Platform.get() != Platform.MACOSX) return false
	val javaProcess = ProcessHandle.current().info()

	val requiredArgument = "-XstartOnFirstThread"
	val executable = javaProcess.command()
	val arguments = javaProcess.arguments()
	if (executable.isPresent && arguments.isPresent) {
		val args: Array<String> = arguments.get()
		if (args.contains(requiredArgument)) return false

		val newCommand = arrayOf(executable.get(), requiredArgument) + args
		println("new command is ${newCommand.contentToString()}")
		val newProcess = ProcessBuilder(newCommand.toList()).inheritIO().start()
		newProcess.onExit().get()
		return true
	} else {
		println("Failed to add -XstartOnFirstThread to $executable ${arguments.map { it.contentToString() }}")
		return false
	}
}
