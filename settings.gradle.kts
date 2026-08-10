pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://repo.spongepowered.org/repository/maven-public/")
    }
}

rootProject.name = "svc-better-groups"

include("client-fabric")
include("client-fabric:mc26_1")
include("client-fabric:mc26_2")
include("client-fabric:compat-runner")
