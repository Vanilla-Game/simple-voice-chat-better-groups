pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://repo.spongepowered.org/repository/maven-public/")
    }
}

rootProject.name = "voicechat-group-tools"

include("client-fabric")
