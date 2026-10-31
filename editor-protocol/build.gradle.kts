plugins {
	id("java-library")
	id("knokko-reposilite")
	alias(libs.plugins.kotlinJvm)
}

dependencies {
	api(project(":bitser"))
	api(project(":content"))
	api(project(":export-helper"))
	api(libs.kwik)
	api(libs.vk.boiler)
}
