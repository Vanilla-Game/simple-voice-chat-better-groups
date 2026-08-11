@file:Suppress("UNCHECKED_CAST")

import groovy.json.JsonSlurper
import java.util.zip.ZipFile
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.api.fabricapi.FabricApiExtension
import net.fabricmc.loom.task.RemapJarTask
import net.fabricmc.loom.task.prod.ClientProductionRunTask
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    base
    id("net.fabricmc.fabric-loom") version "1.17.19" apply false
    id("net.fabricmc.fabric-loom-remap") version "1.17.19" apply false
}

group = rootProject.group
version = rootProject.version

fun readCatalog(): Map<String, Any?> =
    JsonSlurper().parse(rootProject.file("compatibility.json")) as Map<String, Any?>

val catalog = readCatalog()
val catalogJavaVersion = (catalog.getValue("java") as Number).toInt()
val fabricCatalog = catalog.getValue("fabric") as Map<String, Any?>
val fabricTargets = (fabricCatalog.getValue("targets") as List<Map<String, Any?>>)
    .associateBy { it.getValue("id") as String }
val compatRunnerOnly = providers.gradleProperty("compatRunnerOnly")
    .map(String::toBoolean)
    .getOrElse(false)
val compatReleaseJar = providers.gradleProperty("compatReleaseJar")

require(!compatRunnerOnly || compatReleaseJar.isPresent) {
    "-PcompatRunnerOnly=true requires an explicit -PcompatReleaseJar"
}

fun Project.addFabricRepositories() {
    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/")
        maven("https://api.modrinth.com/maven") {
            content {
                includeGroup("maven.modrinth")
            }
        }
    }
}

fun Project.configureJava(targetVersion: Int) {
    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion = JavaLanguageVersion.of(targetVersion)
        sourceCompatibility = JavaVersion.toVersion(targetVersion)
        targetCompatibility = JavaVersion.toVersion(targetVersion)
    }
    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release = targetVersion
    }
}

fun Project.configureReleaseClient(target: Map<String, Any?>) {
    val obfuscated = target.getValue("obfuscated") as Boolean
    pluginManager.apply(
        if (obfuscated) "net.fabricmc.fabric-loom-remap"
        else "net.fabricmc.fabric-loom"
    )
    group = rootProject.group
    version = rootProject.version

    val targetId = target.getValue("id") as String
    val adapterSet = target.getValue("adapterSet") as String
    val clientVersion = version.toString()
    val compile = target.getValue("compile") as Map<String, String>
    val minecraftVersion = compile.getValue("minecraft")
    val loaderVersion = compile.getValue("fabricLoader")
    val fabricApiVersion = compile.getValue("fabricApi")
    val voicechatArtifact = compile.getValue("voicechatArtifact")
    val minecraftDependency = target.getValue("minecraftDependency") as String
    val voicechatRange = target.getValue("voicechatRange") as String
    val javaVersion = (target.getValue("java") as Number).toInt()
    val fabricApiDependency = "net.fabricmc.fabric-api:fabric-api:$fabricApiVersion"
    val voicechatDependency = "maven.modrinth:simple-voice-chat:$voicechatArtifact"
    val loom = extensions.getByType<LoomGradleExtensionAPI>()

    extensions.configure<BasePluginExtension> {
        archivesName.set(target.getValue("archiveBaseName") as String)
    }
    addFabricRepositories()

    dependencies {
        add("minecraft", "com.mojang:minecraft:$minecraftVersion")
        if (obfuscated) {
            add("mappings", loom.officialMojangMappings())
            add("modImplementation", "net.fabricmc:fabric-loader:$loaderVersion")
            add("modImplementation", fabricApiDependency)
            add("modCompileOnly", voicechatDependency)
            add("modRuntimeOnly", voicechatDependency)
        } else {
            add("implementation", "net.fabricmc:fabric-loader:$loaderVersion")
            add("implementation", fabricApiDependency)
            add("compileOnly", voicechatDependency)
            add("runtimeOnly", voicechatDependency)
        }
        add("productionRuntimeMods", fabricApiDependency)
        add("productionRuntimeMods", voicechatDependency)
    }

    extensions.configure<SourceSetContainer> {
        named("main") {
            java.setSrcDirs(listOf(
                rootProject.file("client-fabric/src/shared/java"),
                rootProject.file("client-fabric/src/adapters/$adapterSet/java")
            ))
            resources.setSrcDirs(listOf(rootProject.file("client-fabric/src/shared/resources")))
        }
    }
    configureJava(javaVersion)

    tasks.named<ProcessResources>("processResources") {
        inputs.property("version", clientVersion)
        inputs.property("minecraftDependency", minecraftDependency)
        inputs.property("loaderVersion", loaderVersion)
        inputs.property("fabricApiVersion", fabricApiVersion)
        inputs.property("voicechatRange", voicechatRange)
        inputs.property("javaVersion", javaVersion)
        filesMatching("fabric.mod.json") {
            expand(
                "version" to clientVersion,
                "minecraft_version" to minecraftDependency,
                "fabric_loader_version" to loaderVersion,
                "fabric_api_version" to fabricApiVersion.substringBefore('+'),
                "java_version" to javaVersion,
                "voicechat_range" to voicechatRange
            )
        }
        filesMatching("svc-better-groups-client.mixins.json") {
            expand("java_version" to javaVersion)
        }
    }

    val releaseJar = layout.buildDirectory.file(
        "libs/${target.getValue("archiveBaseName")}-$clientVersion.jar"
    )
    val probeMetadataDir = layout.buildDirectory.dir("generated/compatProbeMetadata")
    val generateCompatProbeMetadata = tasks.register("generateCompatProbeMetadata") {
        group = "verification"
        description = "Generates probe-only Fabric metadata with an unrestricted SVC dependency"
        inputs.file(rootProject.file("client-fabric/src/shared/resources/fabric.mod.json"))
        inputs.properties(
            mapOf(
                "version" to clientVersion,
                "minecraftDependency" to minecraftDependency,
                "loaderVersion" to loaderVersion,
                "fabricApiVersion" to fabricApiVersion,
                "javaVersion" to javaVersion,
                "targetId" to targetId
            )
        )
        outputs.file(probeMetadataDir.map { it.file("fabric.mod.json") })
        doLast {
            val output = probeMetadataDir.get().file("fabric.mod.json").asFile
            output.parentFile.mkdirs()
            output.writeText(
                rootProject.file("client-fabric/src/shared/resources/fabric.mod.json").readText()
                    .replace("\${version}", clientVersion)
                    .replace("\${minecraft_version}", minecraftDependency)
                    .replace("\${fabric_loader_version}", loaderVersion)
                    .replace("\${fabric_api_version}", fabricApiVersion.substringBefore('+'))
                    .replace("\${java_version}", javaVersion.toString())
                    .replace("\${voicechat_range}", "*")
            )
        }
    }

    tasks.register<Jar>("compatProbeJar") {
        group = "verification"
        description = "Repackages the release classes with probe-only SVC dependency metadata"
        dependsOn(if (obfuscated) "remapJar" else "jar", generateCompatProbeMetadata)
        archiveBaseName.set(target.getValue("archiveBaseName") as String)
        archiveClassifier.set("compat-probe")
        duplicatesStrategy = DuplicatesStrategy.FAIL
        if (!obfuscated) {
            manifest.attributes["Fabric-Mapping-Namespace"] = "official"
        }
        from(releaseJar.map { zipTree(it) }) {
            exclude("fabric.mod.json", "META-INF/MANIFEST.MF")
        }
        from(probeMetadataDir)

        doLast {
            val releaseEntries = ZipFile(releaseJar.get().asFile).use { zip ->
                zip.entries().asSequence().filterNot {
                    it.isDirectory || it.name == "fabric.mod.json" || it.name == "META-INF/MANIFEST.MF"
                }
                    .associate { entry -> entry.name to zip.getInputStream(entry).readBytes().contentHashCode() }
            }
            val probeEntries = ZipFile(archiveFile.get().asFile).use { zip ->
                zip.entries().asSequence().filterNot {
                    it.isDirectory || it.name == "fabric.mod.json" || it.name == "META-INF/MANIFEST.MF"
                }
                    .associate { entry -> entry.name to zip.getInputStream(entry).readBytes().contentHashCode() }
            }
            check(releaseEntries == probeEntries) { "Compatibility probe changed files other than fabric.mod.json" }
        }
    }
}

if (!compatRunnerOnly) {
    fabricTargets.values.forEach { target ->
        project(target.getValue("projectPath") as String).configureReleaseClient(target)
    }
}

project(":client-fabric:compat-runner") {
    val targetId = providers.gradleProperty("compatTarget").getOrElse("26.2")
    val target = requireNotNull(fabricTargets[targetId]) {
        "Unknown -PcompatTarget=$targetId; expected one of ${fabricTargets.keys}"
    }
    val obfuscated = target.getValue("obfuscated") as Boolean
    pluginManager.apply(
        if (obfuscated) "net.fabricmc.fabric-loom-remap"
        else "net.fabricmc.fabric-loom"
    )
    group = rootProject.group
    version = rootProject.version

    val runnerVersion = version.toString()
    val compile = target.getValue("compile") as Map<String, String>
    val minecraftVersion = providers.gradleProperty("compatMinecraftVersion")
        .getOrElse(compile.getValue("minecraft"))
    val voicechatArtifact = providers.gradleProperty("compatVoicechatArtifact")
        .getOrElse(compile.getValue("voicechatArtifact"))
    val loaderVersion = compile.getValue("fabricLoader")
    val fabricApiVersion = compile.getValue("fabricApi")
    val javaVersion = (target.getValue("java") as Number).toInt()
    val probe = providers.gradleProperty("compatProbe").map(String::toBoolean).getOrElse(false)
    val declaredRows = target.getValue("compatibility") as List<Map<String, String>>

    if (!probe) {
        require(declaredRows.any {
            it.getValue("minecraft") == minecraftVersion && it.getValue("voicechatArtifact") == voicechatArtifact
        }) {
            "Undeclared compatibility pair $minecraftVersion / $voicechatArtifact for target $targetId; " +
                "use -PcompatProbe=true only for discovery candidates"
        }
    } else {
        require(minecraftVersion in (target.getValue("releaseMinecraftVersions") as List<String>)) {
            "Probe Minecraft $minecraftVersion is outside target $targetId"
        }
    }

    addFabricRepositories()
    val loom = extensions.getByType<LoomGradleExtensionAPI>()
    dependencies {
        add("minecraft", "com.mojang:minecraft:$minecraftVersion")
        if (obfuscated) {
            add("mappings", loom.officialMojangMappings())
            add("modImplementation", "net.fabricmc:fabric-loader:$loaderVersion")
            add("modImplementation", "net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
            add("modCompileOnly", "maven.modrinth:simple-voice-chat:$voicechatArtifact")
            add("modRuntimeOnly", "maven.modrinth:simple-voice-chat:$voicechatArtifact")
        } else {
            add("implementation", "net.fabricmc:fabric-loader:$loaderVersion")
            add("implementation", "net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
            add("compileOnly", "maven.modrinth:simple-voice-chat:$voicechatArtifact")
            add("runtimeOnly", "maven.modrinth:simple-voice-chat:$voicechatArtifact")
        }
        add("productionRuntimeMods", "net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
        add("productionRuntimeMods", "maven.modrinth:simple-voice-chat:$voicechatArtifact")
    }

    extensions.configure<FabricApiExtension> {
        configureTests {
            createSourceSet.set(true)
            modId.set("svc_better_groups_client_test")
            enableGameTests.set(false)
            enableClientGameTests.set(true)
            eula.set(true)
        }
    }
    extensions.configure<SourceSetContainer> {
        named("gametest") {
            java.setSrcDirs(listOf(rootProject.file("client-fabric/src/gametest/java")))
            resources.setSrcDirs(listOf(rootProject.file("client-fabric/src/gametest/resources")))
        }
    }
    configureJava(javaVersion)

    tasks.named<ProcessResources>("processGametestResources") {
        inputs.property("fabricLoaderVersion", loaderVersion)
        filesMatching("fabric.mod.json") {
            expand("fabric_loader_version" to loaderVersion)
        }
    }

    val gametestJar = tasks.register<Jar>("gametestJar") {
        archiveClassifier.set("gametest")
        from(project.extensions.getByType<SourceSetContainer>().named("gametest").map { it.output })
    }
    val runtimeGametestJar = if (obfuscated) {
        val remapped = tasks.register<RemapJarTask>("remapGametestJar") {
            dependsOn(gametestJar)
            inputFile.set(gametestJar.flatMap { it.archiveFile })
            archiveClassifier.set("gametest-remapped")
        }
        remapped.flatMap { it.archiveFile }
    } else {
        gametestJar.flatMap { it.archiveFile }
    }

    val archiveBaseName = target.getValue("archiveBaseName") as String
    val selectedArchiveTask = when {
        probe -> "compatProbeJar"
        obfuscated -> "remapJar"
        else -> "jar"
    }
    val selectedClientPath = target.getValue("projectPath") as String
    val selectedArchiveTaskPath = "$selectedClientPath:$selectedArchiveTask"
    val addonFile = if (compatReleaseJar.isPresent) {
        layout.file(providers.provider { file(compatReleaseJar.get()) })
    } else {
        project(selectedClientPath).layout.buildDirectory.file(
            if (probe) "libs/$archiveBaseName-$runnerVersion-compat-probe.jar"
            else "libs/$archiveBaseName-$runnerVersion.jar"
        )
    }

    tasks.register<ClientProductionRunTask>("runProductionClientGameTest") {
        group = "verification"
        description = "Runs an exact packaged addon against one Minecraft/SVC compatibility pair"
        dependsOn(runtimeGametestJar)
        if (!compatReleaseJar.isPresent) {
            dependsOn(selectedArchiveTaskPath)
        }
        // Replace Loom's default project jar: the runner itself is only a
        // launcher. Keep the explicit runtime mods alongside the two mods
        // that must be exercised.
        mods.setFrom(runtimeGametestJar, addonFile, configurations.named("productionRuntimeMods"))
        useXVFB.set(System.getProperty("os.name").equals("Linux", ignoreCase = true))
        jvmArgs.add("-Dfabric.client.gametest")
        jvmArgs.add("-Dfabric.client.gametest.disableNetworkSynchronizer=true")
        jvmArgs.add(addonFile.map { "-Dsvc.bettergroups.expectedJar=${it.asFile.canonicalFile.toURI()}" })
        runDir.set(layout.buildDirectory.dir("run/$targetId/$minecraftVersion/${voicechatArtifact.replace('+', '_')}"))
        doFirst {
            val jar = addonFile.get().asFile.canonicalFile
            require(jar.isFile) { "Compatibility addon JAR does not exist: $jar" }
        }
    }
}

tasks.named("build") {
    dependsOn(fabricTargets.values.map { "${it.getValue("projectPath")}:build" })
}
