plugins {
	id("shared-game-configuration")
	alias(libs.plugins.kotlinJvm)
}

dependencies {
	implementation(project(":content"))
	implementation(project(":input"))
}
