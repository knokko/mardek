package mardek.editor.server

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.Bitser
import com.github.knokko.bitser.field.BitField
import mardek.content.BITSER
import mardek.content.Content
import mardek.editor.MardekOS
import java.io.File
import java.io.Serializable
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Locale
import java.util.UUID
import kotlin.io.path.writeBytes
import kotlin.time.measureTime

@BitStruct(backwardCompatible = true)
class EditorServerAccounts : Serializable {

	internal var shouldSkipSaving = false

	@BitField(id = 0)
	private val entries = ArrayList<Entry>()

	fun executeCommand(command: String, properties: EditorServerProperties) {
		if (command == "save") {
			val savingTime = measureTime { save() }
			println("Saving accounts took $savingTime ms")
		}

		if (command.startsWith("add ")) {
			val addCommand = command.substringAfter("add ")
			val indexSpace = addCommand.indexOf(' ')
			if (indexSpace == -1) {
				println("Use: accounts add <os> <username>")
				return
			}

			val rawOS = addCommand.substring(0, indexSpace)
			val os = MardekOS.entries.find { it.name.equals(rawOS, ignoreCase = true) }
			if (os == null) {
				println("Invalid OS $rawOS: use one of ${MardekOS.entries.map { it.name.lowercase(Locale.ROOT) }}")
				return
			}

			val username = addCommand.substring(indexSpace + 1)

			val newAccount = synchronized(this) {
				if (entries.any { it.username == username }) {
					println("User $username already exists")
					return
				}

				val newAccount = Entry(username = username)
				entries.add(newAccount)
				newAccount
			}

			val outputFile = File("editor-client-${newAccount.username}.zip")
			zipEditorClient(outputFile, properties, newAccount.password, os)
		}
	}

	fun save() {
		if (shouldSkipSaving) {
			println("Skip saving accounts")
			return
		}

		val tempSave = Files.createTempFile("accounts", ".bits")
		tempSave.toFile().deleteOnExit()

		synchronized(this) {
			tempSave.writeBytes(BITSER.toBytes(this, Bitser.BACKWARD_COMPATIBLE))
			Files.move(tempSave, FILE.toPath(), StandardCopyOption.REPLACE_EXISTING)
		}
	}

	fun getByPassword(password: UUID): Any? {
		return entries.find { it.password == password }
	}

	companion object {

		private val FILE = File("${Content.RESOURCES_DIRECTORY}/../editor-server/accounts.bits")

		fun load(): EditorServerAccounts {
			return if (FILE.exists()) {
				BITSER.fromBytes(
					EditorServerAccounts::class.java,
					FILE.readBytes(),
					Bitser.BACKWARD_COMPATIBLE,
				)
			} else EditorServerAccounts()
		}
	}

	@BitStruct(backwardCompatible = true)
	private class Entry(

		@BitField(id = 0)
		val username: String,
	) : Serializable {

		@BitField(id = 1)
		val password = UUID.randomUUID()!!
	}
}
