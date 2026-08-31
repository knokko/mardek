//applyJvmModuleSettings(this)



// The audio module doesn't have unit tests; its test sources only contain a bunch of main() methods
// that are occasionally needed in development.
tasks.test {
	failOnNoDiscoveredTests = false
}

dependencies {
	implementation(project(":content"))
	implementation(project(":state"))

	implementation(libs.lwjgl)
	implementation(libs.lwjgl.openal)
	implementation(libs.lwjgl.stb)
	implementation(libs.lwjgl.zstd)

	for (natives in (project.ext.get("lwjglNatives") as ArrayList<*>)) {
		runtimeOnly("org.lwjgl:lwjgl::$natives")
		runtimeOnly("org.lwjgl:lwjgl-openal::$natives")
		runtimeOnly("org.lwjgl:lwjgl-stb::$natives")
		runtimeOnly("org.lwjgl:lwjgl-zstd::$natives")
	}
}

tasks.register("compressMusic", JavaExec::class) {
	group = "Audio"
	description = "Compresses the files in resources/music"
	classpath(sourceSets.test.get().runtimeClasspath)
	mainClass = "mardek.audio.AudioCompressorKt"
}
