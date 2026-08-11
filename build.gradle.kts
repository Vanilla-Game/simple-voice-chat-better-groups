@file:Suppress("UNCHECKED_CAST")

import groovy.json.JsonSlurper
import java.util.zip.ZipFile
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Sync

plugins {
    java
}

group = "ru.vanillagame.voicechat"
version = "0.8.0" // x-release-please-version

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
    maven("https://maven.maxhenkel.de/repository/public") {
        name = "maxhenkel"
    }
}

fun readCompatibilityCatalog(): Map<String, Any?> =
    JsonSlurper().parse(layout.projectDirectory.file("compatibility.json").asFile) as Map<String, Any?>

val compatibilityCatalog = readCompatibilityCatalog()
val serverCompatibility = compatibilityCatalog.getValue("server") as Map<String, Any?>
val serverCompile = serverCompatibility.getValue("compile") as Map<String, Any?>
val paperApiVersion = serverCompile.getValue("paperApi") as String
val voicechatApiVersion = serverCompile.getValue("voicechatApi") as String
val catalogJavaVersion = (compatibilityCatalog.getValue("java") as Number).toInt()

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
    compileOnly("de.maxhenkel.voicechat:voicechat-api:$voicechatApiVersion")

    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.papermc.paper:paper-api:$paperApiVersion")
    testImplementation("de.maxhenkel.voicechat:voicechat-api:$voicechatApiVersion")
    testImplementation("org.mockito:mockito-core:5.23.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(catalogJavaVersion)
    sourceCompatibility = JavaVersion.toVersion(catalogJavaVersion)
    targetCompatibility = JavaVersion.toVersion(catalogJavaVersion)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = catalogJavaVersion
}

val pluginVersion = version.toString()

tasks.processResources {
    inputs.property("version", pluginVersion)
    filesMatching("plugin.yml") {
        expand("version" to pluginVersion)
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    archiveBaseName = "svc-better-groups"
}

val validateCompatibilityCatalog = tasks.register("validateCompatibilityCatalog") {
    group = "verification"
    description = "Validates the compatibility catalog and its release matrix"
    inputs.file(layout.projectDirectory.file("compatibility.json"))

    doLast {
        fun svcCore(artifact: String, loader: String): List<Int> {
            check(artifact.startsWith("$loader-")) { "$artifact is not a $loader Simple Voice Chat artifact" }
            val core = artifact.removePrefix("$loader-").substringAfterLast('-').substringBefore('+')
            val parts = core.split('.').map(String::toInt)
            check(parts.size == 3) { "$artifact does not contain a semantic 3-part SVC version" }
            return parts
        }

        fun checkContiguousPatches(artifacts: List<String>, loader: String, label: String): List<Int> {
            check(artifacts.distinct().size == artifacts.size) { "$label contains duplicate SVC artifacts" }
            val cores = artifacts.map { svcCore(it, loader) }
            val minor = cores.first().take(2)
            check(cores.all { it.take(2) == minor }) { "$label crosses an SVC minor-version boundary" }
            val patches = cores.map { it[2] }.distinct().sorted()
            check(patches == (patches.first()..patches.last()).toList()) {
                "$label contains a gap in its SVC patch range: $patches"
            }
            return listOf(minor[0], minor[1], patches.first(), patches.last())
        }

        check(compatibilityCatalog["schemaVersion"] == 1) { "Unsupported compatibility.json schemaVersion" }
        check(catalogJavaVersion == 25) { "The server baseline must use Java 25" }

        val serverTargets = serverCompatibility.getValue("targets") as List<Map<String, Any?>>
        check(serverTargets.map { it.getValue("minecraft") } == listOf("26.1.2", "26.2")) {
            "Server targets must be exactly 26.1.2 and 26.2"
        }
        check(serverTargets.sumOf { (it.getValue("voicechatArtifacts") as List<*>).size } >= 9) {
            "The server matrix must retain at least the 9 baseline Paper/SVC combinations"
        }
        serverTargets.forEach { target ->
            val artifacts = target.getValue("voicechatArtifacts") as List<String>
            check(artifacts.isNotEmpty()) {
                "Server target ${target["minecraft"]} has no SVC artifacts"
            }
            checkContiguousPatches(artifacts, "bukkit", "Server ${target["minecraft"]}")
            check((target.getValue("paperBuild") as Number).toInt() > 0)
            check((target.getValue("leafBuild") as Number).toInt() > 0)
        }

        val fabric = compatibilityCatalog.getValue("fabric") as Map<String, Any?>
        val fabricTargets = fabric.getValue("targets") as List<Map<String, Any?>>
        check(fabricTargets.map { it.getValue("id") } == listOf("1.21.11", "26.1", "26.2")) {
            "Fabric targets must be exactly 1.21.11, 26.1, and 26.2"
        }
        val expectedRows = mapOf("1.21.11" to 17, "26.1" to 27, "26.2" to 5)
        fabricTargets.forEach { target ->
            val id = target.getValue("id") as String
            val rows = target.getValue("compatibility") as List<Map<String, String>>
            val releaseMinecraftVersions = target.getValue("releaseMinecraftVersions") as List<String>
            check(rows.size >= expectedRows.getValue(id)) {
                "Fabric $id must retain at least ${expectedRows.getValue(id)} compatibility rows, found ${rows.size}"
            }
            check(rows.distinctBy { it.getValue("minecraft") to it.getValue("voicechatArtifact") }.size == rows.size) {
                "Fabric $id contains duplicate Minecraft/SVC rows"
            }
            val compile = target.getValue("compile") as Map<String, String>
            val targetJava = (target.getValue("java") as Number).toInt()
            val obfuscated = target.getValue("obfuscated") as Boolean
            val adapterSet = target.getValue("adapterSet") as String
            check(adapterSet.matches(Regex("[a-z0-9_]+"))) {
                "Fabric $id has an invalid adapterSet name: $adapterSet"
            }
            check(file("client-fabric/src/adapters/$adapterSet/java").isDirectory) {
                "Fabric $id references missing adapterSet $adapterSet"
            }
            check(targetJava == if (obfuscated) 21 else 25) {
                "Fabric $id has an invalid Java baseline"
            }
            check(rows.any { it.getValue("voicechatArtifact") == compile.getValue("voicechatArtifact") }) {
                "Fabric $id compile SVC artifact is not covered by the compatibility matrix"
            }
            check(rows.all { it.getValue("minecraft") in releaseMinecraftVersions }) {
                "Fabric $id contains a runtime outside releaseMinecraftVersions"
            }
            val bounds = checkContiguousPatches(
                rows.map { it.getValue("voicechatArtifact") }.distinct(),
                "fabric",
                "Fabric $id"
            )
            val rangeMatch = Regex(">=(?:\\d+\\.\\d+\\.\\d+-)?(\\d+)\\.(\\d+)\\.(\\d+) <(?:\\d+\\.\\d+\\.\\d+-)?(\\d+)\\.(\\d+)\\.(\\d+)")
                .matchEntire(target.getValue("voicechatRange") as String)
            check(rangeMatch != null) {
                "Fabric $id must stay within a bounded SVC 2.6.x range"
            }
            val range = rangeMatch!!.groupValues.drop(1).map(String::toInt)
            check(range.take(3) == bounds.take(3)) {
                "Fabric $id range lower bound does not match its first tested SVC patch"
            }
            check(range.drop(3) == listOf(bounds[0], bounds[1], bounds[3] + 1)) {
                "Fabric $id range upper bound must follow its last contiguous tested SVC patch"
            }
        }
        check(fabricTargets.sumOf { (it.getValue("compatibility") as List<*>).size } >= 49) {
            "The Fabric matrix must retain at least the 49 baseline runtime combinations"
        }
    }
}

val serverJar = tasks.named<Jar>("jar")
val fabricTargets = (compatibilityCatalog.getValue("fabric") as Map<String, Any?>)
    .getValue("targets") as List<Map<String, Any?>>
val clientJars = fabricTargets.associate { target ->
    val id = target.getValue("id") as String
    val archiveBaseName = target.getValue("archiveBaseName") as String
    id to project(target.getValue("projectPath") as String).layout.buildDirectory.file(
        "libs/$archiveBaseName-$pluginVersion.jar"
    )
}

val stageReleaseArtifacts = tasks.register<Sync>("stageReleaseArtifacts") {
    group = "build"
    description = "Stages and verifies all publishable Better Groups artifacts"
    dependsOn(validateCompatibilityCatalog)
    dependsOn(fabricTargets.map { target ->
        val task = if (target.getValue("obfuscated") as Boolean) "remapJar" else "jar"
        "${target.getValue("projectPath")}:$task"
    })
    duplicatesStrategy = DuplicatesStrategy.FAIL
    into(layout.buildDirectory.dir("release"))
    from(serverJar.flatMap { it.archiveFile })
    clientJars.values.forEach { from(it) }

    doLast {
        val staged = destinationDir.listFiles { file -> file.extension == "jar" }?.sortedBy { it.name }.orEmpty()
        val expectedNames = (listOf("svc-better-groups-$pluginVersion.jar") +
            fabricTargets.map { "${it.getValue("archiveBaseName")}-$pluginVersion.jar" }).sorted()
        check(staged.map { it.name } == expectedNames) {
            "Expected exactly $expectedNames in ${destinationDir}, found ${staged.map { it.name }}"
        }

        fun zipText(jar: java.io.File, entryName: String): String = ZipFile(jar).use { zip ->
            val entry = requireNotNull(zip.getEntry(entryName)) { "${jar.name} is missing $entryName" }
            zip.getInputStream(entry).bufferedReader().use { it.readText() }
        }

        val serverMetadata = zipText(staged.single { it.name.startsWith("svc-better-groups-$pluginVersion") }, "plugin.yml")
        check("api-version: \"26.1.2\"" in serverMetadata) { "Server plugin.yml has the wrong api-version" }

        fabricTargets.forEach { target ->
            val id = target.getValue("id") as String
            val compile = target.getValue("compile") as Map<String, String>
            val targetJava = (target.getValue("java") as Number).toInt()
            val jar = staged.single { it.name.startsWith("svc-better-groups-fabric-$id-") }
            val metadata = JsonSlurper().parseText(zipText(jar, "fabric.mod.json")) as Map<*, *>
            val depends = metadata["depends"] as Map<*, *>
            check(depends["minecraft"] == target.getValue("minecraftDependency")) {
                "${jar.name} has the wrong Minecraft dependency"
            }
            check(depends["fabricloader"] == ">=${compile.getValue("fabricLoader")}") {
                "${jar.name} has the wrong Fabric Loader dependency"
            }
            check(depends["fabric-api"] == ">=${compile.getValue("fabricApi").substringBefore('+')}") {
                "${jar.name} has the wrong Fabric API dependency"
            }
            check(depends["voicechat"] == target.getValue("voicechatRange")) {
                "${jar.name} has the wrong Simple Voice Chat dependency"
            }
            check(depends["java"] == ">=$targetJava") { "${jar.name} has the wrong Java dependency" }
            check(metadata["id"] == "svc_better_groups_client") { "${jar.name} has the wrong mod id" }
            ZipFile(jar).use { zip ->
                check(zip.entries().asSequence().none { it.name.contains("/test/") || it.name.contains("gametest", true) }) {
                    "${jar.name} contains compatibility test classes or resources"
                }
            }
        }
    }
}

tasks.named("check") {
    dependsOn(validateCompatibilityCatalog)
}

tasks.named("build") {
    dependsOn(stageReleaseArtifacts)
}
