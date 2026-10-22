// Fabric node (Fabric Loom + Fabric API + Fabric Language Kotlin). Shared logic is in gradle/common.gradle.kts.
// Obfuscated Minecraft versions (up to 1.21.11) use the `fabric-loom` plugin with Mojang names + Parchment, the same
// names the Forge node compiles against. The unobfuscated 26.x versions need the loom-back-compat plugin instead.
plugins {
    id("org.jetbrains.kotlin.jvm")
    id("fabric-loom")
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

repositories {
    maven {
        name = "ParchmentMC"
        url = uri("https://maven.parchmentmc.org")
        content {
            includeGroup("org.parchmentmc.data")
        }
    }
}

dependencies {
    "minecraft"("com.mojang:minecraft:${property("minecraft_version")}")
    "mappings"(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-${property("minecraft_version")}:${property("parchment_mappings")}@zip")
    })

    "modImplementation"("net.fabricmc:fabric-loader:${property("fabric_loader_version")}")
    "modImplementation"("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
    "modImplementation"("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")
}

loom {
    mixin {
        // Loom 1.17+ no longer runs the mixin annotation processor by default; the Mojang-named mixin targets need
        // the refmap it generates to find their intermediary names at runtime.
        useLegacyMixinAp = true
        // Same refmap name as the Forge node, so the shared superbcamera.mixins.json works on both.
        defaultRefmapName.set("superbcamera.refmap.json")
    }

    runConfigs.configureEach {
        // One run directory per node, so versions and loaders never share a world or a mods folder.
        runDir("../../run/$nodeName")
    }
}

// Placeholders of fabric.mod.json on top of the shared ones.
val metadataProperties = mapOf("fabric_loader_range" to property("fabric_loader_range").toString())

tasks.named<ProcessResources>("processResources") {
    val properties = commonProperties + metadataProperties
    inputs.properties(properties)
    filesMatching(listOf("fabric.mod.json", "*.mixins.json", "pack.mcmeta")) {
        expand(properties)
    }
}
