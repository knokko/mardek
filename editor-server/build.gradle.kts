plugins {
	java
	id("knokko-reposilite")
	alias(libs.plugins.kotlinJvm)
}

dependencies {
	implementation(project(":editor-protocol"))
	implementation(project(":bitser-kwik"))
}

tasks.register("dummy-editor-server1", JavaExec::class) {
	group = "Editor"
	description = "Runs dummy editor server 1"
	classpath(sourceSets.main.get().runtimeClasspath)
	mainClass = "mardek.editor.server.dummy1.DummyEditorServer1Kt"
	standardInput = System.`in`
}
