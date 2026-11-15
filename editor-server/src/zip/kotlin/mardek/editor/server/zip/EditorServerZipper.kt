package mardek.editor.server.zip

import java.io.BufferedOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipOutputStream
import mardek.content.Content
import mardek.editor.EDITOR_KEY_ALIAS
import java.io.PrintWriter
import java.util.UUID
import java.util.zip.ZipEntry

fun main(args: Array<String>) {
	if (args.size != 1) {
		throw RuntimeException("You should use ./gradlew zipEditorServer --args=\"SERVER_IP\"")
	}
	val serverIp = args[0]

	val serverJar = File("${Content.RESOURCES_DIRECTORY}/../editor-server/build/libs/editor-server-all.jar")
	val gameJar = File("${Content.RESOURCES_DIRECTORY}/../game/build/libs/mardek.jar")
	val clientJar = File("${Content.RESOURCES_DIRECTORY}/../editor-client/desktopApp/build/libs/desktopApp-all.jar")
	val contentBits = File("${Content.RESOURCES_DIRECTORY}/../editor-server/content.bits")

	val outputFile = File("./editor-server.zip")
	val zipOutput = ZipOutputStream(BufferedOutputStream(Files.newOutputStream(outputFile.toPath())))

	fun includeFile(source: File, path: String) {
		zipOutput.putNextEntry(ZipEntry(path))
		zipOutput.write(source.readBytes())
		zipOutput.closeEntry()
	}

	includeFile(serverJar, "/editor-server.jar")
	includeFile(gameJar, "/mardek.jar")
	includeFile(clientJar, "/editor-client.jar")
	includeFile(contentBits, "/editor-server/content.bits")

	val keyStoreFile = Files.createTempFile("keystore", ".p12").toFile()
	keyStoreFile.deleteOnExit()
	val password = UUID.randomUUID().toString()
	val generateKeyStore = Runtime.getRuntime().exec(arrayOf(
		"keytool", "-genkeypair", "-keystore", keyStoreFile.absolutePath, "-storetype", "PKCS12",
		"-storepass", password, "-alias", EDITOR_KEY_ALIAS, "-keyalg", "RSA", "-keysize", "2048",
		"-validity", "99999", "-dname", "CN=$serverIp"
	))
	val generateKeyStoreResult = generateKeyStore.waitFor()
	if (generateKeyStoreResult != 0) {
		throw RuntimeException("keytool (keystore) failed with $generateKeyStoreResult")
	}

	val publicKeyFile = Files.createTempFile("public", ".pem").toFile()
	publicKeyFile.deleteOnExit()
	val generatePublicKey = Runtime.getRuntime().exec(arrayOf(
		"keytool", "-exportcert", "-keystore", keyStoreFile.absolutePath,
		"-storepass", password, "-alias", EDITOR_KEY_ALIAS, "-rfc", "-file", publicKeyFile.absolutePath,
	))
	val generatePublicKeyResult = generatePublicKey.waitFor()
	if (generatePublicKeyResult != 0) {
		throw RuntimeException("keytool (public key) failed with $generatePublicKeyResult")
	}

	includeFile(keyStoreFile, "/editor-server/certificate/key-store.p12")
	includeFile(publicKeyFile, "/editor-server/certificate/public-certificate.pem")

	fun writeTextFile(path: String, lines: Array<String>) {
		zipOutput.putNextEntry(ZipEntry(path))
		val writer = PrintWriter(zipOutput)
		for (line in lines) writer.println(line)
		writer.flush()
		zipOutput.closeEntry()
	}

	writeTextFile(
		"/editor-server/server.properties",
		arrayOf("ip=$serverIp", "port=39657", "password=$password")
	)
	writeTextFile("/run.sh", arrayOf(
		"java -XX:+UseZGC -XX:ZUncommitDelay=10 -XX:SoftMaxHeapSize=500m -jar editor-server.jar"
	))

	zipOutput.putNextEntry(ZipEntry("/resources/"))
	zipOutput.closeEntry()

	zipOutput.finish()
	zipOutput.flush()
	zipOutput.close()
}
