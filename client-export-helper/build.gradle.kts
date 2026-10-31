plugins {
	id("shared-game-configuration")
	id("java-library")
	alias(libs.plugins.kotlinJvm)
}

dependencies {
	implementation(project(":bitser"))
	implementation(project(":content"))
	implementation(project(":state"))
	implementation(project(":vk2d"))
	api(project(":export-helper"))
	implementation(libs.vk.boiler)
	implementation(libs.lwjgl.zstd)

	for (format in arrayOf("kim1", "kim3")) {
		compileOnly("com.github.knokko.vk-compressor:$format:${libs.versions.vk.compressor.get()}")
	}
}
