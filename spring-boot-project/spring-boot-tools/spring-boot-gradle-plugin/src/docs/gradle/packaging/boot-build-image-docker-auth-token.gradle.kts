import org.springframework.boot.gradle.tasks.bundling.BootJar
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
	java
	id("org.springframework.boot") version "{gradle-project-version}"
}

tasks.getByName<BootJar>("bootJar") {
	mainClass.set("com.example.ExampleApplication")
}

// tag::docker-auth-token[]
tasks.getByName<BootBuildImage>("bootBuildImage") {
	docker {
		builderRegistry {
			token = "9cbaf023786cd7..."
		}
	}
}
// end::docker-auth-token[]
