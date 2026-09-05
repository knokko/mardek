import java.net.URI

repositories {
	maven {
		name = "knokko-reposilite"
		url = URI("https://49.12.188.159:8080/releases/")
		content {
			includeGroup("com.github.knokko")
			includeGroup("com.github.knokko.vk-compressor")
		}
	}
}
