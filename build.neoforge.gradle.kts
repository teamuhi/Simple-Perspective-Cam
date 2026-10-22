// NeoForge node (1.21+: ModDevGradle `net.neoforged.moddev`, Kotlin for Forge). Shared logic is in gradle/common.gradle.kts.
// NeoForge runs on Mojang names, so the mixins need no refmap here (the shared config just points at one that is absent).
plugins {
    eclipse
    idea
    id("org.jetbrains.kotlin.jvm")
    id("net.neoforged.moddev")
}

apply(from = rootProject.file("gradle/common.gradle.kts"))

@Suppress("UNCHECKED_CAST")
val commonProperties = extra["commonProperties"] as Map<String, String>
val javaVersion = extra["javaVersion"] as Int
@Suppress("UNCHECKED_CAST")
val kotlinSourceDirs = extra["kotlinSourceDirs"] as List<File>
val nodeName = name

kotlin {
    jvmToolchain(javaVersion)
    sourceSets.main {
        kotlin.srcDirs(kotlinSourceDirs)
    }
}

neoForge {
    version = property("neoforge_version").toString()

    parchment {
        minecraftVersion = property("minecraft_version").toString()
        mappingsVersion = property("parchment_mappings").toString()
    }

    mods {
        create(property("mod_id").toString()) {
            sourceSet(sourceSets.main.get())
        }
    }

    runs {
        configureEach {
            // One run directory per node, so versions and loaders never share a world or a mods folder.
            gameDirectory = rootProject.file("run/$nodeName")
            logLevel = org.slf4j.event.Level.INFO
        }

        create("client") {
            client()
        }

        create("server") {
            server()
        }
    }
}

repositories {
    maven {
        name = "Kotlin for Forge"
        url = uri("https://thedarkcolour.github.io/KotlinForForge/")
    }
}

dependencies {
    implementation("thedarkcolour:kotlinforforge-neoforge:${property("kotlin_for_forge_version")}")
}

// Placeholders of neoforge.mods.toml on top of the shared ones.
val metadataProperties = mapOf(
    "neoforge_version_range" to property("neoforge_version_range").toString(),
    "loader_version_range" to property("loader_version_range").toString()
)

tasks.named<ProcessResources>("processResources") {
    val properties = commonProperties + metadataProperties
    inputs.properties(properties)
    filesMatching(listOf("META-INF/neoforge.mods.toml", "*.mixins.json", "pack.mcmeta")) {
        expand(properties)
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
