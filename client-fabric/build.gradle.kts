plugins {
    id("net.fabricmc.fabric-loom") version "1.17.19"
}

group = rootProject.group
version = rootProject.version

base {
    archivesName = "voicechat-group-tools-fabric"
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
    minecraft("com.mojang:minecraft:26.2")

    implementation("net.fabricmc:fabric-loader:0.19.3")
    compileOnly("maven.modrinth:simple-voice-chat:fabric-2.6.21+26.2")
    runtimeOnly("maven.modrinth:simple-voice-chat:fabric-2.6.21+26.2")
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
    filesMatching("fabric.mod.json") {
        expand("version" to clientVersion)
    }
}
