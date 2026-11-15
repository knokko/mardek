plugins {
	java
	id("knokko-reposilite")
	alias(libs.plugins.kotlinJvm)
	alias(libs.plugins.shadow)
}

sourceSets {
	register("zip") {}
}

dependencies {
	implementation(project(":editor-protocol"))
	implementation(project(":bitser-kwik"))
	implementation(project(":zstd-streams"))
	implementation(libs.memory.query)
	implementation(libs.kwik)
	implementation(project(":content"))
	add("zipImplementation", project(":editor-protocol"))
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
	jvmArgs("-XX:+UseZGC")
	jvmArgs("-XX:ZUncommitDelay=1")
	jvmArgs("-XX:SoftMaxHeapSize=500m")
}

tasks.register("zipEditorServer", JavaExec::class) {
	group = "Editor"
	description = "Creates a build (ZIP) for the editor server, ready to be transferred to a VPS"
	classpath(sourceSets.getByName("zip").runtimeClasspath)
	mainClass = "mardek.editor.server.zip.EditorServerZipperKt"
	dependsOn("shadowJar")
	dependsOn(":game:proguard")
	dependsOn(":editor-client:desktopApp:shadowJar")
}
