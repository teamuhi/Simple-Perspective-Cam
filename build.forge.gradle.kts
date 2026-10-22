// Forge node (1.20.1: ModDevGradle `legacyforge`). Shared logic is in gradle/common.gradle.kts.
plugins {
    eclipse
    idea
    id("org.jetbrains.kotlin.jvm")
    id("net.neoforged.moddev.legacyforge")
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

legacyForge {
    version = "${property("minecraft_version")}-${property("forge_version")}"

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
            systemProperty("forge.logging.markers", "REGISTRIES")
            systemProperty("forge.logging.console.level", "info")
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
    implementation("thedarkcolour:kotlinforforge:${property("kotlin_for_forge_version")}")

    // Mixin annotation processor: generates superbcamera.refmap.json for the Camera accessor.
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
}

mixin {
    add(sourceSets.main.get(), "superbcamera.refmap.json")
    config("superbcamera.mixins.json")
}

// Placeholders of mods.toml on top of the shared ones.
val metadataProperties = mapOf(
    "forge_version_range" to property("forge_version_range").toString(),
    "loader_version_range" to property("loader_version_range").toString()
)

tasks.named<ProcessResources>("processResources") {
    val properties = commonProperties + metadataProperties
    inputs.properties(properties)
    filesMatching(listOf("META-INF/mods.toml", "*.mixins.json", "pack.mcmeta")) {
        expand(properties)
    }
}

tasks.named<Jar>("jar") {
    manifest {
        attributes("MixinConfigs" to "superbcamera.mixins.json")
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
