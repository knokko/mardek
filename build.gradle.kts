import java.net.URI

buildscript {
	repositories {
		mavenCentral()
	}

	dependencies {
		classpath("com.guardsquare:proguard-gradle:7.8.2")
	}
}

plugins {
	java
	alias(libs.plugins.kotlinJvm) apply false
}

allprojects {

	apply {
		plugin(JavaPlugin::class)
	}

	java {
		withSourcesJar()
	}

	repositories {
		mavenCentral()

		maven {
			name = "knokko-reposilite"
			url = URI("https://49.12.188.159:8080/releases/")
			content {
				includeGroup("com.github.knokko")
				includeGroup("com.github.knokko.vk-compressor")
			}
		}

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
		implementation(platform("org.lwjgl:lwjgl-bom:${rootProject.libs.versions.lwjgl.get()}"))
		testImplementation(platform("org.junit:junit-bom:5.12.2"))
		testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
		testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	}

	project.ext.set("vulkanImplementationDependencies", arrayListOf(
			rootProject.libs.vk.boiler,
			rootProject.libs.joml,
			rootProject.libs.lwjgl,
			rootProject.libs.lwjgl.vulkan,
			rootProject.libs.lwjgl.harfbuzz,
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
}
