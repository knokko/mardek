plugins {
	alias(libs.plugins.kotlinJvm)
}

tasks.register("randomIDs", JavaExec::class) {
	group = "Miscellaneous"
	description = "Print some random UUIDs"
	classpath(sourceSets.main.get().runtimeClasspath)
	mainClass = "mardek.miscellaneous.RandomIDsKt"
}

tasks.register("checkIntegrationTestScreenshots", JavaExec::class) {
	group = "Miscellaneous"
	description = "GitHub Actions job to check whether the file names in game/rendering-test-results match"
	classpath(sourceSets.main.get().runtimeClasspath)
	mainClass = "mardek.miscellaneous.CheckIntegrationTestScreenshotsKt"
}
