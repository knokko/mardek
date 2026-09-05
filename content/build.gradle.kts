plugins {
	id("shared-game-configuration")
	id("java-library")
	alias(libs.plugins.kotlinJvm)
}

dependencies {
	api(project(":bitser"))
	compileOnly(project(":vk2d"))
	for (format in arrayOf("kim1", "kim2", "kim3")) {
		implementation("com.github.knokko.vk-compressor:$format:${libs.versions.vk.compressor.get()}")
	}
}
