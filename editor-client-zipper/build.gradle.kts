plugins {
	java
	id("knokko-reposilite")
	alias(libs.plugins.kotlinJvm)
	alias(libs.plugins.shadow)
}

dependencies {
	implementation(project(":editor-protocol"))
}

tasks.register("zipEditorClient", JavaExec::class) {
	group = "Editor"
	description = "Creates a build (ZIP) for the editor client, to be sent to users"
	classpath(sourceSets.main.get().runtimeClasspath)
	mainClass = "mardek.editor.client.zip.EditorClientZipperKt"
	dependsOn(":game:proguard")
	dependsOn(":editor-client:desktopApp:shadowJar")
}
