package com.teamuhi.superbcamera.client.ui.kit

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics

/**
 * Palette and drawing helpers of the editor's widget kit.
 *
 * Minecraft has no light / dark theme and the panel floats over the live world, so contrast comes from a translucent
 * near-black base with light text and one warm accent that stays readable on blue sky as well as in a dark cave.
 * Everything is drawn flat with [GuiGraphics.fill]: no vanilla button textures anywhere.
 */
object Ui {
    const val SURFACE = 0xD0141619.toInt()
    const val RAISED = 0xFF1E2126.toInt()
    const val HOVER = 0xFF2A2E35.toInt()
    const val BORDER = 0x40FFFFFF
    const val TEXT = 0xFFE8E8E8.toInt()
    const val MUTED = 0xFF8A9099.toInt()
    const val ACCENT = 0xFFFFB84D.toInt()
    const val ACCENT_HOVER = 0xFFFFCB7A.toInt()
    const val ACCENT_FILL = 0x30FFB84D
    const val ON_ACCENT = 0xFF14161A.toInt()
    const val DANGER = 0xFFFF6B6B.toInt()
    const val SCRIM = 0x90000000.toInt()

    // Blender's axis colours.
    const val AXIS_X = 0xFFFF5C5C.toInt()
    const val AXIS_Y = 0xFF8BDC5A.toInt()
    const val AXIS_Z = 0xFF5C9DFF.toInt()

    // Deeper variants of the axis colours: white lettering on them keeps >= 4.4:1 contrast (the bright ones are for lines / rims).
    const val AXIS_X_DEEP = 0xFFD23A3A.toInt()
    const val AXIS_Y_DEEP = 0xFF2E8B1F.toInt()
    const val AXIS_Z_DEEP = 0xFF2F6FDD.toInt()

    /** Height of a standard control row. */
    const val ROW = 14

    /** Alpha applied to the text of a disabled control. */
    private const val DISABLED_ALPHA = 0.4f

    /** [color] with its alpha scaled by [factor]. */
    fun fade(color: Int, factor: Float): Int {
        val alpha = ((color ushr 24) * factor).toInt().coerceIn(0, 255)
        return (alpha shl 24) or (color and 0xFFFFFF)
    }

    /** Linear blend of two colours (all four channels): [a] at 0, [b] at 1. */
    fun mix(a: Int, b: Int, t: Float): Int {
        val f = t.coerceIn(0f, 1f)
        var out = 0
        for (shift in intArrayOf(24, 16, 8, 0)) {
            val ca = (a ushr shift) and 0xFF
            val cb = (b ushr shift) and 0xFF
            out = out or ((ca + ((cb - ca) * f).toInt()).coerceIn(0, 255) shl shift)
        }
        return out
    }

    /** [color], dimmed when the control is disabled. */
    fun forState(color: Int, enabled: Boolean): Int = if (enabled) color else fade(color, DISABLED_ALPHA)

    /** [text] cut to [maxWidth] pixels with a trailing ellipsis. */
    fun fit(font: Font, text: String, maxWidth: Int): String {
        if (maxWidth <= 0) return ""
        if (font.width(text) <= maxWidth) return text
        val ellipsis = "…"
        return font.plainSubstrByWidth(text, (maxWidth - font.width(ellipsis)).coerceAtLeast(0)) + ellipsis
    }

    /** Draws [text] without a shadow (flat look), vertically centred in a row of [height] starting at [y]. */
    fun text(g: GuiGraphics, font: Font, text: String, x: Int, y: Int, height: Int, color: Int) {
        g.drawString(font, text, x, y + (height - font.lineHeight + 1) / 2, color, false)
    }

    /** [text] centred horizontally in `[x, x + width)` and vertically in the row. */
    fun centered(g: GuiGraphics, font: Font, text: String, x: Int, y: Int, width: Int, height: Int, color: Int) {
        text(g, font, text, x + (width - font.width(text)) / 2, y, height, color)
    }

    /** A 1 px outline around the rectangle. */
    fun outline(g: GuiGraphics, x: Int, y: Int, width: Int, height: Int, color: Int) {
        g.fill(x, y, x + width, y + 1, color)
        g.fill(x, y + height - 1, x + width, y + height, color)
        g.fill(x, y + 1, x + 1, y + height - 1, color)
        g.fill(x + width - 1, y + 1, x + width, y + height - 1, color)
    }

    /**
     * A flat button body with the four corner pixels cut off (reads as slightly rounded). [fill] is drawn as three
     * non-overlapping strips, so a translucent colour keeps its alpha; [border] (0 for none) is a 1 px notched outline.
     */
    fun chip(g: GuiGraphics, x: Int, y: Int, width: Int, height: Int, fill: Int, border: Int = 0) {
        if (width < 3 || height < 3) {
            g.fill(x, y, x + width, y + height, fill)
            return
        }
        g.fill(x + 1, y, x + width - 1, y + 1, fill)
        g.fill(x, y + 1, x + width, y + height - 1, fill)
        g.fill(x + 1, y + height - 1, x + width - 1, y + height, fill)
        if (border != 0) {
            g.fill(x + 1, y, x + width - 1, y + 1, border)
            g.fill(x + 1, y + height - 1, x + width - 1, y + height, border)
            g.fill(x, y + 1, x + 1, y + height - 1, border)
            g.fill(x + width - 1, y + 1, x + width, y + height - 1, border)
        }
    }

    /** The hover look shared by all controls: a lighter background plus a 1 px accent edge on the left. */
    fun hoverFill(g: GuiGraphics, x: Int, y: Int, width: Int, height: Int) {
        g.fill(x, y, x + width, y + height, HOVER)
        g.fill(x, y, x + 1, y + height, ACCENT)
    }

    /** A filled circle of [radius] built from scanlines (panel units are whole pixels, so this stays crisp). */
    fun disc(g: GuiGraphics, cx: Int, cy: Int, radius: Int, color: Int) {
        for (dy in -radius..radius) {
            val half = Math.sqrt((radius * radius - dy * dy).toDouble()).toInt()
            g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color)
        }
    }

    /**
     * An anti-aliased disc centred on the continuous point [cx], [cy] (pixel `p` spans `[p, p + 1)`), with a fractional
     * [radius]. Edge pixels get a coverage-scaled alpha; runs of equal alpha are merged into one fill, so the interior is
     * a handful of fills per row. A [hole] > 0 leaves only the band between [hole] and [radius] (an anti-aliased ring).
     */
    fun discAa(g: GuiGraphics, cx: Float, cy: Float, radius: Float, color: Int, hole: Float = 0f) {
        val baseAlpha = color ushr 24
        if (radius <= 0f || baseAlpha == 0 || !cx.isFinite() || !cy.isFinite()) return
        val rgb = color and 0xFFFFFF
        val x0 = Math.floor((cx - radius - 1f).toDouble()).toInt()
        val x1 = Math.ceil((cx + radius + 1f).toDouble()).toInt()
        val y0 = Math.floor((cy - radius - 1f).toDouble()).toInt()
        val y1 = Math.ceil((cy + radius + 1f).toDouble()).toInt()
        for (py in y0 until y1) {
            val dy = py + 0.5f - cy
            var runStart = x0
            var runAlpha = 0
            // One step past the end (alpha 0) flushes the last run.
            for (px in x0..x1) {
                var alpha = 0
                if (px < x1) {
                    val dx = px + 0.5f - cx
                    val d = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                    var cover = (radius + 0.5f - d).coerceIn(0f, 1f)
                    if (hole > 0f) cover *= (d - hole + 0.5f).coerceIn(0f, 1f)
                    alpha = (baseAlpha * cover + 0.5f).toInt()
                }
                if (alpha != runAlpha) {
                    if (runAlpha > 0) g.fill(runStart, py, px, py + 1, (runAlpha shl 24) or rgb)
                    runStart = px
                    runAlpha = alpha
                }
            }
        }
    }

    /** An anti-aliased line of [width] with round ends between two continuous points (see [discAa]). */
    fun lineAa(g: GuiGraphics, x0: Float, y0: Float, x1: Float, y1: Float, width: Float, color: Int) {
        val baseAlpha = color ushr 24
        if (width <= 0f || baseAlpha == 0 || !(x0.isFinite() && y0.isFinite() && x1.isFinite() && y1.isFinite())) return
        val rgb = color and 0xFFFFFF
        val half = width / 2f
        val ex = x1 - x0
        val ey = y1 - y0
        val lengthSq = ex * ex + ey * ey
        val minX = Math.floor((minOf(x0, x1) - half - 1f).toDouble()).toInt()
        val maxX = Math.ceil((maxOf(x0, x1) + half + 1f).toDouble()).toInt()
        val minY = Math.floor((minOf(y0, y1) - half - 1f).toDouble()).toInt()
        val maxY = Math.ceil((maxOf(y0, y1) + half + 1f).toDouble()).toInt()
        for (py in minY until maxY) {
            for (px in minX until maxX) {
                val qx = px + 0.5f - x0
                val qy = py + 0.5f - y0
                val t = if (lengthSq <= 1e-6f) 0f else ((qx * ex + qy * ey) / lengthSq).coerceIn(0f, 1f)
                val dx = qx - ex * t
                val dy = qy - ey * t
                val d = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                val alpha = (baseAlpha * (half + 0.5f - d).coerceIn(0f, 1f) + 0.5f).toInt()
                if (alpha > 0) g.fill(px, py, px + 1, py + 1, (alpha shl 24) or rgb)
            }
        }
    }

    /** A circle outline of [radius] with a hollow centre. */
    fun ring(g: GuiGraphics, cx: Int, cy: Int, radius: Int, color: Int) {
        val inner = radius - 1
        for (dy in -radius..radius) {
            val outerHalf = Math.sqrt((radius * radius - dy * dy).toDouble()).toInt()
            val innerHalf = if (kotlin.math.abs(dy) >= inner) -1 else Math.sqrt((inner * inner - dy * dy).toDouble()).toInt()
            if (innerHalf < 0) {
                g.fill(cx - outerHalf, cy + dy, cx + outerHalf + 1, cy + dy + 1, color)
            } else {
                g.fill(cx - outerHalf, cy + dy, cx - innerHalf, cy + dy + 1, color)
                g.fill(cx + innerHalf + 1, cy + dy, cx + outerHalf + 1, cy + dy + 1, color)
            }
        }
    }
}
