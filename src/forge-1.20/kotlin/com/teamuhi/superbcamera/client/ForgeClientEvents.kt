package com.teamuhi.superbcamera.client

import net.minecraftforge.client.event.ClientPlayerNetworkEvent
import net.minecraftforge.client.event.RenderGuiOverlayEvent
import net.minecraftforge.client.event.ViewportEvent
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.eventbus.api.SubscribeEvent

/**
 * Forge bus glue (client only): forwards the Forge events to the loader-agnostic [CameraController].
 *
 * The takeover runs in `ViewportEvent.ComputeCameraAngles` at the lowest priority. Vanilla's `GameRenderer.renderLevel`
 * calls `Camera.setup` (where Superb Warfare's own camera mixin acts) and then this event, and only afterwards reads the
 * camera position, so whatever is set here wins without replacing `Camera.setup`.
 */
object ForgeClientEvents {

    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        CameraController.onClientTickStart()
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    fun onComputeCameraAngles(event: ViewportEvent.ComputeCameraAngles) {
        val angles = CameraController.onCameraSetup(
            event.camera, event.partialTick.toFloat(), event.yaw, event.pitch, event.roll
        ) ?: return
        event.yaw = angles.yaw
        event.pitch = angles.pitch
        event.roll = angles.roll
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    fun onComputeFov(event: ViewportEvent.ComputeFov) {
        // The hand is rendered in a second pass with its own fov; only the main (configured fov) pass is replaced.
        if (!event.usedConfiguredFov()) return
        event.fov = CameraController.fovOverride() ?: return
    }

    /** Hides the HUD (including Superb Warfare's overlays) without touching the player's own F1 state. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onOverlay(event: RenderGuiOverlayEvent.Pre) {
        if (!CameraController.hidesHud()) return
        val id = event.overlay.id()
        if (id == VanillaGuiOverlay.CHAT_PANEL.id() || id == VanillaGuiOverlay.DEBUG_TEXT.id()) return
        event.isCanceled = true
    }

    @SubscribeEvent
    fun onLoggingOut(event: ClientPlayerNetworkEvent.LoggingOut) {
        CameraController.onLoggingOut()
    }
}
