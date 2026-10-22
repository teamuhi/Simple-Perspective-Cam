package com.teamuhi.superbcamera.client.ui

import net.minecraft.client.gui.GuiGraphics
import org.joml.Quaternionf
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * The composition grid drawn over the camera view while the editor is open. [key] is the suffix of the lang key
 * `screen.superbcamera.grid.<key>` (display name); the enum is saved by name in the config.
 */
enum class GridStyle(val key: String) {
    OFF("off"),

    /** Rule of thirds. */
    THIRDS("thirds"),

    /** Roughly square cells: 4 rows, as many columns as the window aspect ratio calls for. */
    GRID("grid"),

    /** A cross through the middle with a centre dot. */
    CENTER("center"),

    /** Lines at the golden ratio (38.2% / 61.8%) on both axes. */
    GOLDEN("golden"),

    /** Both corner-to-corner diagonals. */
    DIAGONALS("diagonals"),

    /** Action safe (90%) and title safe (80%) frames. */
    SAFE("safe");

    fun next(): GridStyle = entries[(ordinal + 1) % entries.size]
}

/**
 * Draws a [GridStyle] across the whole window (the camera view is the whole window; the panel is drawn on top of it,
 * and with the panel hidden the grid covers everything).
 *
 * Lines are exactly one real pixel wide whatever the GUI scale (the pose is scaled back to window pixels): a
 * semi-transparent white line with a one pixel darker halo on each side, which reads on a bright sky and in a dark
 * cave alike. Nothing is allocated per frame beyond what [com.mojang.blaze3d.vertex.PoseStack] itself does; the one
 * rotation used for the diagonals is a reused scratch quaternion.
 */
object GridOverlay {
    private const val LINE = 0x59FFFFFF
    private const val SHADOW = 0x40000000

    private const val GRID_ROWS = 4
    private const val MIN_COLUMNS = 2
    private const val MAX_COLUMNS = 16
    private const val GOLDEN_SMALL = 0.381966
    private const val GOLDEN_LARGE = 0.618034

    /** Below this window size (in pixels) a grid is meaningless and skipped. */
    private const val MIN_SIZE = 32

    private val spin = Quaternionf()

    /** Draws [style] over a window of [pixelW] x [pixelH] real pixels whose GUI scale is [guiScale]. */
    fun render(g: GuiGraphics, pixelW: Int, pixelH: Int, guiScale: Double, style: GridStyle) {
        if (style == GridStyle.OFF || pixelW < MIN_SIZE || pixelH < MIN_SIZE) return
        val scale = if (guiScale > 0.0) guiScale else 1.0
        val pose = g.pose()
        pose.pushPose()
        pose.scale((1.0 / scale).toFloat(), (1.0 / scale).toFloat(), 1f)
        try {
            when (style) {
                GridStyle.THIRDS -> cells(g, pixelW, pixelH, 3, 3)
                GridStyle.GRID -> {
                    val columns = (GRID_ROWS * pixelW.toFloat() / pixelH).roundToInt().coerceIn(MIN_COLUMNS, MAX_COLUMNS)
                    cells(g, pixelW, pixelH, columns, GRID_ROWS)
                }
                GridStyle.CENTER -> center(g, pixelW, pixelH)
                GridStyle.GOLDEN -> golden(g, pixelW, pixelH)
                GridStyle.DIAGONALS -> {
                    diagonal(g, 0f, 0f, pixelW.toFloat(), pixelH.toFloat())
                    diagonal(g, pixelW.toFloat(), 0f, 0f, pixelH.toFloat())
                }
                GridStyle.SAFE -> {
                    frame(g, pixelW, pixelH, 0.9f)
                    frame(g, pixelW, pixelH, 0.8f)
                }
                GridStyle.OFF -> {}
            }
        } finally {
            pose.popPose()
        }
    }

    // ------------------------------------------------------------------------------------------------ shapes

    /** Evenly spaced inner lines: [columns] x [rows] cells. */
    private fun cells(g: GuiGraphics, w: Int, h: Int, columns: Int, rows: Int) {
        for (i in 1 until columns) vertical(g, w * i / columns, 0, h)
        for (i in 1 until rows) horizontal(g, 0, w, h * i / rows)
    }

    private fun golden(g: GuiGraphics, w: Int, h: Int) {
        vertical(g, (w * GOLDEN_SMALL).roundToInt(), 0, h)
        vertical(g, (w * GOLDEN_LARGE).roundToInt(), 0, h)
        horizontal(g, 0, w, (h * GOLDEN_SMALL).roundToInt())
        horizontal(g, 0, w, (h * GOLDEN_LARGE).roundToInt())
    }

    private fun center(g: GuiGraphics, w: Int, h: Int) {
        val cx = w / 2
        val cy = h / 2
        vertical(g, cx, 0, h)
        horizontal(g, 0, w, cy)
        g.fill(cx - 3, cy - 3, cx + 4, cy + 4, SHADOW)
        g.fill(cx - 2, cy - 2, cx + 3, cy + 3, LINE)
    }

    /** A rectangle covering [fraction] of the window, centred. */
    private fun frame(g: GuiGraphics, w: Int, h: Int, fraction: Float) {
        val left = (w * (1f - fraction) / 2f).roundToInt()
        val top = (h * (1f - fraction) / 2f).roundToInt()
        val right = w - left
        val bottom = h - top
        horizontal(g, left, right, top)
        horizontal(g, left, right, bottom)
        vertical(g, left, top, bottom)
        vertical(g, right, top, bottom)
    }

    /** A one pixel line from ([x0], [y0]) to ([x1], [y1]) drawn as a rotated bar. */
    private fun diagonal(g: GuiGraphics, x0: Float, y0: Float, x1: Float, y1: Float) {
        val dx = x1 - x0
        val dy = y1 - y0
        val length = sqrt(dx * dx + dy * dy).roundToInt()
        if (length <= 0) return
        val pose = g.pose()
        pose.pushPose()
        pose.translate(x0, y0, 0f)
        pose.mulPose(spin.rotationZ(atan2(dy, dx)))
        g.fill(0, -1, length, 2, SHADOW)
        g.fill(0, 0, length, 1, LINE)
        pose.popPose()
    }

    // ------------------------------------------------------------------------------------------------ primitives

    /** A one pixel vertical line at [x] from [y0] to [y1] with a one pixel halo on each side. */
    private fun vertical(g: GuiGraphics, x: Int, y0: Int, y1: Int) {
        g.fill(x - 1, y0, x + 2, y1, SHADOW)
        g.fill(x, y0, x + 1, y1, LINE)
    }

    private fun horizontal(g: GuiGraphics, x0: Int, x1: Int, y: Int) {
        g.fill(x0, y - 1, x1, y + 2, SHADOW)
        g.fill(x0, y, x1, y + 1, LINE)
    }
}
