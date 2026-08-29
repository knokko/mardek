package mardek.editor.client

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform