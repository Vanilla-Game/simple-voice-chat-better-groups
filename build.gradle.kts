plugins {
    java
}

group = "ru.vanillagame.voicechat"
version = "0.4.0" // x-release-please-version

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
    maven("https://maven.maxhenkel.de/repository/public") {
        name = "maxhenkel"
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.84-stable")
    compileOnly("de.maxhenkel.voicechat:voicechat-api:2.6.20")

    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("net.kyori:adventure-api:5.2.0")
    testImplementation("net.kyori:adventure-text-serializer-plain:5.2.0")
    testImplementation("io.papermc.paper:paper-api:26.2.build.84-stable")
    testImplementation("de.maxhenkel.voicechat:voicechat-api:2.6.20")
    testImplementation("org.mockito:mockito-core:5.23.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 25
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

tasks.named("build") {
    dependsOn(":client-fabric:build")
}
