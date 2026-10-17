plugins {
	java
	id("knokko-reposilite")
	alias(libs.plugins.kotlinJvm)
}

dependencies {
	implementation(project(":editor-protocol"))
	implementation(project(":bitser-kwik"))
}

tasks.register("runEditorServer", JavaExec::class) {
	group = "Editor"
	description = "Runs the editor server"
	classpath(sourceSets.main.get().runtimeClasspath)
	mainClass = "mardek.editor.server.EditorServerKt"
	standardInput = System.`in`
}
