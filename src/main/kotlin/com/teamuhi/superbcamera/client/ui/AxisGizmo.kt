package com.teamuhi.superbcamera.client.ui

import com.teamuhi.superbcamera.client.CameraMath
import com.teamuhi.superbcamera.client.Perspective
import com.teamuhi.superbcamera.client.ui.kit.Ui
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.roundToInt

/**
 * The axis gizmo in the corner of the free viewport, after Blender's navigation gizmo: a 2D projection of the
 * **anchor's** X / Y / Z axes into the current camera view (editor convention: +X right, +Y up, +Z forward).
 *
 * Layers, back to front: a soft dark disc with a faint light outline (readable on bright sky and in dark caves), then
 * every axis end sorted by depth. Positive ends are solid balls with a white letter at the end of a line; negative ends
 * are small tinted rings with no line and no letter. Ends nearer the viewer are larger, brighter and drawn on top, ends
 * behind fade toward the backdrop. Hovering a ball enlarges it, lightens it and adds a white outline and halo (a hovered
 * negative ball also shows a "-"). Clicking a ball snaps to the matching [StandardView] (+X right view, +Y top, +Z front
 * view, and the opposite for the negative balls).
 */
object AxisGizmo {

    /** Pixel (panel unit) distance from the centre to the end of an axis. */
    const val ARM = 24

    private const val BACKDROP = ARM + 8f
    private const val BALL = 6f
    private const val BALL_NEGATIVE = 4.5f
    private const val HOVER_GROW = 1.3f
    private const val HIT_SLACK = 1.5f

    private const val SHADE = 0xFF15181D.toInt()
    private const val WHITE = 0xFFFFFFFF.toInt()

    private class Axis(val label: String, val bright: Int, val deep: Int, val direction: Vector3f, val positive: StandardView, val negative: StandardView)

    private val axes = arrayOf(
        Axis("X", Ui.AXIS_X, Ui.AXIS_X_DEEP, Vector3f(-1f, 0f, 0f), StandardView.RIGHT, StandardView.LEFT),
        Axis("Y", Ui.AXIS_Y, Ui.AXIS_Y_DEEP, Vector3f(0f, 1f, 0f), StandardView.TOP, StandardView.BOTTOM),
        Axis("Z", Ui.AXIS_Z, Ui.AXIS_Z_DEEP, Vector3f(0f, 0f, 1f), StandardView.FRONT, StandardView.BACK)
    )

    /** One axis end, projected: centre [x], [y] (continuous panel coordinates), [near] 0 (far) .. 1 (nearest). */
    private class Dot(val axis: Axis, val positive: Boolean, val x: Float, val y: Float, val depth: Float) {
        val view: StandardView get() = if (positive) axis.positive else axis.negative
        val near: Float get() = (0.5f - 0.5f * depth).coerceIn(0f, 1f)
        val radius: Float get() = (if (positive) BALL else BALL_NEGATIVE) * (0.86f + 0.22f * near)
    }

    /** Ends for the camera orientation of [p], at the gizmo centre [cx], [cy], farthest first. */
    private fun dots(p: Perspective, cx: Int, cy: Int): List<Dot> {
        val inverse = Quaternionf(CameraMath.body(p.yaw, p.pitch, p.roll)).conjugate()
        val out = ArrayList<Dot>(6)
        for (axis in axes) {
            val camera = Vector3f(axis.direction).rotate(inverse)
            // Camera space: +X is left, +Y up, +Z away from the viewer. A degenerate orientation collapses to the centre.
            val sx = if (camera.x.isFinite()) -camera.x else 0f
            val sy = if (camera.y.isFinite()) -camera.y else 0f
            val sz = if (camera.z.isFinite()) camera.z else 0f
            for (positive in booleanArrayOf(true, false)) {
                val sign = if (positive) 1f else -1f
                out += Dot(axis, positive, cx + 0.5f + sx * sign * ARM, cy + 0.5f + sy * sign * ARM, sz * sign)
            }
        }
        return out.sortedByDescending { it.depth }
    }

    fun render(g: GuiGraphics, font: Font, p: Perspective, cx: Int, cy: Int, mx: Int, my: Int) {
        val ox = cx + 0.5f
        val oy = cy + 0.5f
        // Backdrop: dark enough to hold white / coloured marks on bright sky, outlined so it also reads on black.
        Ui.discAa(g, ox, oy, BACKDROP, 0x62101318)
        Ui.discAa(g, ox, oy, BACKDROP, 0x40FFFFFF, hole = BACKDROP - 1f)
        Ui.discAa(g, ox, oy, 1.8f, 0x70FFFFFF)

        val dots = dots(p, cx, cy)
        val hovered = dots.lastOrNull { near(it, mx, my) }
        for (dot in dots) if (dot !== hovered) drawDot(g, font, dot, ox, oy, false)
        // The hovered ball goes on top of everything so its outline is never clipped by a nearer axis.
        if (hovered != null) drawDot(g, font, hovered, ox, oy, true)
    }

    private fun drawDot(g: GuiGraphics, font: Font, dot: Dot, ox: Float, oy: Float, hover: Boolean) {
        val far = 1f - dot.near
        val r = dot.radius + if (hover) HOVER_GROW else 0f
        if (hover) Ui.discAa(g, dot.x, dot.y, r + 3.5f, 0x48FFFFFF)

        if (dot.positive) {
            // Arm first so the ball sits on top of its own line; opaque colour so overlapping arms do not stack alpha.
            val armColor = if (hover) Ui.mix(dot.axis.bright, WHITE, 0.3f) else Ui.mix(dot.axis.bright, SHADE, far * 0.5f)
            Ui.lineAa(g, ox, oy, dot.x, dot.y, 1.5f + 0.9f * dot.near + if (hover) 0.6f else 0f, armColor)

            Ui.discAa(g, dot.x, dot.y, r + 1.3f, 0xA0000000.toInt())
            var body = Ui.mix(dot.axis.deep, SHADE, far * 0.4f)
            var rim = Ui.mix(dot.axis.bright, SHADE, far * 0.4f)
            if (hover) {
                body = Ui.mix(dot.axis.deep, WHITE, 0.22f)
                rim = WHITE
            }
            Ui.discAa(g, dot.x, dot.y, r, body)
            Ui.discAa(g, dot.x, dot.y, r, rim, hole = r - 1.2f)
            letter(g, font, dot.axis.label, dot.x, dot.y)
        } else {
            val strength = 0.55f + 0.45f * dot.near
            Ui.discAa(g, dot.x, dot.y, r + 1f, Ui.fade(0xB0000000.toInt(), strength))
            Ui.discAa(g, dot.x, dot.y, r, Ui.fade(dot.axis.deep, if (hover) 0.95f else 0.45f * strength))
            Ui.discAa(g, dot.x, dot.y, r, if (hover) WHITE else Ui.fade(dot.axis.bright, strength), hole = r - 1.2f)
            if (hover) letter(g, font, "-", dot.x, dot.y)
        }
    }

    /** [text] centred on the ball, white with the vanilla shadow so it reads on every ball colour. */
    private fun letter(g: GuiGraphics, font: Font, text: String, x: Float, y: Float) {
        // Glyph width is advance minus the 1 px gap; capitals are 7 rows tall.
        val left = (x - (font.width(text) - 1) / 2f).roundToInt()
        val top = (y - 3.5f).roundToInt()
        g.drawString(font, text, left, top, WHITE, true)
    }

    /** The view to snap to when [mx], [my] hit a ball, or null. */
    fun hit(p: Perspective, cx: Int, cy: Int, mx: Int, my: Int): StandardView? =
        dots(p, cx, cy).lastOrNull { near(it, mx, my) }?.view

    private fun near(dot: Dot, mx: Int, my: Int): Boolean {
        val dx = mx + 0.5f - dot.x
        val dy = my + 0.5f - dot.y
        val reach = dot.radius + HIT_SLACK
        return dx * dx + dy * dy <= reach * reach
    }
}
