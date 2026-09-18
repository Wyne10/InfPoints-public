plugins {
    id("java-library")
    id("com.vanniktech.maven.publish") version "0.35.0"
}

dependencies {
    compileOnly(libs.paperApi)
}

java {
    withSourcesJar()
    toolchain.languageVersion.set(JavaLanguageVersion.of(16))
}

tasks.named("publish") {
    dependsOn("publishToMavenLocal")
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates(findProperty("centralGroup").toString(), "infpoints-api", version.toString())

    pom {
        name.set("InfPoints API")
        description.set("Consumer API for the InfPoints Bukkit/Paper plugin, a point and currency system with exact fixed-point balances, a transaction ledger, and synchronous and asynchronous access.")
        inceptionYear.set("2025")
        url.set("https://github.com/Wyne10/InfPoints-public")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id.set("Wyne10")
                name.set("Wyne")
                email.set("izmodenov1997@gmail.com")
                organization.set("BigTeam")
                organizationUrl.set("https://github.com/NeverMined-Entertainment")
            }
        }
        scm {
            url.set("https://github.com/Wyne10/InfPoints-public")
            connection.set("scm:git:git://github.com/Wyne10/InfPoints-public.git")
            developerConnection.set("scm:git:ssh://git@github.com/Wyne10/InfPoints-public.git")
        }
    }
}
