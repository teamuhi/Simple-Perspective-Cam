package com.teamuhi.superbcamera.client

/**
 * Loader-agnostic client start-up. The loader entry point installs the [Platform], registers [CameraKeys.EDITOR] and
 * forwards its events to [CameraController], then calls [init].
 */
object ClientSetup {

    fun init() {
        CameraStore.load()
    }
}
