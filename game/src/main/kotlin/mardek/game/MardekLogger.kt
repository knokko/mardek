package mardek.game

import mardek.state.saves.LOGS_DIRECTORY
import java.io.File
import java.io.PrintStream

fun configureLogging(args: Array<String>) {
	if (args.contains("enable-logging")) {
		val time = System.currentTimeMillis()
		LOGS_DIRECTORY.mkdirs()

		val oldOut = System.out
		val oldErr = System.err
		System.setOut(ExtraPrintStream(oldOut, File("$LOGS_DIRECTORY/stdout$time.txt")))
		System.setErr(ExtraPrintStream(oldErr, File("$LOGS_DIRECTORY/stderr$time.txt")))
	}
}

private class ExtraPrintStream(private val original: PrintStream, file: File) : PrintStream(file) {
	override fun print(s: String?) {
		super.print(s)
		original.print(s)
	}

	override fun println(x: String?) {
		super.println(x)
		original.println(x)
	}

	override fun println() {
		super.println()
		original.println()
	}
}
