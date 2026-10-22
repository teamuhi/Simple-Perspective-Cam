package com.teamuhi.superbcamera.client

import com.mojang.blaze3d.platform.InputConstants
import com.teamuhi.superbcamera.client.ui.CameraEditorScreen
import com.teamuhi.superbcamera.mixin.CameraAccessor
import net.minecraft.client.Camera
import net.minecraft.client.CameraType
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import org.lwjgl.glfw.GLFW

/**
 * Runtime heart of the mod (client only): the per-tick work, the camera takeover and the HUD hiding.
 *
 * This object is loader-agnostic: every entry point is a plain function that the loader glue calls from its own hook
 * (Forge events in `src/forge`, mixins in `src/fabric`).
 *
 * The takeover ([onCameraSetup]) must run right after `Camera.setup` (where Superb Warfare's own camera mixin acts) and
 * before the game reads the camera position, so whatever is set here wins without replacing `Camera.setup`. On Forge
 * that is `ViewportEvent.ComputeCameraAngles` at the lowest priority.
 */
object CameraController {

    /** Editor status line for the frame that was just rendered. */
    var status: Component = Component.empty()
        private set

    /** True when [status] describes a fallback or lost target (drawn in the warning colour). */
    var statusWarning = false
        private set

    private var smoothRotation: Quaternionf? = null
    private var smoothPosition: Vec3? = null
    private var smoothFor: Perspective? = null

    /** The vanilla camera type from before [toggleCustom] switched on a perspective; restored when it switches off. */
    private var previousCameraType: CameraType? = null

    /** A jump larger than this many blocks is a teleport or a new anchor, so smoothing snaps instead of sliding. */
    private const val SNAP_DISTANCE_SQ = 128.0 * 128.0

    /** Forgets the smoothed state so the next frame snaps to the target camera. */
    fun resetSmoothing() {
        FreeLook.reset()
        smoothRotation = null
        smoothPosition = null
        smoothFor = null
    }

    /** Sets the vanilla camera type with the same side effects as the F5 handler in `Minecraft.handleKeybinds`. */
    fun switchCameraType(type: CameraType) {
        val mc = Minecraft.getInstance()
        val before = mc.options.cameraType
        mc.options.cameraType = type
        if (before.isFirstPerson != type.isFirstPerson) {
            mc.gameRenderer.checkEntityPostEffect(if (type.isFirstPerson) mc.getCameraEntity() else null)
        }
        mc.levelRenderer.needsUpdate()
    }

    /**
     * True while the active perspective should hide the in-game HUD (never while the editor is open). The loader glue
     * hides every HUD layer except the chat and the debug screen while this is true, which leaves the player's own F1
     * state untouched.
     */
    fun hidesHud(): Boolean {
        val perspective = CameraStore.active ?: return false
        val mc = Minecraft.getInstance()
        return perspective.hideHud && mc.screen !is CameraEditorScreen && mc.options.cameraType == CameraType.THIRD_PERSON_BACK
    }

    // ------------------------------------------------------------------------------------------------ hooks

    /** Start of every client tick. */
    fun onClientTickStart() {
        val mc = Minecraft.getInstance()
        if (mc.level == null || mc.player == null) return

        // Something else (Superb Warfare's camera reset, another mod) left third person back: stop overriding.
        if (CameraStore.activeIndex >= 0 && mc.options.cameraType != CameraType.THIRD_PERSON_BACK) {
            CameraStore.deactivate()
        }
        if (CameraStore.activeIndex < 0) previousCameraType = null
        FreeLook.update(mc)
        TargetTracker.tick()
        PerspectiveCycle.tick(mc)

        // Drain the key even when it cannot be used, so presses made inside other screens do not pile up.
        while (CameraKeys.EDITOR.consumeClick()) {
            if (mc.screen == null) openEditor(mc)
        }
        while (CameraKeys.TOGGLE.consumeClick()) {
            if (mc.screen == null) toggleCustom(mc)
        }
    }

    /**
     * The camera takeover, called once per frame right after `Camera.setup`. [yaw], [pitch] and [roll] are the look
     * direction the game would have used (what `ViewportEvent.ComputeCameraAngles` carries on Forge).
     *
     * Moves [camera] to the active perspective's position and rotation and returns the final angles, or null when the
     * camera was left alone (no active perspective, wrong camera type, or a non-finite result). The loader glue must
     * hand the returned angles to the renderer; the roll in particular is not part of the vanilla camera.
     */
    fun onCameraSetup(camera: Camera, partialTick: Float, yaw: Float, pitch: Float, roll: Float): CameraMath.Angles? {
        val perspective = CameraStore.active ?: return null
        val mc = Minecraft.getInstance()
        if (mc.options.cameraType != CameraType.THIRD_PERSON_BACK) {
            CameraStore.deactivate()
            return null
        }
        FreeLook.update(mc)

        val frame = CameraAnchor.resolve(perspective, partialTick)
        status = frame.status
        statusWarning = frame.warning

        // Locked: relative to the anchor. Free: relative to the look direction the game would have used.
        val base = if (perspective.locked) Quaternionf(frame.rotation)
        else CameraMath.body(yaw.toDouble(), pitch.toDouble(), if (perspective.inheritRoll) roll.toDouble() else 0.0)
        // Left Alt free-look turns only the rendered camera (position is unchanged); the offsets are 0 otherwise.
        val look = CameraMath.body(perspective.yaw + FreeLook.yaw, perspective.pitch + FreeLook.pitch, perspective.roll)
        var rotation = Quaternionf(base).mul(look)
        var position = frame.position.add(CameraMath.worldOffset(base, perspective.x, perspective.y, perspective.z))
        if (!CameraMath.isFinite(rotation) || !CameraMath.isFinite(position)) return null

        // Smoothing in quaternion space avoids yaw wrap-around and flips; 0 is an exact lock.
        val previousRotation = smoothRotation
        val previousPosition = smoothPosition
        if (perspective.smoothing > 0.0 && previousRotation != null && previousPosition != null && smoothFor === perspective &&
            previousPosition.distanceToSqr(position) < SNAP_DISTANCE_SQ
        ) {
            val alpha = CameraMath.smoothingAlpha(perspective.smoothing, frameTime(mc))
            rotation = Quaternionf(previousRotation).slerp(rotation, alpha).normalize()
            position = CameraMath.lerp(alpha.toDouble(), previousPosition, position)
        }
        smoothRotation = rotation
        smoothPosition = position
        smoothFor = perspective
        if (!CameraMath.isFinite(rotation) || !CameraMath.isFinite(position)) {
            resetSmoothing()
            return null
        }

        val angles = CameraMath.toAngles(rotation)
        if (!angles.yaw.isFinite() || !angles.pitch.isFinite() || !angles.roll.isFinite()) return null

        // setRotation refreshes the camera's look / up / left vectors, then the quaternion gets the roll as well
        // (particles and anything else reading Camera.rotation() would otherwise ignore it). `rotation` is the body
        // orientation (looking along +Z); Camera.rotation() is that turned half a turn about its own Y axis (the camera
        // looks along -Z). From 1.21 the renderer builds the world view from this quaternion, so it has to be exact.
        val accessor = camera as CameraAccessor
        accessor.`superbcamera$setRotation`(angles.yaw, angles.pitch)
        camera.rotation().set(rotation).rotateY(Math.PI.toFloat())
        accessor.`superbcamera$setPosition`(position.x, position.y, position.z)
        return angles
    }

    /**
     * The field of view of the active perspective, or null to keep the game's own value. Only ask for the main
     * (configured fov) pass: the hand is rendered in a second pass with its own fov that must not be replaced.
     */
    fun fovOverride(): Double? {
        val perspective = CameraStore.active ?: return null
        if (Minecraft.getInstance().options.cameraType != CameraType.THIRD_PERSON_BACK) return null
        return perspective.fov
    }

    /** Leaving a world: forget the tracked target and the runtime selection, then save the config. */
    fun onLoggingOut() {
        previousCameraType = null
        FreeLook.reset()
        TargetTracker.forget()
        CameraStore.resetRuntime()
        CameraStore.save()
    }

    // ------------------------------------------------------------------------------------------------ toggle

    /**
     * The "Toggle Simple Perspective Camera" key. With no custom perspective active: activates the last used one
     * (creating the first when none exist) and remembers the vanilla view to return to. With one active: switches
     * back to that view, or to the next perspective (wrapping around) while Shift is held.
     */
    private fun toggleCustom(mc: Minecraft) {
        val shift = InputConstants.isKeyDown(mc.window.window, GLFW.GLFW_KEY_LEFT_SHIFT) ||
            InputConstants.isKeyDown(mc.window.window, GLFW.GLFW_KEY_RIGHT_SHIFT)
        val active = CameraStore.activeIndex
        if (active >= 0) {
            if (shift) {
                CameraStore.activate(Math.floorMod(active + 1, CameraStore.perspectives.size))
            } else {
                val target = previousCameraType ?: CameraType.FIRST_PERSON
                previousCameraType = null
                CameraStore.deactivate()
                switchCameraType(target)
            }
            mc.levelRenderer.needsUpdate()
            return
        }
        if (CameraStore.perspectives.isEmpty() && !createFirstPerspective(mc)) return
        val before = mc.options.cameraType
        if (!CameraStore.activate(CameraStore.lastUsedIndex)) return
        previousCameraType = before
        // activate() already forces third person back; this keeps the side effects explicit for other camera types.
        switchCameraType(CameraType.THIRD_PERSON_BACK)
    }

    /**
     * Adds the first perspective (locked while riding; vehicle-anchored only with Superb Warfare, otherwise it follows
     * the player). Returns false when it could not be added.
     */
    private fun createFirstPerspective(mc: Minecraft): Boolean {
        val riding = (mc.cameraEntity ?: mc.player)?.vehicle != null
        val first = Perspective(
            anchor = if (riding && Anchor.VEHICLE.isAvailable) Anchor.VEHICLE else Anchor.PLAYER,
            locked = riding
        )
        return CameraStore.add(first) >= 0
    }

    /** Real time since the previous frame in ticks (the unit the smoothing curve expects). */
    private fun frameTime(mc: Minecraft): Float {
        //? if >=1.21 {
        /*return mc.timer.realtimeDeltaTicks
        *///?} else {
        return mc.deltaFrameTime
        //?}
    }

    // ------------------------------------------------------------------------------------------------ editor

    /** Opens the editor on the active perspective (or the last used one); creates the first perspective if none exist. */
    fun openEditor(mc: Minecraft) {
        if (CameraStore.perspectives.isEmpty() && !createFirstPerspective(mc)) return
        val wasActive = CameraStore.activeIndex >= 0
        val before = mc.options.cameraType
        val index = if (wasActive) CameraStore.activeIndex else CameraStore.lastUsedIndex
        if (!CameraStore.activate(index)) return
        // Coming from a vanilla view: remember it so Esc in the editor can return to it.
        if (!wasActive) previousCameraType = before
        mc.setScreen(CameraEditorScreen())
    }

    /**
     * Switches the custom camera off and returns to the vanilla view from before it was switched on (first person when
     * none is remembered, e.g. it was reached through F5). Stops spectating a followed entity. The perspectives stay
     * saved; the caller closes the editor.
     */
    fun exitToVanilla() {
        TargetTracker.release()
        val target = previousCameraType ?: CameraType.FIRST_PERSON
        previousCameraType = null
        CameraStore.deactivate()
        switchCameraType(target)
        Minecraft.getInstance().levelRenderer.needsUpdate()
    }
}
