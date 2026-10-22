plugins {
    id("dev.kikugie.stonecutter")
    // Loaded once here (not per node), so the nodes share one plugin class loader. Add the other loaders' plugins here
    // too when their nodes are added; the versions come from settings.gradle.kts.
    id("org.jetbrains.kotlin.jvm") apply false
    id("net.neoforged.moddev.legacyforge") apply false
    id("net.neoforged.moddev") apply false
    id("net.minecraftforge.gradle") apply false
    id("net.minecraftforge.renamer") apply false
    id("fabric-loom") apply false
}

// The node the IDE sees (switch with the `Set active project to ...` Gradle tasks).
stonecutter active "1.20.1-forge"

stonecutter parameters {
    // Node names are `<minecraft version>-<loader>`.
    val loader = current.project.substringAfterLast('-')

    constants {
        // Loader checks in shared code: `//? if forge {` ... `//?} else if fabric {` ...
        match(loader, "forge", "fabric", "neoforge")
    }
}

// Builds the jar of every node: `./gradlew buildAll`.
tasks.register("buildAll") {
    group = "build"
    description = "Builds the jar of every loader / Minecraft version node."
    dependsOn(subprojects.map { "${it.path}:build" })
}
