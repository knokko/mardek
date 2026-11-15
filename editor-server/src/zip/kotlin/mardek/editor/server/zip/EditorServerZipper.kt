package mardek.editor.server.zip

import java.io.BufferedOutputStream
import java.io.File
import java.nio.file.Files
import mardek.content.Content
import mardek.editor.EDITOR_KEY_ALIAS
import mardek.editor.util.includeDirectory
import mardek.editor.util.includeFile
import mardek.editor.util.writeTextFile
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import java.util.UUID
import java.util.stream.Collectors

fun main(args: Array<String>) {
	if (args.size != 1) {
		throw RuntimeException("You should use ./gradlew zipEditorServer --args=\"SERVER_IP\"")
	}
	val serverIp = args[0]

	val serverJar = File("${Content.RESOURCES_DIRECTORY}/../editor-server/build/libs/editor-server-all.jar")
	val gameJar = File("${Content.RESOURCES_DIRECTORY}/../game/build/libs/mardek.jar")
	val clientJar = File("${Content.RESOURCES_DIRECTORY}/../editor-client/desktopApp/build/libs/desktopApp-all.jar")
	val contentBits = File("${Content.RESOURCES_DIRECTORY}/../editor-server/content.bits")
	val musicFolder = File("${Content.RESOURCES_DIRECTORY}/music")
	val gameWindowsRuntime = File("${Content.RESOURCES_DIRECTORY}/game-java-runtime-windows-x64.zip")
	val gameLinuxRuntime = File("${Content.RESOURCES_DIRECTORY}/game-java-runtime-linux-x64.zip")
	val editorWindowsRuntime = File("${Content.RESOURCES_DIRECTORY}/editor-java-runtime-windows-x64.zip")
	val editorLinuxRuntime = File("${Content.RESOURCES_DIRECTORY}/editor-java-runtime-linux-x64.zip")

	val outputFile = File("./editor-server.zip")
	val zipOutput = ZipArchiveOutputStream(BufferedOutputStream(Files.newOutputStream(outputFile.toPath())))

	includeFile(zipOutput, serverJar, "/editor-server.jar")
	includeFile(zipOutput, gameJar, "/mardek.jar")
	includeFile(zipOutput, clientJar, "/editor-client.jar")
	includeFile(zipOutput, contentBits, "/editor-server/content.bits")
	includeFile(zipOutput, gameWindowsRuntime, "/resources/game-java-runtime-windows-x64.zip")
	includeFile(zipOutput, gameLinuxRuntime, "/resources/game-java-runtime-linux-x64.zip")
	includeFile(zipOutput, editorWindowsRuntime, "/resources/editor-java-runtime-windows-x64.zip")
	includeFile(zipOutput, editorLinuxRuntime, "/resources/editor-java-runtime-linux-x64.zip")
	includeDirectory(zipOutput, musicFolder, "/resources/music/")

	val keyStoreFile = Files.createTempFile("keystore", ".p12").toFile()
	keyStoreFile.delete()
	val password = UUID.randomUUID().toString()
	val generateKeyStore = Runtime.getRuntime().exec(arrayOf(
		"keytool", "-genkeypair", "-keystore", keyStoreFile.absolutePath, "-storetype", "PKCS12",
		"-storepass", password, "-alias", EDITOR_KEY_ALIAS, "-keyalg", "RSA", "-keysize", "2048",
		"-validity", "99999", "-dname", "CN=$serverIp"
	))
	val generateKeyStoreResult = generateKeyStore.waitFor()
	if (generateKeyStoreResult != 0) {
		println("errors are ${generateKeyStore.errorReader().lines().collect(Collectors.joining("\n"))}")
		println("output was ${generateKeyStore.inputReader().lines().collect(Collectors.joining("\n"))}")
		throw RuntimeException("keytool (keystore) failed with $generateKeyStoreResult")
	}

	val publicKeyFile = Files.createTempFile("public", ".pem").toFile()
	publicKeyFile.delete()
	val generatePublicKey = Runtime.getRuntime().exec(arrayOf(
		"keytool", "-exportcert", "-keystore", keyStoreFile.absolutePath,
		"-storepass", password, "-alias", EDITOR_KEY_ALIAS, "-rfc", "-file", publicKeyFile.absolutePath,
	), null, null)
	val generatePublicKeyResult = generatePublicKey.waitFor()
	if (generatePublicKeyResult != 0) {
		println("errors are ${generatePublicKey.errorReader().lines().collect(Collectors.joining("\n"))}")
		println("output was ${generatePublicKey.inputReader().lines().collect(Collectors.joining("\n"))}")
		throw RuntimeException("keytool (public key) failed with $generatePublicKeyResult")
	}

	includeFile(zipOutput, keyStoreFile, "/editor-server/certificate/key-store.p12")
	includeFile(zipOutput, publicKeyFile, "/editor-server/certificate/public-certificate.pem")
	keyStoreFile.delete()
	publicKeyFile.delete()

	writeTextFile(
		zipOutput,
		"/editor-server/server.properties",
		arrayOf("ip=$serverIp", "port=39657", "password=$password")
	)
	writeTextFile(zipOutput, "/run.sh", arrayOf(
		"java -XX:+UseZGC -XX:ZUncommitDelay=10 -XX:SoftMaxHeapSize=500m -jar editor-server.jar"
	), makeExecutable = true)

	zipOutput.putArchiveEntry(ZipArchiveEntry("/resources/"))
	zipOutput.closeArchiveEntry()

	zipOutput.finish()
	zipOutput.flush()
	zipOutput.close()
}
