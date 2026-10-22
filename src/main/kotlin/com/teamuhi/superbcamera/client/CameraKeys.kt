package com.teamuhi.superbcamera.client

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping
import org.lwjgl.glfw.GLFW

/** Key bindings of the mod. Registered by the loader entry point; rebindable in the vanilla Controls screen. */
object CameraKeys {
    const val CATEGORY = "key.categories.superbcamera"

    /** Opens the live editor on a custom perspective (and closes it again while it is open). Default: backslash. */
    val EDITOR = KeyMapping("key.superbcamera.editor", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_BACKSLASH, CATEGORY)

    /**
     * Switches between the vanilla view and the custom perspectives without the F5 cycle; with Shift held and a
     * perspective already active it steps to the next one instead. Default: V.
     */
    val TOGGLE = KeyMapping("key.superbcamera.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY)
}
