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
            url = uri("https://git.bigteam.pw/api/v4/projects/11/packages/maven")
            credentials(HttpHeaderCredentials::class) {
                name = "Deploy-Token"
                value = findProperty("gitLabPrivateToken") as String?
            }
            authentication {
                create("header", HttpHeaderAuthentication::class)
            }
        }
    }

    publications {
        create<MavenPublication>("maven") {
            groupId = findProperty("group").toString()
            artifactId = "InfPoints-api"
            version = findProperty("version").toString()

            from(components["java"])
        }
    }
}