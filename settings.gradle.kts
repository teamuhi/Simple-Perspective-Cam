pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "FabricMC" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
    }
    // Plugin versions are declared once here; the per-loader build scripts apply them without a version.
    plugins {
        id("org.jetbrains.kotlin.jvm") version "2.0.0"
        id("net.neoforged.moddev.legacyforge") version "2.0.91" // Forge 1.20.1
        id("net.neoforged.moddev") version "2.0.91" // NeoForge 1.21+ (same ModDevGradle release as legacyforge)
        id("net.minecraftforge.gradle") version "7.0.40" // Forge 1.21+ (ForgeGradle 6 rejects Gradle 9)
        id("net.minecraftforge.renamer") version "1.1.0" // Forge 1.21+: SRG jar + mixin refmaps
        id("fabric-loom") version "1.17.21" // Fabric, obfuscated Minecraft versions (1.18+ needs a Java 25 Gradle JVM)
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
    // Lets Gradle download the toolchain a node needs (Java 17 / 21 / 25) when it is not installed.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        /**
         * One node per (Minecraft version, loader): the project `versions/<version>-<loader>` is built with
         * `build.<loader>.gradle.kts` and its properties live in `versions/<version>-<loader>/gradle.properties`.
         */
        fun match(version: String, vararg loaders: String) {
            for (loader in loaders) {
                // Forge 1.20.1 is built by ModDevGradle legacyforge, 1.21+ by ForgeGradle: two different build scripts.
                val script = if (loader == "forge" && version != "1.20.1") "forge-modern" else loader
                version("$version-$loader", version).buildscript("build.$script.gradle.kts")
            }
        }

        match("1.20.1", "forge", "fabric")
        match("1.21.1", "forge", "neoforge", "fabric")

        vcsVersion = "1.20.1-forge"
    }
}

rootProject.name = "superbcamera"
