package mardek.editor

import mardek.content.Content
import java.io.File

const val EDITOR_APPLICATION_PROTOCOL_NAME = "mardek editor protocol"
const val EDITOR_PORT = 39657
const val EDITOR_KEY_ALIAS = "mardek_editor_key"
const val AUTH_TOKEN_LENGTH = 100

val SERVER_CERTIFICATE_FOLDER = File("${Content.RESOURCES_DIRECTORY}/../editor-server/certificate")

val TEST_AUTH_TOKEN = ByteArray(AUTH_TOKEN_LENGTH) { it.toByte() }
