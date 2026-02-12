pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

if (!file(".git").exists()) {
    val errorText = """

        =====================[ ERROR ]=====================
         The project directory is not a properly cloned Git repository.

         In order to build SurvivalCore from source you must clone
         the repository using Git, not download a code zip.
        ===================================================
    """.trimIndent()
    error(errorText)
}

rootProject.name = "survivalcore"

include("survivalcore-api")
include("survivalcore-server")
