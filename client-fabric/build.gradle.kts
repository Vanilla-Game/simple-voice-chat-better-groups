import java.util.Properties

import net.fabricmc.loom.task.prod.ClientProductionRunTask
import org.gradle.jvm.tasks.Jar

plugins {
    id("net.fabricmc.fabric-loom") version "1.17.19"
}

group = rootProject.group
version = rootProject.version

val compatibility = Properties().apply {
    file("compatibility.properties").inputStream().use { load(it) }
}
val minecraftVersion: String = compatibility.getProperty("minecraft")
val fabricApiVersion: String = compatibility.getProperty("fabric-api")
val voicechatVersion: String = providers.gradleProperty("voicechatVersion")
    .getOrElse(compatibility.getProperty("voicechat.compile"))

// CI-only test flavor: relaxes the voicechat dependency range so Fabric Loader
// does not reject a candidate Simple Voice Chat version before mixins are
// verified, and renames the jar so the flavor can never ship by accident.
val compatCheck = providers.gradleProperty("voicechatCompatCheck").isPresent
val voicechatRange: String =
    if (compatCheck) "*" else compatibility.getProperty("voicechat.range")
val fabricApiDependency = "net.fabricmc.fabric-api:fabric-api:$fabricApiVersion"
val voicechatDependency = "maven.modrinth:simple-voice-chat:$voicechatVersion"

base {
    archivesName =
        if (compatCheck) "svc-better-groups-fabric-compat-test"
        else "svc-better-groups-fabric"
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
    maven("https://api.modrinth.com/maven") {
        content {
            includeGroup("maven.modrinth")
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")

    implementation("net.fabricmc:fabric-loader:0.19.3")
    implementation(fabricApiDependency)
    compileOnly(voicechatDependency)
    runtimeOnly(voicechatDependency)

    // ClientProductionRunTask launches packaged mods rather than the
    // development runtime classpath, so its runtime mods must be explicit.
    add("productionRuntimeMods", fabricApiDependency)
    add("productionRuntimeMods", voicechatDependency)
}

fabricApi {
    configureTests {
        createSourceSet.set(true)
        modId.set("svc_better_groups_client_test")
        enableGameTests.set(false)
        enableClientGameTests.set(true)
        eula.set(true)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 25
}

val clientVersion = version.toString()

tasks.processResources {
    inputs.property("version", clientVersion)
    inputs.property("minecraftVersion", minecraftVersion)
    inputs.property("fabricApiVersion", fabricApiVersion)
    inputs.property("voicechatRange", voicechatRange)
    filesMatching("fabric.mod.json") {
        expand(
            "version" to clientVersion,
            "minecraft_version" to minecraftVersion,
            "fabric_api_version" to fabricApiVersion.substringBefore('+'),
            "voicechat_range" to voicechatRange
        )
    }
}

val gametestJar = tasks.register<Jar>("gametestJar") {
    archiveClassifier.set("gametest")
    from(sourceSets.named("gametest").map { it.output })
}

tasks.register<ClientProductionRunTask>("runProductionClientGameTest") {
    group = "verification"
    description = "Runs the packaged client mod's Simple Voice Chat compatibility game test"
    mods.from(gametestJar)
    useXVFB.set(true)
    jvmArgs.add("-Dfabric.client.gametest")
    jvmArgs.add("-Dfabric.client.gametest.disableNetworkSynchronizer=true")
}
