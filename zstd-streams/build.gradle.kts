plugins {
	id("shared-game-configuration")
}

dependencies {
	implementation(libs.lwjgl)
	implementation(libs.lwjgl.zstd)
	for (natives in (project.ext.get("lwjglNatives") as ArrayList<*>)) {
		runtimeOnly("org.lwjgl:lwjgl::$natives")
		runtimeOnly("org.lwjgl:lwjgl-zstd::$natives")
	}
}
