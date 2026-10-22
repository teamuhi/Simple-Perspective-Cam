package com.teamuhi.superbcamera.client

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.CameraType
import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW

/**
 * Left Alt free-look (client only, runtime only): while a custom perspective drives the camera, holding Left Alt turns
 * the mouse into extra yaw / pitch for the rendered camera and the player's own body and aim stay where they are.
 *
 * The offsets are added to the active perspective's own yaw / pitch in [CameraController.onCameraSetup]. They are never
 * saved and are dropped as soon as free-look stops being possible: Alt released, a screen opened (the editor's own
 * Alt+drag emulation lives in its screen and never sees this), the window lost focus (Alt+Tab), the perspective
 * changed or deactivated, a non-custom camera type, or a world left. With the perspective's smoothing above 0 the
 * view eases back after release; with 0 it snaps.
 *
 * The mouse is intercepted by `MouseHandlerMixin` (shared mixin, both loaders), which calls [onTurn] instead of
 * `LocalPlayer.turn` while this is active.
 */
object FreeLook {

    /** Same factor as `Entity.turn`, so free-look feels like normal looking. */
    private const val TURN_SCALE = 0.15
    private const val MAX_PITCH = 89.0

    /** Extra yaw in degrees, -180..180. */
    var yaw = 0.0
        private set

    /** Extra pitch in degrees (positive looks down); the perspective's pitch plus this stays within +-89. */
    var pitch = 0.0
        private set

    /** True while Alt is held and free-look is allowed. */
    var active = false
        private set

    /** Drops the offsets and ends free-look. */
    fun reset() {
        yaw = 0.0
        pitch = 0.0
        active = false
    }

    /** Re-evaluates the conditions (cheap; called each tick, each frame and on each mouse update). */
    fun update(mc: Minecraft): Boolean {
        val on = eligible(mc)
        if (on) active = true else reset()
        return on
    }

    private fun eligible(mc: Minecraft): Boolean {
        if (CameraStore.active == null) return false
        if (mc.level == null || mc.player == null || mc.screen != null) return false
        if (!mc.isWindowActive || !mc.mouseHandler.isMouseGrabbed) return false
        if (mc.options.cameraType != CameraType.THIRD_PERSON_BACK) return false
        return InputConstants.isKeyDown(mc.window.window, GLFW.GLFW_KEY_LEFT_ALT)
    }

    /**
     * Mouse movement of one frame, already scaled by the sensitivity and the invert option (the arguments of
     * `LocalPlayer.turn`). Returns true when free-look took it, so the player must not be turned.
     */
    fun onTurn(dx: Double, dy: Double): Boolean {
        if (!update(Minecraft.getInstance())) return false
        val perspective = CameraStore.active ?: return false
        if (dx.isFinite()) {
            val turned = yaw + dx * TURN_SCALE
            yaw = ((turned + 180.0) % 360.0 + 360.0) % 360.0 - 180.0
        }
        if (dy.isFinite()) {
            pitch = (perspective.pitch + pitch + dy * TURN_SCALE).coerceIn(-MAX_PITCH, MAX_PITCH) - perspective.pitch
        }
        return true
    }
}
