package mardek.editor.server.dummy2


import com.github.knokko.bitser.connection.BitServer
import mardek.content.inventory.EquipmentProperties
import mardek.content.inventory.WeaponProperties
import mardek.content.stats.Resistances
import mardek.editor.*
import mardek.editor.server.EDITOR_KEY_PASSWORD
import mardek.editor.view.generateDummyView2
import tech.kwik.core.QuicConnection
import tech.kwik.core.log.SysOutLogger
import tech.kwik.core.server.ApplicationProtocolConnection
import tech.kwik.core.server.ApplicationProtocolConnectionFactory
import tech.kwik.core.server.ServerConnectionConfig
import tech.kwik.core.server.ServerConnector
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.lang.Thread.sleep
import java.security.KeyStore

private class ProtocolConnection(
	private val controllers: BitServer.ControllerMapping,
	private val clientConnection: QuicConnection
) : ApplicationProtocolConnection {

	init {
		val thread = Thread {
			val mainStream = clientConnection.createStream(true)

			// It looks like the client cannot see the stream until the server writes the first byte
			mainStream.outputStream.write(0)
			mainStream.outputStream.flush()

			val authToken = mainStream.inputStream.readNBytes(AUTH_TOKEN_LENGTH)
			if (!authToken.contentEquals(TEST_AUTH_TOKEN)) {
				clientConnection.close()
				return@Thread
			}

			val keepAliveThread = Thread {
				while (true) {
					mainStream.outputStream.write(123)
					mainStream.outputStream.flush()
					sleep(10_000L)
				}
			}
			keepAliveThread.isDaemon = true
			keepAliveThread.start()

			val dataInput = DataInputStream(mainStream.inputStream)
			while (true) {
				val requestedStreamID = dataInput.readInt()
				val controllerID = dataInput.readLong()
				println("creating stream $requestedStreamID for controller $controllerID")
				val nextStream = clientConnection.createStream(true)
				val dataOutput = DataOutputStream(nextStream.outputStream)
				dataOutput.writeInt(requestedStreamID)
				dataOutput.flush()

				val controller = controllers.getControllerById(controllerID) as BitServer.StructController<*>
				controller.addClient(nextStream.outputStream, nextStream.inputStream) {
					nextStream.resetStream(0L)
				}
				println("added client to the controller")
			}
		}
		thread.isDaemon = true
		thread.start()
	}
}

private class ProtocolFactory(
	private val controllers: BitServer.ControllerMapping
) : ApplicationProtocolConnectionFactory {

	override fun createConnection(
		protocol: String,
		quicConnection: QuicConnection
	): ApplicationProtocolConnection? {
		if (protocol != EDITOR_APPLICATION_PROTOCOL_NAME) {
			quicConnection.close()
			return null
		}
		return ProtocolConnection(controllers, quicConnection)
	}
}

fun main() {
	val config = ServerConnectionConfig.builder()
		.maxTotalPeerInitiatedBidirectionalStreams(0)
		.maxTotalPeerInitiatedUnidirectionalStreams(0)
		.maxOpenPeerInitiatedBidirectionalStreams(0)
		.maxOpenPeerInitiatedUnidirectionalStreams(0)
		.maxConnectionBufferSize(100_000L)
		.maxUnidirectionalStreamBufferSize(0L)
		.maxBidirectionalStreamBufferSize(1000L)
		.build()

	val keyStore = KeyStore.getInstance(
		File("$SERVER_CERTIFICATE_FOLDER/key-store.p12"),
		EDITOR_KEY_PASSWORD.toCharArray()
	)

	val logger = SysOutLogger()

	val connector = ServerConnector.builder()
		.withPort(EDITOR_PORT)
		.withConfiguration(config)
		.withKeyStore(keyStore, EDITOR_KEY_ALIAS, EDITOR_KEY_PASSWORD.toCharArray())
		.withLogger(logger)
		.build()

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

	val controllerMapping = BitServer.ControllerMapping()

	val rootView = generateDummyView2()
	val weaponView = rootView.getChildStructView(null, "weapon")
	val rootController = BitServer.StructController(controllerMapping, rootStruct, rootView)
	val weaponController = BitServer.StructController(controllerMapping, weaponProperties, weaponView)

	controllerMapping.add(rootController)
	controllerMapping.add(weaponController)

	connector.registerApplicationProtocol(
		EDITOR_APPLICATION_PROTOCOL_NAME,
		ProtocolFactory(controllerMapping)
	)
	connector.start()
	println("Press ENTER to exit...")
	readln()
	println("Exiting...")
	connector.close()
	println("Stopped the server")
}
