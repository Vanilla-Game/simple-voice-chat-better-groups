pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://repo.spongepowered.org/repository/maven-public/")
    }
}

rootProject.name = "simple-voice-chat-group-management"

include("client-fabric")
