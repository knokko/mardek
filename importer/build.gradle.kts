plugins {
	id("shared-game-configuration")
	alias(libs.plugins.kotlinJvm)
}

dependencies {
	for (dependency in (project.ext.get("vulkanImplementationDependencies") as ArrayList<*>)) {
		implementation(dependency)
	}
	implementation(libs.lwjgl.zstd)
	for (dependency in (project.ext.get("vulkanRuntimeDependencies") as ArrayList<*>)) {
		runtimeOnly(dependency)
	}
	for (natives in (project.ext.get("lwjglNatives") as ArrayList<*>)) {
		runtimeOnly("org.lwjgl:lwjgl-zstd::$natives")
	}
	for (format in arrayOf("kim1", "kim2", "kim3", "bc1", "bc4", "bc7")) {
		implementation("com.github.knokko.vk-compressor:$format:${libs.versions.vk.compressor.get()}")
	}
	implementation(files("../flash/ffdec_lib.jar"))
	implementation(project(":content"))
	implementation(project(":state"))
	implementation(project(":vk2d"))
	implementation("org.apache.xmlgraphics:batik-all:1.19")
	implementation("com.github.knokko:sample-profiler:1.0.0")
}

tasks.register("exportContent", JavaExec::class) {
	group = "Export"
	description = "Imports the vanilla content, and exports it to game/src/main/resources/mardek/game"

	classpath(sourceSets.main.get().runtimeClasspath)
	mainClass = "mardek.importer.ContentExporterKt"

	jvmArgs("--add-exports", "java.base/jdk.internal.misc=ALL-UNNAMED")
	jvmArgs("--enable-native-access", "ALL-UNNAMED")
}

tasks.register("convertSVGs", JavaExec::class) {
	group = "Export"
	description = "Converts the Flash shapes from SVG to PNG. See the comments in " +
			"importer/converter/SvgShapeConverter.kt for more information."

	classpath(sourceSets.main.get().runtimeClasspath)
	mainClass = "mardek.importer.converter.SvgShapeConverterKt"
}

tasks.register("importRawAreaData", JavaExec::class) {
	group = "Export"
	description = "Extracts the raw area data from JPEX, " +
			"and exports it to importer/src/main/resources/mardek/importer/area/data-raw"

	classpath(sourceSets.main.get().runtimeClasspath)
	mainClass = "mardek.importer.area.AreaDumperKt"
}
