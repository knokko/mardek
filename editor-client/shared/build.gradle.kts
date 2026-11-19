plugins {
    id("compose-repositories")
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvm()
    
    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-datetime-jvm:0.7.1")
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            api(project(":editor-protocol"))
            implementation(project(":client-export-helper"))
            implementation(project(":bitser-kwik"))
            implementation(project(":vk2d"))
            implementation(project(":zstd-streams"))
            implementation(project(":state"))
            implementation(project(":input"))
            implementation(project(":game"))
            implementation(libs.vk.boiler)
            implementation(libs.memory.query)
            implementation(libs.kwik)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
