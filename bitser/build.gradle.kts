plugins {
	java
	id("knokko-reposilite")
}

java {
	withSourcesJar()
}

repositories {
	mavenCentral()
}

dependencies {
	testImplementation("com.github.knokko:sample-profiler:1.0.0")
	testImplementation(platform("org.junit:junit-bom:5.12.2"))
	testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<Test>("test") {
	useJUnitPlatform()
}
