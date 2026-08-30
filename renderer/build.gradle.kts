plugins {
	alias(libs.plugins.kotlinJvm)
}

dependencies {
	implementation("org.jetbrains.kotlinx:kotlinx-datetime-jvm:0.7.1")
	for (dependency in (project.ext.get("vulkanImplementationDependencies") as ArrayList<*>)) {
		implementation(dependency)
	}
	for (dependency in (project.ext.get("vulkanRuntimeDependencies") as ArrayList<*>)) {
		runtimeOnly(dependency)
	}
	implementation(project(":content"))
	implementation(project(":state"))
	implementation(project(":vk2d"))
}
