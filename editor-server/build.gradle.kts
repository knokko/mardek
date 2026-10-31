plugins {
	java
	id("knokko-reposilite")
	alias(libs.plugins.kotlinJvm)
	alias(libs.plugins.shadow)
}

dependencies {
	implementation(project(":editor-protocol"))
	implementation(project(":bitser-kwik"))
}

tasks.shadowJar {
	manifest {
		attributes["Main-Class"] = "mardek.editor.server.EditorServerKt"
		attributes["Enable-Native-Access"] = "ALL-UNNAMED"
		attributes["Add-Exports"] = "java.base/jdk.internal.misc"
	}
}

tasks.register("runEditorServer", JavaExec::class) {
	group = "Editor"
	description = "Runs the editor server"
	classpath(sourceSets.main.get().runtimeClasspath)
	mainClass = "mardek.editor.server.EditorServerKt"
	standardInput = System.`in`
}
