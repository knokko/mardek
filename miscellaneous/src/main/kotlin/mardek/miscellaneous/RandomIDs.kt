package mardek.miscellaneous

import java.util.UUID

fun main() {
	repeat(100) {
		println("UUID.fromString(\"${UUID.randomUUID()}\"),")
	}
}
