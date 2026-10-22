package com.teamuhi.superbcamera.client.ui.kit

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component

/** One tab of a [TabRail]; a tab that is not [enabled] is dimmed and ignores clicks (its tooltip still shows). */
class RailTab(val label: Component, val tooltip: Component? = null, val enabled: () -> Boolean = { true })

/**
 * A row of category buttons, each one its own enclosed flat button (filled body, 1 px notched border):
 *  - normal: [Ui.RAISED] fill, [Ui.BORDER] outline, light text;
 *  - hover: [Ui.HOVER] fill with an accent outline;
 *  - selected: [Ui.ACCENT_FILL] tint over the raised fill, accent outline plus a thicker accent underline, accent text.
 *
 * The buttons are as wide as their label plus padding and share whatever room is left evenly, so short labels do not
 * waste space. When they do not all fit on one row (narrow panel, long translations) they all shrink in proportion to
 * their labels and the text is cut with an ellipsis; the tooltip carries the full meaning. The tooltip is anchored
 * below the rail ([UiRoot.tooltipBelow]) so it never hides the buttons.
 */
class TabRail(
    private val tabs: List<RailTab>,
    private val selected: () -> Int,
    private val onSelect: (Int) -> Unit
) : Widget() {

    private val lefts = IntArray(tabs.size)
    private val rights = IntArray(tabs.size)

    /** Recomputes the cell of every tab for the current width (cheap; called per frame and per click). */
    private fun layout(ui: UiRoot) {
        if (tabs.isEmpty()) return
        val count = tabs.size
        val gaps = (count - 1) * GAP
        val natural = IntArray(count) { ui.font.width(tabs[it].label.string) + 2 * TEXT_PAD }
        val naturalSum = natural.sum()
        val room = (width - gaps).coerceAtLeast(count)
        // Spare room is shared evenly when everything fits, otherwise every button shrinks by the same factor.
        val factor = if (naturalSum <= room) 1.0 else room.toDouble() / naturalSum
        val extra = if (naturalSum <= room) (room - naturalSum).toDouble() / count else 0.0
        var cursor = x.toDouble()
        for (index in 0 until count) {
            val w = natural[index] * factor + extra
            lefts[index] = Math.round(cursor).toInt()
            rights[index] = Math.round(cursor + w).toInt()
            cursor += w + GAP
        }
        rights[count - 1] = x + width
    }

    private fun indexAt(mx: Double, my: Double): Int {
        if (my < y || my >= y + height) return -1
        for (index in tabs.indices) if (mx >= lefts[index] && mx < rights[index]) return index
        return -1
    }

    override fun render(g: GuiGraphics, ui: UiRoot, mx: Int, my: Int) {
        layout(ui)
        val over = indexAt(mx.toDouble(), my.toDouble())
        val current = selected()
        for ((index, tab) in tabs.withIndex()) {
            val left = lefts[index]
            val w = rights[index] - left
            val active = tab.enabled()
            val isCurrent = index == current
            val hovered = index == over && active
            when {
                isCurrent -> {
                    Ui.chip(g, left, y, w, height, Ui.RAISED, Ui.BORDER)
                    Ui.chip(g, left, y, w, height, Ui.ACCENT_FILL, Ui.forState(Ui.ACCENT, active))
                    // Thicker underline, inset so it stays inside the notched corners.
                    g.fill(left + 1, y + height - 2, left + w - 1, y + height - 1, Ui.forState(Ui.ACCENT, active))
                }
                hovered -> Ui.chip(g, left, y, w, height, Ui.HOVER, Ui.fade(Ui.ACCENT, 0.7f))
                else -> Ui.chip(g, left, y, w, height, Ui.RAISED, Ui.BORDER)
            }
            val color = when {
                isCurrent -> Ui.ACCENT
                hovered -> Ui.ACCENT_HOVER
                else -> Ui.TEXT
            }
            Ui.centered(g, ui.font, Ui.fit(ui.font, tab.label.string, w - 4), left, y, w, height, Ui.forState(color, active))
            if (index == over) ui.tooltipBelow(tab.tooltip, left, y, w, height)
        }
    }

    override fun mouseClicked(ui: UiRoot, mx: Double, my: Double, button: Int): Boolean {
        if (button != 0 || !contains(mx, my)) return false
        layout(ui)
        val index = indexAt(mx, my)
        // A click in the gap between two buttons is swallowed (it must not reach the world behind the panel).
        if (index >= 0 && index != selected() && tabs[index].enabled()) onSelect(index)
        return true
    }

    private companion object {
        /** Space between two buttons. */
        const val GAP = 2

        /** Horizontal padding between a button's edge and its label. */
        const val TEXT_PAD = 6
    }
}
