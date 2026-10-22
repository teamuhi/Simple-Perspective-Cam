package com.teamuhi.superbcamera.client

import net.minecraftforge.client.event.ClientPlayerNetworkEvent
import net.minecraftforge.client.event.ViewportEvent
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.eventbus.api.SubscribeEvent

/**
 * Forge bus glue (client only, Forge 1.21): forwards the Forge events to the loader-agnostic [CameraController].
 *
 * The takeover runs in `ViewportEvent.ComputeCameraAngles` at the lowest priority. Forge posts it from
 * `GameRenderer.renderLevel` right after `Camera.setup` (where Superb Warfare's own camera mixin acts) and before the
 * camera is read, so whatever is set here wins without replacing `Camera.setup`. The HUD hiding is `GuiMixin`: Forge's
 * 1.21 layer API has no event that can cancel the vanilla layers.
 */
object ForgeClientEvents {

    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent.Pre) {
        CameraController.onClientTickStart()
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    fun onComputeCameraAngles(event: ViewportEvent.ComputeCameraAngles) {
        val angles = CameraController.onCameraSetup(
            event.camera, event.partialTick.toFloat(), event.yaw, event.pitch, event.roll
        ) ?: return
        event.yaw = angles.yaw
        event.pitch = angles.pitch
        // Forge hands the roll straight to Camera.setRotation(yaw, pitch, z), whose z turns the opposite way of the
        // roll the controller reports (the one vanilla and NeoForge use), hence the sign.
        event.roll = -angles.roll
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    fun onComputeFov(event: ViewportEvent.ComputeFov) {
        // The hand is rendered in a second pass with its own fov; only the main (configured fov) pass is replaced.
        if (!event.usedConfiguredFov()) return
        event.fov = CameraController.fovOverride() ?: return
    }

    @SubscribeEvent
    fun onLoggingOut(event: ClientPlayerNetworkEvent.LoggingOut) {
        CameraController.onLoggingOut()
    }
}
