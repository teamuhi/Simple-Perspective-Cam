package com.teamuhi.superbcamera.client

import net.neoforged.bus.api.EventPriority
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent
import net.neoforged.neoforge.client.event.ViewportEvent
import net.neoforged.neoforge.client.gui.VanillaGuiLayers

/**
 * NeoForge bus glue (client only): forwards the NeoForge events to the loader-agnostic [CameraController].
 *
 * The camera takeover is not here: since 1.21 NeoForge posts `ViewportEvent.ComputeCameraAngles` at the start of
 * `Camera.setup`, before the position is set, so it cannot move the camera. `CameraMixin` (shared by every 1.21 loader)
 * does it at the end of `Camera.setup` instead. The field of view still goes through the event at the lowest priority.
 */
object NeoForgeClientEvents {

    @SubscribeEvent
    fun onClientTick(event: ClientTickEvent.Pre) {
        CameraController.onClientTickStart()
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    fun onComputeFov(event: ViewportEvent.ComputeFov) {
        // The hand is rendered in a second pass with its own fov; only the main (configured fov) pass is replaced.
        if (!event.usedConfiguredFov()) return
        event.fov = CameraController.fovOverride() ?: return
    }

    /** Hides the HUD (including Superb Warfare's overlays) without touching the player's own F1 state. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onGuiLayer(event: RenderGuiLayerEvent.Pre) {
        if (!CameraController.hidesHud()) return
        val id = event.name
        if (id == VanillaGuiLayers.CHAT || id == VanillaGuiLayers.DEBUG_OVERLAY) return
        event.isCanceled = true
    }

    @SubscribeEvent
    fun onLoggingOut(event: ClientPlayerNetworkEvent.LoggingOut) {
        CameraController.onLoggingOut()
    }
}
