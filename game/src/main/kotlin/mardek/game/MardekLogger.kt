package mardek.game

import mardek.state.saves.LOGS_DIRECTORY
import java.io.File
import java.io.PrintStream

fun configureLogging(args: Array<String>) {
	if (args.contains("enable-logging")) {
		val time = System.currentTimeMillis()
		LOGS_DIRECTORY.mkdirs()
		System.setOut(PrintStream(File("$LOGS_DIRECTORY/stdout$time.txt")))
		System.setErr(PrintStream(File("$LOGS_DIRECTORY/stderr$time.txt")))
	}
}
