import java.net.URI

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

plugins {
	java
	id("knokko-reposilite")
}

java {
	withSourcesJar()
}

repositories {
	mavenCentral()

	// Used for LWJGL and JOML
	maven {
		name = "sonatype"
		url = URI("https://central.sonatype.com/repository/maven-snapshots")
		content {
			includeGroup("org.joml")
			includeGroup("org.lwjgl")
		}
	}
}

project.ext.set("lwjglNatives", arrayListOf(
	"natives-linux", "natives-linux-arm32", "natives-linux-arm64",
	"natives-linux-ppc64le", "natives-linux-riscv64", "natives-freebsd",
	"natives-windows", "natives-windows-x86", "natives-windows-arm64",
	"natives-macos", "natives-macos-arm64"
))

dependencies {
	implementation(platform("org.lwjgl:lwjgl-bom:${libs.findVersion("lwjgl").get()}"))
	testImplementation(platform("org.junit:junit-bom:5.12.2"))
	testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

project.ext.set("vulkanImplementationDependencies", arrayListOf(
	libs.findLibrary("vk-boiler").get(),
	libs.findLibrary("joml").get(),
	libs.findLibrary("lwjgl").get(),
	libs.findLibrary("lwjgl-vulkan").get(),
	libs.findLibrary("lwjgl-harfbuzz").get(),
))

@Suppress("UNCHECKED_CAST")
project.ext.set("vulkanRuntimeDependencies", (project.ext.get("lwjglNatives") as ArrayList<String>).flatMap {
	val result = mutableListOf("org.lwjgl:lwjgl::$it")
	if (it.startsWith("natives-macos")) result.add("org.lwjgl:lwjgl-vulkan::$it")
	result.add("org.lwjgl:lwjgl-harfbuzz::$it")
	result.add("org.lwjgl:lwjgl-zstd::$it")
	result
})

tasks.named<Test>("test") {
	useJUnitPlatform()

	minHeapSize = "512m"
	maxHeapSize = "2g"
}
