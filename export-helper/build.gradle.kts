plugins {
	id("shared-game-configuration")
	id("java-library")
	alias(libs.plugins.kotlinJvm)
}

dependencies {
	implementation(project(":bitser"))
	implementation(project(":content"))
	implementation(project(":state"))
	implementation(libs.lwjgl.zstd)
	for (natives in (project.ext.get("lwjglNatives") as ArrayList<*>)) {
		runtimeOnly("org.lwjgl:lwjgl::$natives")
		runtimeOnly("org.lwjgl:lwjgl-zstd::$natives")
	}
}
