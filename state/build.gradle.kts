plugins {
	alias(libs.plugins.kotlinJvm)
}

dependencies {
	implementation(project(":content"))
	implementation(project(":input"))
}
