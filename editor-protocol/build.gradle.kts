plugins {
	id("java-library")
	id("knokko-reposilite")
	alias(libs.plugins.kotlinJvm)
}

dependencies {
	api(project(":bitser"))
	api(project(":content"))
	api(libs.kwik)
}
