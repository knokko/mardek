import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("compose-repositories")
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":editor-client:shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

//compose.desktop {
//    application {
//        mainClass = "mardek.editor.client.EditorClientMainKt"
//
//        nativeDistributions {
//            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
//            packageName = "mardek.editor.client"
//            packageVersion = "1.0.0"
//        }
//    }
//}
