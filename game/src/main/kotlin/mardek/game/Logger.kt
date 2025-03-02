package mardek.game

import java.io.File
import java.io.PrintStream

class Logger(file: File, private val oldOut: PrintStream) : PrintStream(file) {
	override fun write(b: Int) {
		super.write(b)
		oldOut.write(b)
	}

	override fun write(bytes: ByteArray, offset: Int, length: Int) {
		super.write(bytes, offset, length)
		oldOut.write(bytes, offset, length)
	}
}
