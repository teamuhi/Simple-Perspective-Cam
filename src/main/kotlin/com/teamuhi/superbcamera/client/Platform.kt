package com.teamuhi.superbcamera.client

import java.nio.file.Path

/** What the common code needs from the mod loader. Each loader entry point supplies one through [Platform.install]. */
interface PlatformServices {
    /** The loader's config directory (`config/`). */
    val configDir: Path

    /** True when the mod with [id] is loaded. */
    fun isModLoaded(id: String): Boolean
}

/**
 * Access point for the loader-specific services, so the shared code never imports a loader class. The loader entry
 * point must call [install] before anything else of the mod runs.
 */
object Platform : PlatformServices {
    private var services: PlatformServices? = null

    /** Registers the loader's implementation (called once from the entry point). */
    fun install(services: PlatformServices) {
        this.services = services
    }

    private fun current(): PlatformServices =
        services ?: error("Platform.install() was not called before the Simple Perspective Camera platform services were used")

    override val configDir: Path get() = current().configDir

    override fun isModLoaded(id: String): Boolean = current().isModLoaded(id)
}
