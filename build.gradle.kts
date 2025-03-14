plugins {
    id("java")
    alias(libs.plugins.shadow)
    alias(libs.plugins.lombok)
    alias(libs.plugins.runPaper)
    alias(libs.plugins.pluginYml)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(16))
}

dependencies {
    compileOnly(libs.paperApi)
    compileOnly(libs.placeholderApi)
    compileOnly(libs.commandApi)

    implementation(libs.guice)

    implementation(libs.wutilsConfig)
    implementation(libs.wutilsConfigugrables)
    implementation(libs.wutilsLog)
    implementation(libs.wutilsI18n)
    implementation(libs.wutilsJdbc)
    implementation(libs.wutilsJson)
}

tasks {
    shadowJar {
        archiveBaseName.set(findProperty("name").toString())
        archiveClassifier.set("")
        minimize()
        relocate("com.google.inject", "org.bigcraft.infpoints.shadow.google.guice")
        relocate("com.google.common", "org.bigcraft.infpoints.shadow.google.common")
        relocate("me.wyne.wutils", "org.bigcraft.infpoints.shadow.wutils")
    }

    runServer {
        downloadPlugins {
            url("https://ci.extendedclip.com/view/Plugins/job/PlaceholderAPI/197/artifact/build/libs/PlaceholderAPI-2.11.6.jar")
            url("https://download.luckperms.net/1571/bukkit/loader/LuckPerms-Bukkit-5.4.154.jar")
            url("https://ci.dmulloy2.net/job/ProtocolLib/lastSuccessfulBuild/artifact/build/libs/ProtocolLib.jar")
            github("ViaVersion", "ViaVersion", "5.2.1", "ViaVersion-5.2.1.jar")
            github("ViaVersion", "ViaBackwards", "5.2.1", "ViaBackwards-5.2.1.jar")
            github("CommandAPI", "CommandAPI", "9.7.0", "CommandAPI-9.7.0.jar")
            github("DecentSoftware-eu", "DecentHolograms", "2.8.15", "DecentHolograms-2.8.15.jar")
        }
        minecraftVersion("1.21.3")
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
    }
}

tasks.withType(xyz.jpenilla.runtask.task.AbstractRun::class) {
    javaLauncher = javaToolchains.launcherFor {
        vendor = JvmVendorSpec.JETBRAINS
        languageVersion = JavaLanguageVersion.of(21)
    }
    jvmArgs("-XX:+AllowEnhancedClassRedefinition")
}

bukkit {
    name = findProperty("name").toString()
    version = getVersion().toString()
    website = findProperty("website").toString()
    author = findProperty("author").toString()
    main = "org.bigcraft.infpoints.InfPoints"
    apiVersion = "1.16"
    softDepend = listOf("PlaceholderAPI", "CommandAPI")
}