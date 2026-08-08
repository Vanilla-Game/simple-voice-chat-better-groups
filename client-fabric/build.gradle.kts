import java.util.Properties

plugins {
    id("net.fabricmc.fabric-loom") version "1.17.19"
}

group = rootProject.group
version = rootProject.version

val compatibility = Properties().apply {
    rootProject.file("compatibility.properties").inputStream().use { load(it) }
}
val minecraftVersion: String = compatibility.getProperty("minecraft")
val voicechatVersion: String = providers.gradleProperty("voicechatVersion")
    .getOrElse(compatibility.getProperty("voicechat.compile"))

// CI-only test flavor: relaxes the voicechat dependency range so Fabric Loader
// does not reject a candidate Simple Voice Chat version before mixins are
// verified, and renames the jar so the flavor can never ship by accident.
val compatCheck = providers.gradleProperty("voicechatCompatCheck").isPresent
val voicechatRange: String =
    if (compatCheck) "*" else compatibility.getProperty("voicechat.range")

base {
    archivesName =
        if (compatCheck) "voicechat-group-tools-fabric-compat-test"
        else "voicechat-group-tools-fabric"
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
    compileOnly("maven.modrinth:simple-voice-chat:$voicechatVersion")
    runtimeOnly("maven.modrinth:simple-voice-chat:$voicechatVersion")
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
    inputs.property("voicechatRange", voicechatRange)
    filesMatching("fabric.mod.json") {
        expand(
            "version" to clientVersion,
            "minecraft_version" to minecraftVersion,
            "voicechat_range" to voicechatRange
        )
    }
}

if (compatCheck) {
    loom.runs.named("client") {
        systemProperties.put("voicechat_group_tools.compat_check", "true")
    }
}
