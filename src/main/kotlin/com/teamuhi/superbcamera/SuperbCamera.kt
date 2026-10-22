package com.teamuhi.superbcamera

import org.apache.logging.log4j.LogManager

/**
 * Loader-agnostic constants of the mod. The entry points live in the per-loader source sets (`src/forge`,
 * `src/fabric`, ...): Simple Perspective Camera is client-only, so on a dedicated server nothing is registered and no client class
 * is touched. Superb Warfare is an optional dependency (see [com.teamuhi.superbcamera.client.SbwBridge]).
 */
object SuperbCamera {
    const val MOD_ID = "superbcamera"

    @JvmField
    val LOGGER = LogManager.getLogger(MOD_ID)
}
