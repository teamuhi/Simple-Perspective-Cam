package com.teamuhi.superbcamera.client

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Path

/**
 * Fabric entry point (`client` entrypoint through Fabric Language Kotlin; the mod is client-only, so nothing runs on a
 * dedicated server). The camera takeover, the field of view and the HUD hiding are not events on Fabric: they are
 * mixins in `com.teamuhi.superbcamera.mixin` that call the same [CameraController] functions the Forge events do.
 */
object SuperbCameraFabric : ClientModInitializer {

    override fun onInitializeClient() {
        Platform.install(object : PlatformServices {
            override val configDir: Path get() = FabricLoader.getInstance().configDir
            override fun isModLoaded(id: String): Boolean = FabricLoader.getInstance().isModLoaded(id)
        })
        ClientSetup.init()

        KeyBindingHelper.registerKeyBinding(CameraKeys.EDITOR)
        KeyBindingHelper.registerKeyBinding(CameraKeys.TOGGLE)
        ClientTickEvents.START_CLIENT_TICK.register { CameraController.onClientTickStart() }
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ -> CameraController.onLoggingOut() }
    }
}
