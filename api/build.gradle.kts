plugins {
    id("java")
    id("maven-publish")
}

java {
    withSourcesJar()
    toolchain.languageVersion.set(JavaLanguageVersion.of(16))
}

dependencies {
    compileOnly(libs.paperApi)
}

publishing {
    repositories {
        maven {
            url = uri(findProperty("myMavenRepoWriteUrl") ?: "")

            credentials {
                username = findProperty("myMavenRepoWriteUsername").toString()
                password = findProperty("myMavenRepoWritePassword").toString()
            }
        }
    }

    publications {
        create<MavenPublication>("maven") {
            groupId = findProperty("group").toString()
            artifactId = "InfPoints-api"
            version = findProperty("pluginVersion").toString()

            from(components["java"])
        }
    }
}