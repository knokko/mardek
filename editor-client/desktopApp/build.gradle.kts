plugins {
    id("compose-repositories")
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":editor-client:shared"))

    implementation(libs.vk.boiler)
    implementation(libs.memory.query)
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

tasks.register("runEditorClient", JavaExec::class) {
    group = "Editor"
    description = "Runs the editor client"
    classpath(sourceSets.main.get().runtimeClasspath)
    mainClass = "mardek.editor.client.EditorClientMainKt"
    standardInput = System.`in`
    jvmArgs("-XX:+UseZGC")
    jvmArgs("-XX:ZUncommitDelay=1")
    jvmArgs("-XX:SoftMaxHeapSize=500m")
}

tasks.test {
    failOnNoDiscoveredTests = false
}

tasks.shadowJar {
    exclude("windows/**")
    exclude("linux/**")
    exclude("macos/**")
    exclude("freebsd/**")
    exclude("org/lwjgl/vulkan/**")
    exclude("org/lwjgl/system/**")
    exclude("org/joml/**")
    exclude("mardek/audio/**")
    exclude("mardek/content/**")
    exclude("mardek/game/**")
    exclude("mardek/input/**")
    exclude("mardek/renderer/**")
    exclude("mardek/state/**")
    exclude("com/github/knokko/boiler/**")
    manifest {
        attributes["Main-Class"] = "mardek.editor.client.EditorClientMainKt"
        attributes["Enable-Native-Access"] = "ALL-UNNAMED"
        attributes["Add-Exports"] = "java.base/jdk.internal.misc"
        attributes["Class-Path"] = "../game/build/libs/mardek.jar"
    }
}
