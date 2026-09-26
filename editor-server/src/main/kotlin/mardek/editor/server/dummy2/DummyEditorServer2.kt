package mardek.editor.server.dummy2

import com.github.knokko.bitser.kwik.BikServer
import com.github.knokko.bitser.kwik.BikServerProtocol
import mardek.content.inventory.EquipmentProperties
import mardek.content.inventory.WeaponProperties
import mardek.content.stats.Resistances
import mardek.editor.*
import mardek.editor.server.EDITOR_KEY_PASSWORD
import mardek.editor.view.generateDummyView2
import tech.kwik.core.QuicConnection
import tech.kwik.core.QuicStream
import tech.kwik.core.log.SysOutLogger
import tech.kwik.core.server.ServerConnectionConfig
import tech.kwik.core.server.ServerConnector
import java.io.File
import java.security.KeyStore

private class DummyEditorServerProtocol2 : BikServerProtocol {

	override fun configureConfig(builder: ServerConnectionConfig.Builder) {
		builder.maxConnectionBufferSize(10_000_000L)
	}

	override fun configureConnector(builder: ServerConnector.Builder) {
		val keyStore = KeyStore.getInstance(
			File("$SERVER_CERTIFICATE_FOLDER/key-store.p12"),
			EDITOR_KEY_PASSWORD.toCharArray()
		)

		val logger = SysOutLogger()

		builder.withPort(EDITOR_PORT)
		builder.withKeyStore(keyStore, EDITOR_KEY_ALIAS, EDITOR_KEY_PASSWORD.toCharArray())
		builder.withLogger(logger)
	}

	override fun runHandshake(clientConnection: QuicConnection, mainStream: QuicStream): Boolean {
		mainStream.outputStream.write(0)
		mainStream.outputStream.flush()

		val authToken = mainStream.inputStream.readNBytes(AUTH_TOKEN_LENGTH)
		return authToken.contentEquals(TEST_AUTH_TOKEN)
	}

	override fun waitUntilServerShouldStop() {
		println("Press ENTER to exit...")
		readln()
		println("Exiting...")
	}
}

fun main() {
	val weaponProperties = WeaponProperties(
		hitChance = 100,
		critChance = 5,
		hpDrain = 0f,
		mpDrain = 0f,
		effectiveAgainstCreatureTypes = ArrayList(0),
		effectiveAgainstElements = ArrayList(0),
		addEffects = ArrayList(0),
		hitSound = null,
	)
	val rootStruct = EquipmentProperties(
		skills = ArrayList(0),
		stats = ArrayList(0),
		elementalBonuses = ArrayList(0),
		resistances = Resistances(),
		autoEffects = ArrayList(0),
		weapon = weaponProperties,
		gem = null,
		onlyUser = null,
		charismaticPerformanceChance = 0,
	)

	BikServer.run(
		DummyEditorServerProtocol2(),
		EDITOR_APPLICATION_PROTOCOL_NAME,
		rootStruct,
		generateDummyView2(),
	)
}
