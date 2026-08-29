buildscript {
	repositories {
		mavenCentral()
	}

	dependencies {
		classpath("com.guardsquare:proguard-gradle:7.8.2")
	}
}

plugins {
	java
	alias(libs.plugins.kotlinJvm) apply false
	alias(libs.plugins.kotlinMultiplatform) apply false
	alias(libs.plugins.composeMultiplatform) apply false
	alias(libs.plugins.composeCompiler) apply false
}
