// Build logic shared by every node, whatever its loader. Applied by build.<loader>.gradle.kts right after its plugins {}.
// Everything loader specific (Minecraft / loader dependencies, run configs, mixin wiring) stays in those scripts.
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSetContainer
import java.time.Instant

fun prop(name: String): String = property(name).toString()

/** Node names are `<minecraft version>-<loader>`. */
val loader = name.substringAfterLast('-')
val javaVersion = prop("java_version").toInt()

// <archive_name>-<mod version>-<loader>-mc<minecraft version>.jar, e.g. simpleperspectivecamera-0.3.0-fabric-mc1.21.1.jar
version = "${prop("mod_version")}-$loader-mc${prop("minecraft_version")}"
group = "com.teamuhi.superbcamera"

extensions.configure<BasePluginExtension> {
    archivesName.set(prop("archive_name"))
}

extensions.configure<JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
    withSourcesJar()
}

// Source sets next to the shared one (src/main). They sit in the root project, not in versions/<node>, because every node
// of every version shares them. Stonecutter comments are only processed in src/main, so code that differs between
// Minecraft versions but cannot live there (loader glue, mixins and their configs) lives in extra directories, each with
// java / kotlin / resources:
//   src/<loader>          loader glue shared by every version (entry point, metadata)
//   src/<loader>-<band>   loader glue of one version band; the node's `source_band` property (1.20, 1.21, ...)
//   src/<name>            shared pieces a node opts into with `extra_source_dirs=<name>,<name>` (e.g. a mixin that two
//                         loaders need but the third does not)
// The Kotlin ones are added by the loader scripts (this script cannot see the Kotlin plugin's types):
// `kotlin { sourceSets.main { kotlin.srcDirs(kotlinSourceDirs) } }`.
val sourceBand = findProperty("source_band")?.toString()
val extraSourceDirs = findProperty("extra_source_dirs")?.toString().orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }
val sourceRoots = (listOf("src/$loader") + listOfNotNull(sourceBand?.let { "src/$loader-$it" }) + extraSourceDirs.map { "src/$it" })
    .map { rootProject.file(it) }
extra["kotlinSourceDirs"] = sourceRoots.map { File(it, "kotlin") }
extra["javaVersion"] = javaVersion

extensions.configure<SourceSetContainer> {
    named("main") {
        sourceRoots.forEach {
            java.srcDir(File(it, "java"))
            resources.srcDir(File(it, "resources"))
        }
    }
}

repositories {
    mavenCentral()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

/** Placeholders shared by every metadata file (mods.toml, fabric.mod.json, the mixin configs). */
extra["commonProperties"] = mapOf(
    "mod_id" to prop("mod_id"),
    "mod_name" to prop("mod_name"),
    "mod_license" to prop("mod_license"),
    "mod_version" to prop("mod_version"),
    "mod_authors" to prop("mod_authors"),
    "mod_description" to prop("mod_description"),
    "minecraft_version_range" to prop("minecraft_version_range"),
    "superbwarfare_version_range" to prop("superbwarfare_version_range"),
    "java_version" to javaVersion.toString(),
    "pack_format" to prop("pack_format")
)

val nodeName = name
val nodeVersion = version.toString()

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            "Specification-Title" to prop("mod_id"),
            "Specification-Vendor" to prop("mod_authors"),
            "Specification-Version" to "1",
            "Implementation-Title" to nodeName,
            "Implementation-Version" to nodeVersion,
            "Implementation-Vendor" to prop("mod_authors"),
            "Implementation-Timestamp" to Instant.now().toString()
        )
    }
}
