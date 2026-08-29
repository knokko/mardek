plugins {
	id("shared-game-configuration")
}

dependencies {
	implementation(libs.vk.boiler)
	implementation(libs.lwjgl)
	implementation(libs.lwjgl.harfbuzz)
	implementation(libs.lwjgl.sdl)
	implementation(libs.lwjgl.vulkan)
	implementation(libs.lwjgl.zstd)

	compileOnly(libs.lwjgl.harfbuzz)
	implementation("com.github.knokko.vk-compressor:bc4:${libs.versions.vk.compressor.get()}")
	for (format in arrayOf("kim1", "kim2", "kim3", "bc1", "bc7")) {
		compileOnly("com.github.knokko.vk-compressor:$format:${libs.versions.vk.compressor.get()}")
	}

	testImplementation(libs.lwjgl.harfbuzz)
	testImplementation(libs.lwjgl.vma)
	for (format in arrayOf("kim1", "kim2", "kim3", "bc1", "bc4", "bc7")) {
		testImplementation("com.github.knokko.vk-compressor:$format:${libs.versions.vk.compressor.get()}")
	}

	@Suppress("UNCHECKED_CAST")
	val myRuntimeDependencies = (project.ext.get("lwjglNatives") as ArrayList<String>).flatMap {
		val result = mutableListOf("org.lwjgl:lwjgl::$it", "org.lwjgl:lwjgl-sdl::$it")
		if (it.startsWith("natives-macos")) result.add("org.lwjgl:lwjgl-vulkan::$it")
		result
	}
	for (dependency in myRuntimeDependencies) {
		runtimeOnly(dependency)
	}
	for (natives in (project.ext.get("lwjglNatives") as ArrayList<*>)) {
		runtimeOnly("org.lwjgl:lwjgl-harfbuzz::$natives")
		runtimeOnly("org.lwjgl:lwjgl-zstd::$natives")
		testRuntimeOnly("org.lwjgl:lwjgl-vma::$natives")
	}
}

tasks.register("generateImageBenchmarkResources", JavaExec::class) {
	group = "Benchmark"
	description = "Generates the resources needed for e.g. Kim1Benchmark"
	classpath = sourceSets.test.get().runtimeClasspath
	mainClass = "com.github.knokko.vk2d.ImageBenchmarkResourceWriter"
}

tasks.register("kim1Benchmark", JavaExec::class) {
	group = "Benchmark"
	description = "Runs the kim1 benchmark"
	classpath = sourceSets.test.get().runtimeClasspath
	mainClass = "com.github.knokko.vk2d.Kim1Benchmark"
}

tasks.register("kim3Benchmark", JavaExec::class) {
	group = "Benchmark"
	description = "Runs the kim3 benchmark"
	classpath = sourceSets.test.get().runtimeClasspath
	mainClass = "com.github.knokko.vk2d.Kim3Benchmark"
}

tasks.register("textPlayground", JavaExec::class) {
	group = "Benchmark"
	description = "Runs the text benchmark & playground"
	classpath = sourceSets.main.get().runtimeClasspath
	mainClass = "com.github.knokko.vk2d.text.TextPlayground"
}
