import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

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
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")

    implementation(project(":api"))
    implementation(libs.guice)
    implementation(libs.ormLiteJdbc)
    implementation(libs.hikari)
    implementation(libs.adventureMini)
    implementation(libs.adventureBukkit)

    implementation(libs.wutilsConfig)
    implementation(libs.wutilsLog)
    implementation(libs.wutilsI18n)
    implementation(libs.wutilsJdbc)
    implementation(libs.wutilsJson)
    implementation(libs.wutilsCommon)
}

tasks {
    shadowJar {
        archiveBaseName.set(findProperty("name").toString())
        archiveClassifier.set("")
        minimize()
        relocate("com.google.inject", "org.bigcraft.infpoints.shadow.google.guice")
        relocate("com.google.common", "org.bigcraft.infpoints.shadow.google.common")
        relocate("com.j256.ormlite", "org.bigcraft.infpoints.shadow.j256.ormlite")
        relocate("com.zaxxer.hikari", "org.bigcraft.infpoints.shadow.zaxxer.hikari")
        relocate("net.kyori", "org.bigcraft.infpoints.shadow.net.kyori")
        relocate("me.wyne.wutils", "org.bigcraft.infpoints.shadow.wutils")
    }

    runServer {
        downloadPlugins {
            url("https://ci.extendedclip.com/view/Plugins/job/PlaceholderAPI/197/artifact/build/libs/PlaceholderAPI-2.11.6.jar")
            url("https://download.luckperms.net/1604/bukkit/loader/LuckPerms-Bukkit-5.5.15.jar")
            github("dmulloy2", "ProtocolLib", "5.4.0", "ProtocolLib.jar")
            github("ViaVersion", "ViaVersion", "5.4.2", "ViaVersion-5.4.2.jar")
            github("ViaVersion", "ViaBackwards", "5.4.2", "ViaBackwards-5.4.2.jar")
            github("CommandAPI", "CommandAPI", "9.7.0", "CommandAPI-9.7.0.jar")
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
    softDepend = listOf("PlaceholderAPI", "CommandAPI", "Vault")
    permissions {
        register("points.balance.*") {
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("points.balance-other.*") {
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("points.set.*") {
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("points.add.*") {
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("points.sub.*") {
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("points.pay.*") {
            default = BukkitPluginDescription.Permission.Default.OP
        }

        register("points.admin.*") {
            children = listOf("points.admin.reload")
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("points.admin.reload") {
            description = "Allows to reload plugin"
        }
    }
}