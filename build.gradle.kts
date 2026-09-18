import net.minecrell.pluginyml.bukkit.BukkitPluginDescription
import org.codehaus.plexus.util.Os

plugins {
    id("java")
    alias(libs.plugins.shadow)
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
    compileOnly(libs.vaultApi)
    compileOnly(libs.connectionSource)

    implementation(project(":api"))
    implementation(libs.guice)
    implementation(libs.enhancedLegacy)
    implementation(libs.adventureMini)
    implementation(libs.adventureBukkit)
    implementation(libs.adventurePlain)

    implementation(libs.wutilsConfig)
    implementation(libs.wutilsConfigurables)
    implementation(libs.wutilsI18n)
    implementation(libs.wutilsCommon)

    testImplementation(libs.paperApi)
    testImplementation(libs.ormLiteJdbc)
    testImplementation(libs.h2)
    testImplementation(platform(libs.junitBom))
    testImplementation(libs.junitJupiter)
    testRuntimeOnly(libs.junitPlatformLauncher)
}

tasks {
    val isDebug = findProperty("debug")?.toString()?.toBoolean() ?: false

    shadowJar {
        archiveBaseName.set(findProperty("name").toString())
        archiveClassifier.set("")
        minimize()
        if (!isDebug) {
            relocate("com.google.inject", "me.wyne.infpoints.shadow.google.guice")
            relocate("com.google.common", "me.wyne.infpoints.shadow.google.common")
            relocate("net.kyori", "me.wyne.infpoints.shadow.net.kyori")
            relocate("dev.vankka", "me.wyne.infpoints.shadow.dev.vankka")
            relocate("me.wyne.wutils", "me.wyne.infpoints.shadow.wutils")
        }
    }

    runServer {
        val minecraftVersion: String = if (Os.isFamily(Os.FAMILY_WINDOWS) || isDebug) "1.19.4" else "1.16.5"
        val viaVersion = "5.11.0"
        val commandApiVersion = "9.4.2"
        downloadPlugins {
            url("https://download.luckperms.net/1652/bukkit/loader/LuckPerms-Bukkit-5.5.65.jar")
            github("PlaceholderAPI", "PlaceholderAPI", "2.12.2", "PlaceholderAPI-2.12.2.jar")
            github("MilkBowl", "Vault", "1.7.3", "Vault.jar")
            github("ViaVersion", "ViaVersion", viaVersion, "ViaVersion-$viaVersion.jar")
            github("ViaVersion", "ViaBackwards", viaVersion, "ViaBackwards-$viaVersion.jar")
            github("CommandAPI", "CommandAPI", commandApiVersion, "CommandAPI-$commandApiVersion.jar")
            github("Wyne10", "ConnectionSource-public", "2.0.0", "ConnectionSource-2.0.0.jar")
        }
        minecraftVersion(minecraftVersion)
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
    }

    test {
        useJUnitPlatform()
    }
}

tasks.withType(xyz.jpenilla.runtask.task.AbstractRun::class) {
    javaLauncher = javaToolchains.launcherFor {
        vendor = JvmVendorSpec.JETBRAINS
        languageVersion = JavaLanguageVersion.of(21)
    }
    jvmArgs("-XX:+AllowEnhancedClassRedefinition", "-DPaper.IgnoreJavaVersion=true")
}

bukkit {
    name = findProperty("name").toString()
    version = getVersion().toString()
    website = findProperty("website").toString()
    author = findProperty("author").toString()
    main = "me.wyne.infpoints.InfPoints"
    apiVersion = "1.16"
    softDepend = listOf("PlaceholderAPI", "CommandAPI", "Vault", "ConnectionSource")
    permissions {
        register("points.admin.*") {
            children = listOf("points.admin.reload", "points.admin.audit")
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("points.admin.reload") {
            description = "Allows to reload plugin"
        }
        register("points.admin.audit") {
            description = "Allows to compare stored balances with the transaction history"
        }
        register("points.balance.*") {
            description = "Allows to view own balance of every point"
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("points.balance-other.*") {
            description = "Allows to view balances of other players of every point"
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("points.set.*") {
            description = "Allows to set balances of every point"
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("points.add.*") {
            description = "Allows to add to and deliver balances of every point"
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("points.sub.*") {
            description = "Allows to subtract from balances of every point"
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("points.pay.*") {
            description = "Allows to pay other players with every point"
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("points.exchange.*") {
            description = "Allows to exchange every point for a console command"
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("points.history.*") {
            description = "Allows to view own transaction history of every point"
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("points.history-other.*") {
            description = "Allows to view transaction history of other players of every point"
            default = BukkitPluginDescription.Permission.Default.OP
        }
    }
}
