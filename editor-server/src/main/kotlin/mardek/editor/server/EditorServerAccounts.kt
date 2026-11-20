package mardek.editor.server

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.Bitser
import com.github.knokko.bitser.field.BitField
import mardek.content.BITSER
import mardek.content.Content
import mardek.editor.InitialClientConfig
import java.io.DataOutputStream
import java.io.File
import java.io.OutputStream
import java.io.Serializable
import java.nio.file.Files
import java.nio.file.StandardCopyOption
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
			val username = command.substringAfter("add ")

			val newAccount = synchronized(this) {
				if (entries.any { it.username == username }) {
					println("User $username already exists")
					return
				}

				val newAccount = Entry(username = username)
				entries.add(newAccount)
				newAccount
			}

			val certificateFile = File(
				"${Content.RESOURCES_DIRECTORY}/../editor-server/certificate/public-certificate.pem"
			)
			val clientConfig = InitialClientConfig(
				serverIp = properties.ip,
				serverPort = properties.port,
				serverCertificate = Files.readAllBytes(certificateFile.toPath()),
				initialPassword = newAccount.password,
			)
			val configBytes = BITSER.toBytes(clientConfig)
			val configHex = configBytes.toHexString()
			println("Copy & paste the following client config: $configHex")
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

	fun getByPassword(password: UUID, output: OutputStream): Any? {
		val entry = entries.find { it.password == password }
		if (entry != null) entry.claimIfNew(output)
		return entry
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
		var password = UUID.randomUUID()!!
			private set

		@BitField(id = 2)
		private var isClaimed = false

		fun claimIfNew(output: OutputStream) {
			if (!isClaimed) {
				password = UUID.randomUUID()
				val dataOutput = DataOutputStream(output)
				dataOutput.writeLong(password.mostSignificantBits)
				dataOutput.writeLong(password.leastSignificantBits)
				dataOutput.flush()
				isClaimed = true
			}
		}
	}
}
