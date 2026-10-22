// Forge node for 1.21+ (ForgeGradle 7 + Renamer + Kotlin for Forge). 1.20.1 uses build.forge.gradle.kts (ModDevGradle
// legacyforge). ForgeGradle 6 refuses to run on Gradle 9, ForgeGradle 7 supports it. Shared logic is in
// gradle/common.gradle.kts.
//
// Forge 1.21 still runs on SRG names at runtime: the compiled jar (Mojang names) is renamed by the Renamer plugin and
// the mixin annotation processor's refmap is generated for that mapping. The jar that ships is the renamed one.
plugins {
    id("org.jetbrains.kotlin.jvm")
    id("net.minecraftforge.gradle")
    id("net.minecraftforge.renamer")
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

val mixinConfigs = listOf("superbcamera.mixins.json", "superbcamera.forge.mixins.json")

minecraft {
    mappings("official", property("minecraft_version").toString())

    runs {
        configureEach {
            // One run directory per node, so versions and loaders never share a world or a mods folder.
            workingDir.convention(rootProject.layout.projectDirectory.dir("run/$nodeName"))
            // Dev environment: Renamer adds the mixin configs to the arguments and the refmap remapping properties.
            renamer.mixin.run(this)
        }

        register("client")
    }
}

repositories {
    minecraft.mavenizer(this)
    maven(fg.forgeMaven)
    maven(fg.minecraftLibsMaven)
    maven {
        name = "Kotlin for Forge"
        url = uri("https://thedarkcolour.github.io/KotlinForForge/")
    }
}

dependencies {
    implementation(minecraft.dependency("net.minecraftforge:forge:${property("minecraft_version")}-${property("forge_version")}"))
    implementation("thedarkcolour:kotlinforforge:${property("kotlin_for_forge_version")}")

    // Mixin annotation processor: generates superbcamera.refmap.json for the Camera accessor and the Forge mixins.
    annotationProcessor("org.spongepowered:mixin:0.8.7:processor")
}

// The mixin annotation processor writes the refmap and its mappings next to the class output (build/tmp/compileJava),
// outside the task's declared outputs, so a build-cache hit would restore the classes without them.
tasks.withType<JavaCompile>().configureEach {
    outputs.doNotCacheIf("the mixin refmap is an undeclared output") { true }
}

renamer.mappings(minecraft.dependency.toSrg)
renamer.enableMixinRefmaps {
    // Registers the main source set under the name "superbcamera": its refmap is superbcamera.refmap.json, the file the
    // mixin configs point at (the default main.refmap.json would clash with other mods' refmaps on the class path).
    source(sourceSets.main.get(), "superbcamera")
    jar(tasks.named<Jar>("jar"))
    mixinConfigs.forEach { config(it) }
}

// The development jar keeps the Mojang names and is not a release artifact, so it stays out of build/libs; the renamed
// (SRG) jar below is the one that ships under the release name.
tasks.named<Jar>("jar") {
    archiveClassifier.set("dev")
    // Renamer names the refmap after the source set; the mixin configs point at a mod specific name (a shared
    // "main.refmap.json" would clash with other mods' refmaps on the class path).
    rename("main\\.refmap\\.json", "superbcamera.refmap.json")
    destinationDirectory.set(layout.buildDirectory.dir("devlibs"))
    manifest {
        attributes("MixinConfigs" to mixinConfigs.joinToString(","))
    }
}

renamer.classes("renameJar", tasks.named<Jar>("jar")) {
    mappings(renamer.mixin.generatedMappings)
    output.set(layout.buildDirectory.file(provider { "libs/${base.archivesName.get()}-$version.jar" }))
}

tasks.named("assemble") {
    dependsOn("renameJar")
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
